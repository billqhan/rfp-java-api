# ECS Deployment Setup Guide

This guide helps you set up AWS ECS (Fargate) infrastructure to deploy the Java API using the `ci-cd-ecs.yml` workflow.

## Prerequisites

- AWS CLI installed and configured
- Docker installed locally (for testing)
- GitHub repository with secrets configured
- ECR repository created

## Infrastructure Setup

### 1. Create ECS Cluster

```bash
# Development
aws ecs create-cluster \
  --cluster-name dev-ecs-cluster \
  --region us-east-1

# Production
aws ecs create-cluster \
  --cluster-name prod-ecs-cluster \
  --region us-east-1
```

### 2. Create IAM Roles

#### Task Execution Role (for ECS to pull images and write logs)

```bash
# Create trust policy
cat > /tmp/ecs-task-execution-trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ecs-tasks.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create role
aws iam create-role \
  --role-name ecsTaskExecutionRole \
  --assume-role-policy-document file:///tmp/ecs-task-execution-trust-policy.json

# Attach managed policies
aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy

aws iam attach-role-policy \
  --role-name ecsTaskExecutionRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly
```

#### Task Role (for application to access AWS services)

```bash
# Create role
aws iam create-role \
  --role-name ecsTaskRole \
  --assume-role-policy-document file:///tmp/ecs-task-execution-trust-policy.json

# Attach policies for your application needs
aws iam attach-role-policy \
  --role-name ecsTaskRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess

aws iam attach-role-policy \
  --role-name ecsTaskRole \
  --policy-arn arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess
```

### 3. Create CloudWatch Log Groups

```bash
# Development
aws logs create-log-group \
  --log-group-name /ecs/dev-java-api \
  --region us-east-1

# Production
aws logs create-log-group \
  --log-group-name /ecs/prod-java-api \
  --region us-east-1
```

### 4. Create VPC Resources (if not exists)

```bash
# Get default VPC
VPC_ID=$(aws ec2 describe-vpcs \
  --filters "Name=isDefault,Values=true" \
  --query 'Vpcs[0].VpcId' \
  --output text)

# Get subnets
SUBNET_IDS=$(aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=$VPC_ID" \
  --query 'Subnets[*].SubnetId' \
  --output text | tr '\t' ',')

echo "VPC ID: $VPC_ID"
echo "Subnet IDs: $SUBNET_IDS"
```

### 5. Create Security Group

```bash
# Create security group
SG_ID=$(aws ec2 create-security-group \
  --group-name java-api-ecs-sg \
  --description "Security group for Java API ECS tasks" \
  --vpc-id $VPC_ID \
  --query 'GroupId' \
  --output text)

# Allow inbound traffic on port 8080
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0

echo "Security Group ID: $SG_ID"
```

### 6. Create Application Load Balancer (Optional but Recommended)

```bash
# Create ALB
ALB_ARN=$(aws elbv2 create-load-balancer \
  --name java-api-alb \
  --subnets $(echo $SUBNET_IDS | tr ',' ' ') \
  --security-groups $SG_ID \
  --scheme internet-facing \
  --type application \
  --query 'LoadBalancers[0].LoadBalancerArn' \
  --output text)

# Create target group
TG_ARN=$(aws elbv2 create-target-group \
  --name java-api-tg \
  --protocol HTTP \
  --port 8080 \
  --vpc-id $VPC_ID \
  --target-type ip \
  --health-check-path /actuator/health \
  --health-check-interval-seconds 30 \
  --health-check-timeout-seconds 5 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3 \
  --query 'TargetGroups[0].TargetGroupArn' \
  --output text)

# Create listener
aws elbv2 create-listener \
  --load-balancer-arn $ALB_ARN \
  --protocol HTTP \
  --port 80 \
  --default-actions Type=forward,TargetGroupArn=$TG_ARN

echo "ALB ARN: $ALB_ARN"
echo "Target Group ARN: $TG_ARN"
```

### 7. Update Task Definition Templates

Update `task-definition-dev.json` and `task-definition-prod.json`:

```bash
# Get your AWS account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Update dev task definition
sed -i '' "s/YOUR_ACCOUNT_ID/$ACCOUNT_ID/g" task-definition-dev.json

# Update prod task definition
sed -i '' "s/YOUR_ACCOUNT_ID/$ACCOUNT_ID/g" task-definition-prod.json
```

### 8. Register Initial Task Definitions

```bash
# Development
aws ecs register-task-definition \
  --cli-input-json file://task-definition-dev.json

# Production
aws ecs register-task-definition \
  --cli-input-json file://task-definition-prod.json
```

### 9. Create ECS Services

```bash
# Development
aws ecs create-service \
  --cluster dev-ecs-cluster \
  --service-name dev-java-api-service \
  --task-definition dev-java-api-task \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_IDS],securityGroups=[$SG_ID],assignPublicIp=ENABLED}" \
  --load-balancers "targetGroupArn=$TG_ARN,containerName=java-api,containerPort=8080"

# Production
aws ecs create-service \
  --cluster prod-ecs-cluster \
  --service-name prod-java-api-service \
  --task-definition prod-java-api-task \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_IDS],securityGroups=[$SG_ID],assignPublicIp=ENABLED}" \
  --load-balancers "targetGroupArn=$TG_ARN,containerName=java-api,containerPort=8080"
```

