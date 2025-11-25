#!/bin/bash

# Complete ECS Deployment Script
# This script walks you through the entire ECS setup process

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    if ! command -v aws &> /dev/null; then
        log_error "AWS CLI not found. Install it first: https://aws.amazon.com/cli/"
        exit 1
    fi
    
    if ! command -v gh &> /dev/null; then
        log_warning "GitHub CLI not found. You'll need to set secrets manually."
    fi
    
    if ! command -v git &> /dev/null; then
        log_error "Git not found."
        exit 1
    fi
    
    log_success "Prerequisites check passed!"
}

# Step 1: Update task definitions
update_task_definitions() {
    log_info "Step 1: Updating task definition templates..."
    
    if [ -f "./update-task-definitions.sh" ]; then
        chmod +x ./update-task-definitions.sh
        ./update-task-definitions.sh
    else
        log_warning "update-task-definitions.sh not found. Updating manually..."
        ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
        sed -i.bak "s/YOUR_ACCOUNT_ID/$ACCOUNT_ID/g" task-definition-*.json
        rm -f task-definition-*.json.bak
        log_success "Task definitions updated with account ID: $ACCOUNT_ID"
    fi
}

# Step 2: Create ECS infrastructure
create_ecs_infrastructure() {
    log_info "Step 2: Creating ECS infrastructure..."
    
    # Use environment variable or default to 'dev'
    ENV_PREFIX=${ENV_PREFIX:-dev}
    export ENV_PREFIX
    
    ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    REGION="us-east-1"
    
    log_info "Creating ECS cluster: ${ENV_PREFIX}-ecs-cluster..."
    aws ecs create-cluster --cluster-name ${ENV_PREFIX}-ecs-cluster --region $REGION 2>/dev/null || \
        log_warning "Cluster may already exist"
    
    log_info "Creating CloudWatch log group..."
    aws logs create-log-group --log-group-name /ecs/${ENV_PREFIX}-java-api --region $REGION 2>/dev/null || \
        log_warning "Log group may already exist"
    
    log_info "Creating IAM roles..."
    
    # Trust policy
    cat > /tmp/ecs-trust-policy.json <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {"Service": "ecs-tasks.amazonaws.com"},
    "Action": "sts:AssumeRole"
  }]
}
EOF
    
    # Execution role
    aws iam create-role \
        --role-name ecsTaskExecutionRole \
        --assume-role-policy-document file:///tmp/ecs-trust-policy.json 2>/dev/null || \
        log_warning "ecsTaskExecutionRole may already exist"
    
    aws iam attach-role-policy \
        --role-name ecsTaskExecutionRole \
        --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy 2>/dev/null
    
    aws iam attach-role-policy \
        --role-name ecsTaskExecutionRole \
        --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly 2>/dev/null
    
    # Task role
    aws iam create-role \
        --role-name ecsTaskRole \
        --assume-role-policy-document file:///tmp/ecs-trust-policy.json 2>/dev/null || \
        log_warning "ecsTaskRole may already exist"
    
    aws iam attach-role-policy \
        --role-name ecsTaskRole \
        --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess 2>/dev/null
    
    aws iam attach-role-policy \
        --role-name ecsTaskRole \
        --policy-arn arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess 2>/dev/null
    
    log_info "Waiting 10 seconds for IAM roles to propagate..."
    sleep 10
    
    log_info "Registering task definitions..."
    aws ecs register-task-definition --cli-input-json file://task-definition-dev.json || \
        log_error "Failed to register dev task definition"
    
    log_info "Creating VPC resources..."
    VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query 'Vpcs[0].VpcId' --output text)
    SUBNET_IDS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query 'Subnets[*].SubnetId' --output text | tr '\t' ',')
    
    log_info "Checking for existing ALB security group..."
    # Check if ALB-created security group exists (preferred)
    SG_ID=$(aws ec2 describe-security-groups \
        --filters "Name=group-name,Values=${ENV_PREFIX}-java-api-task-sg" \
        --query 'SecurityGroups[0].GroupId' --output text 2>/dev/null)
    
    if [ -z "$SG_ID" ] || [ "$SG_ID" == "None" ]; then
        log_info "ALB security group not found, creating new security group..."
        SG_ID=$(aws ec2 create-security-group \
            --group-name ${ENV_PREFIX}-java-api-ecs-sg \
            --description "Security group for Java API ECS" \
            --vpc-id $VPC_ID \
            --query 'GroupId' --output text 2>/dev/null) || \
            SG_ID=$(aws ec2 describe-security-groups \
                --filters "Name=group-name,Values=${ENV_PREFIX}-java-api-ecs-sg" \
                --query 'SecurityGroups[0].GroupId' --output text)
        
        aws ec2 authorize-security-group-ingress \
            --group-id $SG_ID \
            --protocol tcp \
            --port 8080 \
            --cidr 0.0.0.0/0 2>/dev/null || \
            log_warning "Security group rule may already exist"
    else
        log_info "Using ALB security group: $SG_ID"
    fi
    
    # Get ALB target group ARN (if ALB exists)
    log_info "Checking for ALB target group..."
    TG_ARN=$(aws elbv2 describe-target-groups \
        --names ${ENV_PREFIX}-java-api-tg \
        --region $REGION \
        --query 'TargetGroups[0].TargetGroupArn' \
        --output text 2>/dev/null || echo "")
    
    log_info "Creating ECS service..."
    if [ -n "$TG_ARN" ] && [ "$TG_ARN" != "None" ]; then
        log_info "ALB target group found, creating service with load balancer integration..."
        aws ecs create-service \
            --cluster ${ENV_PREFIX}-ecs-cluster \
            --service-name ${ENV_PREFIX}-java-api-service \
            --task-definition ${ENV_PREFIX}-java-api-task \
            --desired-count 1 \
            --launch-type FARGATE \
            --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_IDS],securityGroups=[$SG_ID],assignPublicIp=ENABLED}" \
            --load-balancers "targetGroupArn=$TG_ARN,containerName=java-api,containerPort=8080" \
            --health-check-grace-period-seconds 120 \
            --region $REGION 2>/dev/null || \
            log_warning "Service may already exist"
    else
        log_info "No ALB target group found, creating service without load balancer..."
        aws ecs create-service \
            --cluster ${ENV_PREFIX}-ecs-cluster \
            --service-name ${ENV_PREFIX}-java-api-service \
            --task-definition ${ENV_PREFIX}-java-api-task \
            --desired-count 1 \
            --launch-type FARGATE \
            --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_IDS],securityGroups=[$SG_ID],assignPublicIp=ENABLED}" \
            --region $REGION 2>/dev/null || \
            log_warning "Service may already exist"
    fi
    
    log_success "ECS infrastructure created!"
    
    # Save for GitHub secrets
    export ECS_CLUSTER_DEV="${ENV_PREFIX}-ecs-cluster"
    export ECS_SERVICE_DEV="${ENV_PREFIX}-java-api-service"
    export ECS_TASK_FAMILY_DEV="${ENV_PREFIX}-java-api-task"
}

