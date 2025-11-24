# Quick Start: ECS Deployment Setup

This is the simplified guide to get ECS deployment running. For detailed instructions, see `ECS-SETUP.md`.

## Prerequisites Checklist

- [ ] AWS CLI installed and configured (`aws configure`)
- [ ] GitHub CLI installed (`brew install gh`)
- [ ] AWS account with admin access
- [ ] Docker installed locally

## 1. Update Task Definitions (Required)

```bash
cd rfp-java-api

# Run the script to update with your AWS account ID
./update-task-definitions.sh
```

## 2. Create ECS Infrastructure

```bash
# Set your environment prefix
export ENV_PREFIX="dev"  # or your bucket prefix from .env

# Create ECS clusters
aws ecs create-cluster --cluster-name ${ENV_PREFIX}-ecs-cluster --region us-east-1

# Create CloudWatch log groups
aws logs create-log-group --log-group-name /ecs/${ENV_PREFIX}-java-api --region us-east-1

# Get your AWS account ID
export ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Create IAM roles (copy from ECS-SETUP.md or run these commands):
# - ecsTaskExecutionRole (for pulling images, writing logs)
# - ecsTaskRole (for accessing S3, DynamoDB, etc.)
```

### Quick IAM Role Setup

```bash
# Create trust policy
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

# Create execution role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file:///tmp/ecs-trust-policy.json

aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

# Create task role
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document file:///tmp/ecs-trust-policy.json

aws iam attach-role-policy \
  --role-name ecsTaskRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess

aws iam attach-role-policy \
  --role-name ecsTaskRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess
```

## 3. Register Task Definitions

```bash
cd rfp-java-api

# Register dev task definition
aws ecs register-task-definition --cli-input-json file://task-definition-dev.json

# Register prod task definition
aws ecs register-task-definition --cli-input-json file://task-definition-prod.json
```

## 4. Create ECS Services

```bash
# Get default VPC and subnets
export VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query 'Vpcs[0].VpcId' --output text)
export SUBNET_IDS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query 'Subnets[*].SubnetId' --output text | tr '\t' ',')

# Create security group
export SG_ID=$(aws ec2 create-security-group \
  --group-name java-api-ecs-sg \
  --description "Security group for Java API ECS" \
  --vpc-id $VPC_ID \
  --query 'GroupId' --output text)

aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

# Create ECS service for dev
aws ecs create-service \
  --cluster ${ENV_PREFIX}-ecs-cluster \
  --service-name ${ENV_PREFIX}-java-api-service \
  --task-definition ${ENV_PREFIX}-java-api-task \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_IDS],securityGroups=[$SG_ID],assignPublicIp=ENABLED}"

echo "✅ ECS infrastructure created!"
```

## 5. Configure GitHub Secrets

```bash
# Login to GitHub
gh auth login

# Set secrets for rfp-java-api
gh secret set AWS_ACCESS_KEY_ID --repo billqhan/rfp-java-api
gh secret set AWS_SECRET_ACCESS_KEY --repo billqhan/rfp-java-api
gh secret set AWS_REGION --body "us-east-1" --repo billqhan/rfp-java-api
gh secret set ECR_REGISTRY --body "${ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com" --repo billqhan/rfp-java-api

# ECS-specific secrets
gh secret set ECS_CLUSTER_DEV --body "${ENV_PREFIX}-ecs-cluster" --repo billqhan/rfp-java-api
gh secret set ECS_CLUSTER_PROD --body "prod-ecs-cluster" --repo billqhan/rfp-java-api
gh secret set ECS_SERVICE_DEV --body "${ENV_PREFIX}-java-api-service" --repo billqhan/rfp-java-api
gh secret set ECS_SERVICE_PROD --body "prod-java-api-service" --repo billqhan/rfp-java-api
gh secret set ECS_TASK_FAMILY_DEV --body "${ENV_PREFIX}-java-api-task" --repo billqhan/rfp-java-api
gh secret set ECS_TASK_FAMILY_PROD --body "prod-java-api-task" --repo billqhan/rfp-java-api

# Verify secrets
gh secret list --repo billqhan/rfp-java-api
```

## 6. Create GitHub Environments

Via GitHub web UI:
1. Go to repository Settings → Environments
2. Create `development` environment
3. Create `production` environment (with protection rules)

## 7. Commit and Deploy

```bash
cd rfp-java-api

# Add all ECS files
git add .github/workflows/ci-cd-ecs.yml
git add .github/workflows/ci-cd.yml  # (disabled EKS)
git add task-definition-*.json
git add update-task-definitions.sh
git add ECS-SETUP.md
git add QUICKSTART-ECS.md

# Commit changes
git commit -m "feat: configure ECS as primary deployment (EKS disabled)"

# Push to develop to test
git checkout develop
git push origin develop

# Monitor in GitHub Actions
# https://github.com/billqhan/rfp-java-api/actions
```

## 8. Verify Deployment

```bash
# Check ECS service
aws ecs describe-services \
  --cluster ${ENV_PREFIX}-ecs-cluster \
  --services ${ENV_PREFIX}-java-api-service

# Get task ARN
TASK_ARN=$(aws ecs list-tasks \
  --cluster ${ENV_PREFIX}-ecs-cluster \
  --service-name ${ENV_PREFIX}-java-api-service \
  --query 'taskArns[0]' --output text)

# Get task public IP
TASK_ENI=$(aws ecs describe-tasks \
  --cluster ${ENV_PREFIX}-ecs-cluster \
  --tasks $TASK_ARN \
  --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' \
  --output text)

PUBLIC_IP=$(aws ec2 describe-network-interfaces \
  --network-interface-ids $TASK_ENI \
  --query 'NetworkInterfaces[0].Association.PublicIp' \
  --output text)

# Test the API
curl http://$PUBLIC_IP:8080/actuator/health

# View logs
aws logs tail /ecs/${ENV_PREFIX}-java-api --follow
```

## Production Deployment

Once dev is working:

```bash
# Create prod infrastructure (repeat steps 2-4 with prod- prefix)
export ENV_PREFIX="prod"

# Merge to main branch
git checkout main
git merge develop
git push origin main

# Monitor production deployment in GitHub Actions
```

## Troubleshooting

### Task won't start
```bash
# Check CloudWatch logs
aws logs tail /ecs/${ENV_PREFIX}-java-api --follow

# Check task stopped reason
aws ecs describe-tasks --cluster ${ENV_PREFIX}-ecs-cluster --tasks $TASK_ARN
```

### GitHub Actions fails
- Verify all secrets are set: `gh secret list --repo billqhan/rfp-java-api`
- Check task definitions exist: `aws ecs list-task-definitions`
- Verify IAM roles exist: `aws iam get-role --role-name ecsTaskExecutionRole`

### Can't connect to API
- Check security group allows inbound on 8080
- Verify task has public IP assigned
- Check task is in RUNNING state

## Next Steps

- [ ] Set up Application Load Balancer for production (see ECS-SETUP.md)
- [ ] Configure auto-scaling
- [ ] Set up CloudWatch alarms
- [ ] Configure custom domain with Route53
- [ ] Enable Container Insights for monitoring

## Reference Files

- Full setup guide: `ECS-SETUP.md`
- Secrets documentation: `../rfp-infrastructure/GITHUB-SECRETS-SETUP.md`
- Task definitions: `task-definition-dev.json`, `task-definition-prod.json`
- Update script: `update-task-definitions.sh`