## GitHub Secrets Configuration

Configure these secrets in your GitHub repository:

```bash
gh secret set AWS_ACCESS_KEY_ID --repo billqhan/rfp-java-api
gh secret set AWS_SECRET_ACCESS_KEY --repo billqhan/rfp-java-api
gh secret set AWS_REGION --repo billqhan/rfp-java-api
gh secret set ECR_REGISTRY --repo billqhan/rfp-java-api

# ECS-specific secrets
gh secret set ECS_CLUSTER_DEV --repo billqhan/rfp-java-api
gh secret set ECS_CLUSTER_PROD --repo billqhan/rfp-java-api
gh secret set ECS_SERVICE_DEV --repo billqhan/rfp-java-api
gh secret set ECS_SERVICE_PROD --repo billqhan/rfp-java-api
gh secret set ECS_TASK_FAMILY_DEV --repo billqhan/rfp-java-api
gh secret set ECS_TASK_FAMILY_PROD --repo billqhan/rfp-java-api
gh secret set API_ENDPOINT_PROD --repo billqhan/rfp-java-api  # Optional
```

Example values:
```
ECS_CLUSTER_DEV=dev-ecs-cluster
ECS_CLUSTER_PROD=prod-ecs-cluster
ECS_SERVICE_DEV=dev-java-api-service
ECS_SERVICE_PROD=prod-java-api-service
ECS_TASK_FAMILY_DEV=dev-java-api-task
ECS_TASK_FAMILY_PROD=prod-java-api-task
```

## GitHub Environments

Create these environments in GitHub repository settings:

1. **development-ecs**
   - No protection rules (or optional approval)
   - URL: Your dev ALB endpoint

2. **production-ecs**
   - Required reviewers: At least 1
   - Deployment branches: Only `main`
   - URL: Your prod ALB endpoint

## Testing the Setup

### Local Docker Test

```bash
# Build locally
mvn clean package -DskipTests
docker build -t java-api-test .
docker run -p 8080:8080 java-api-test

# Test
curl http://localhost:8080/actuator/health
```

### Manual ECS Deployment Test

```bash
# Build and push to ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin $ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com

docker build -t $ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/rfp-java-api:test .
docker push $ACCOUNT_ID.dkr.ecr.us-east-1.amazonaws.com/rfp-java-api:test

# Update service
aws ecs update-service \
  --cluster dev-ecs-cluster \
  --service dev-java-api-service \
  --force-new-deployment
```

## Monitoring

### View Service Status

```bash
aws ecs describe-services \
  --cluster dev-ecs-cluster \
  --services dev-java-api-service
```

### View Task Logs

```bash
# Get task ARN
TASK_ARN=$(aws ecs list-tasks \
  --cluster dev-ecs-cluster \
  --service-name dev-java-api-service \
  --query 'taskArns[0]' \
  --output text)

# View logs
aws logs tail /ecs/dev-java-api --follow
```

### Check ALB Health

```bash
# Get ALB DNS name
ALB_DNS=$(aws elbv2 describe-load-balancers \
  --load-balancer-arns $ALB_ARN \
  --query 'LoadBalancers[0].DNSName' \
  --output text)

curl http://$ALB_DNS/actuator/health
```

## Troubleshooting

### Task fails to start

1. Check CloudWatch logs: `/ecs/dev-java-api`
2. Verify IAM roles have correct permissions
3. Ensure security group allows inbound traffic
4. Check if image exists in ECR

### Service deployment stuck

```bash
# Force new deployment
aws ecs update-service \
  --cluster dev-ecs-cluster \
  --service dev-java-api-service \
  --force-new-deployment

# Or scale to 0 and back up
aws ecs update-service \
  --cluster dev-ecs-cluster \
  --service dev-java-api-service \
  --desired-count 0

aws ecs update-service \
  --cluster dev-ecs-cluster \
  --service dev-java-api-service \
  --desired-count 1
```

### GitHub Actions failure

1. Verify all secrets are set correctly
2. Check AWS credentials have necessary permissions
3. Ensure task definition templates exist in repo
4. Review workflow logs for specific error messages

## Cost Optimization

- Use Fargate Spot for dev environment (cheaper)
- Scale down dev to 0 tasks during off-hours
- Use smaller task sizes for dev (1 vCPU, 2GB)
- Enable Container Insights only for prod

## Next Steps

1. ✅ Complete infrastructure setup above
2. ✅ Configure GitHub secrets
3. ✅ Create GitHub environments
4. ✅ Push code to `develop` branch to trigger deployment
5. ✅ Verify deployment in AWS console
6. ✅ Test API endpoint
7. ✅ Merge to `main` for production deployment

## Reference

- [ECS Task Definitions](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task_definitions.html)
- [Fargate Pricing](https://aws.amazon.com/fargate/pricing/)
- [GitHub Actions ECS Deploy](https://github.com/aws-actions/amazon-ecs-deploy-task-definition)