# Step 3: Configure GitHub secrets
configure_github_secrets() {
    log_info "Step 3: Configuring GitHub secrets..."
    
    if ! command -v gh &> /dev/null; then
        log_warning "GitHub CLI not available. Set secrets manually via GitHub UI."
        log_info "See GITHUB-SECRETS-SETUP.md for instructions."
        return
    fi
    
    # Use environment variable or skip if not set
    if [ -z "$GITHUB_REPO" ]; then
        log_warning "GITHUB_REPO not set. Skipping GitHub secrets configuration."
        return
    fi
    
    ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    REGION="us-east-1"
    
    log_info "Setting secrets for $GITHUB_REPO..."
    
    if [ -z "$AWS_ACCESS_KEY_ID" ] || [ -z "$AWS_SECRET_ACCESS_KEY" ]; then
        log_warning "AWS credentials not set. Skipping GitHub secrets configuration."
        return
    fi
    echo
    
    echo "$AWS_ACCESS_KEY_ID" | gh secret set AWS_ACCESS_KEY_ID --repo "$GITHUB_REPO"
    echo "$AWS_SECRET_ACCESS_KEY" | gh secret set AWS_SECRET_ACCESS_KEY --repo "$GITHUB_REPO"
    echo "$REGION" | gh secret set AWS_REGION --repo "$GITHUB_REPO"
    echo "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com" | gh secret set ECR_REGISTRY --repo "$GITHUB_REPO"
    echo "$ECS_CLUSTER_DEV" | gh secret set ECS_CLUSTER_DEV --repo "$GITHUB_REPO"
    echo "$ECS_SERVICE_DEV" | gh secret set ECS_SERVICE_DEV --repo "$GITHUB_REPO"
    echo "$ECS_TASK_FAMILY_DEV" | gh secret set ECS_TASK_FAMILY_DEV --repo "$GITHUB_REPO"
    
    log_success "GitHub secrets configured!"
    log_info "Don't forget to set PROD secrets too!"
}

# Step 4: Commit and deploy
commit_and_deploy() {
    log_info "Step 4: Committing changes..."
    
    # Skip if AUTO_COMMIT is not set to 'true'
    if [ "$AUTO_COMMIT" != "true" ]; then
        log_info "AUTO_COMMIT not enabled. Skipping git operations."
        log_info "Changes are ready. Review and commit manually if needed."
        return
    fi
    
    git add .github/workflows/ci-cd.yml
    git add .github/workflows/ci-cd-ecs.yml
    git add task-definition-*.json
    git add *.sh
    git add *.md
    
    git status
    
    git commit -m "feat: configure ECS as primary deployment (EKS disabled)"
    git checkout develop 2>/dev/null || git checkout -b develop
    git push origin develop
    log_success "Changes pushed! Check GitHub Actions for deployment status."
}

# Main execution
main() {
    echo ""
    log_info "🚀 ECS Deployment Setup Script"
    echo ""
    
    check_prerequisites
    echo ""
    
    # Run all steps automatically unless INTERACTIVE mode is enabled
    if [ "$INTERACTIVE" = "true" ]; then
        log_info "You can run individual functions:"
        log_info "  update_task_definitions"
        log_info "  create_ecs_infrastructure"
        log_info "  configure_github_secrets"
        log_info "  commit_and_deploy"
        exit 0
    fi
    
    update_task_definitions
    echo ""
    
    # Create ECS infrastructure if CREATE_INFRA is set
    if [ "$CREATE_INFRA" = "true" ]; then
        create_ecs_infrastructure
        echo ""
    fi
    
    # Configure GitHub secrets if CONFIG_SECRETS is set
    if [ "$CONFIG_SECRETS" = "true" ]; then
        configure_github_secrets
        echo ""
    fi
    
    # Commit and deploy if AUTO_COMMIT is set
    if [ "$AUTO_COMMIT" = "true" ]; then
        commit_and_deploy
        echo ""
    fi
    
    log_success "Setup complete!"
    log_info "Next: Monitor deployment at https://github.com/$GITHUB_REPO/actions"
}

main
