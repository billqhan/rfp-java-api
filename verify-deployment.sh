#!/bin/bash

# Verify Java API ECS Deployment
# This script checks if the Java API is properly deployed and healthy

set -e

ENVIRONMENT=${1:-dev}
CLUSTER_NAME="${ENVIRONMENT}-ecs-cluster"
SERVICE_NAME="${ENVIRONMENT}-java-api-service"
REGION="us-east-1"

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

echo ""
log_info "Verifying Java API deployment for $ENVIRONMENT"
echo ""

# Check service status
log_info "1. Checking ECS service status..."
SERVICE_STATUS=$(aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$REGION" \
    --query 'services[0].{Status:status,Running:runningCount,Desired:desiredCount,TaskDef:taskDefinition}' \
    --output json)

STATUS=$(echo "$SERVICE_STATUS" | jq -r '.Status')
RUNNING=$(echo "$SERVICE_STATUS" | jq -r '.Running')
DESIRED=$(echo "$SERVICE_STATUS" | jq -r '.Desired')
TASK_DEF=$(echo "$SERVICE_STATUS" | jq -r '.TaskDef')

if [ "$STATUS" = "ACTIVE" ]; then
    log_success "Service is ACTIVE"
else
    log_error "Service status: $STATUS"
fi

log_info "Running tasks: $RUNNING / $DESIRED"
log_info "Task definition: $TASK_DEF"

if [ "$RUNNING" -lt "$DESIRED" ]; then
    log_warning "Service is scaling or experiencing issues"
fi

# Check recent service events
log_info "2. Checking recent service events..."
aws ecs describe-services \
    --cluster "$CLUSTER_NAME" \
    --services "$SERVICE_NAME" \
    --region "$REGION" \
    --query 'services[0].events[0:3].{Time:createdAt,Message:message}' \
    --output table

# Get task details
log_info "3. Getting task details..."
TASK_ARN=$(aws ecs list-tasks \
    --cluster "$CLUSTER_NAME" \
    --service-name "$SERVICE_NAME" \
    --desired-status RUNNING \
    --region "$REGION" \
    --query 'taskArns[0]' \
    --output text)

if [ -z "$TASK_ARN" ] || [ "$TASK_ARN" = "None" ]; then
    log_error "No running tasks found"
    exit 1
fi

TASK_DETAILS=$(aws ecs describe-tasks \
    --cluster "$CLUSTER_NAME" \
    --tasks "$TASK_ARN" \
    --region "$REGION" \
    --query 'tasks[0].{Status:lastStatus,Health:healthStatus,Image:containers[0].image,Started:startedAt}' \
    --output json)

TASK_STATUS=$(echo "$TASK_DETAILS" | jq -r '.Status')
HEALTH_STATUS=$(echo "$TASK_DETAILS" | jq -r '.Health')
IMAGE=$(echo "$TASK_DETAILS" | jq -r '.Image')
STARTED=$(echo "$TASK_DETAILS" | jq -r '.Started')

log_info "Task status: $TASK_STATUS"
log_info "Health status: $HEALTH_STATUS"
log_info "Image: $IMAGE"
log_info "Started at: $STARTED"

# Check image architecture
log_info "4. Verifying image architecture..."
REPO_NAME=$(echo "$IMAGE" | cut -d'/' -f2 | cut -d':' -f1)
IMAGE_TAG=$(echo "$IMAGE" | cut -d':' -f2)

IMAGE_MANIFEST=$(aws ecr describe-images \
    --repository-name "$REPO_NAME" \
    --image-ids imageTag="$IMAGE_TAG" \
    --region "$REGION" \
    --query 'imageDetails[0].imageTags' \
    --output json 2>/dev/null || echo "[]")

log_info "Image tags: $(echo $IMAGE_MANIFEST | jq -r '. | join(", ")')"

# Get public IP
log_info "5. Getting task public IP..."
ENI_ID=$(aws ecs describe-tasks \
    --cluster "$CLUSTER_NAME" \
    --tasks "$TASK_ARN" \
    --region "$REGION" \
    --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' \
    --output text)

PUBLIC_IP=$(aws ec2 describe-network-interfaces \
    --network-interface-ids "$ENI_ID" \
    --region "$REGION" \
    --query 'NetworkInterfaces[0].Association.PublicIp' \
    --output text 2>/dev/null || echo "")

if [ -n "$PUBLIC_IP" ] && [ "$PUBLIC_IP" != "None" ]; then
    log_success "Task public IP: $PUBLIC_IP"
    
    # Test health endpoint
    log_info "6. Testing health endpoint..."
    HEALTH_URL="http://$PUBLIC_IP:8080/api/actuator/health"
    log_info "Health URL: $HEALTH_URL"
    
    # Wait a moment for the service to be ready
    sleep 2
    
    HEALTH_RESPONSE=$(curl -s --max-time 5 "$HEALTH_URL" 2>/dev/null || echo "")
    
    if [ -n "$HEALTH_RESPONSE" ]; then
        echo "$HEALTH_RESPONSE" | jq . 2>/dev/null || echo "$HEALTH_RESPONSE"
        
        if echo "$HEALTH_RESPONSE" | jq -e '.status == "UP"' >/dev/null 2>&1; then
            log_success "Health check PASSED - API is healthy!"
        else
            log_warning "Health check response received but status is not UP"
        fi
    else
        log_warning "Could not reach health endpoint (may take a minute to start)"
    fi
    
    # Check application logs
    log_info "7. Checking recent application logs..."
    echo ""
    aws logs tail "/ecs/${ENVIRONMENT}-java-api" \
        --since 5m \
        --format short \
        --region "$REGION" 2>/dev/null | tail -20 || log_warning "Could not retrieve logs"
else
    log_warning "No public IP assigned to task"
fi

echo ""
log_success "Verification complete!"
echo ""
echo "📋 Summary:"
echo "   Cluster:      $CLUSTER_NAME"
echo "   Service:      $SERVICE_NAME"
echo "   Status:       $STATUS"
echo "   Tasks:        $RUNNING / $DESIRED"
echo "   Health:       $HEALTH_STATUS"
if [ -n "$PUBLIC_IP" ] && [ "$PUBLIC_IP" != "None" ]; then
    echo "   Public IP:    $PUBLIC_IP"
    echo "   Health URL:   http://$PUBLIC_IP:8080/api/actuator/health"
fi
echo ""

# Final status
if [ "$STATUS" = "ACTIVE" ] && [ "$RUNNING" -eq "$DESIRED" ] && [ "$HEALTH_STATUS" = "HEALTHY" ]; then
    log_success "✅ Java API is fully operational!"
    exit 0
elif [ "$STATUS" = "ACTIVE" ] && [ "$RUNNING" -eq "$DESIRED" ]; then
    log_warning "⚠️  Java API is running but health check is: $HEALTH_STATUS"
    exit 0
else
    log_warning "⚠️  Java API deployment needs attention"
    exit 1
fi
