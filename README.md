[![CI/CD](https://github.com/billqhan/rfp-java-api/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/billqhan/rfp-java-api/actions)
# RFP Response Agent Java REST API

A Java Spring Boot REST API service that provides comprehensive endpoints for the RFP Response Agent platform, mirroring the functionality of the existing Lambda-based API but with enhanced features and enterprise-grade capabilities.

## 🚀 Features

### Core API Endpoints
- **Health Check**: System status and health monitoring
- **Dashboard**: Metrics, analytics, and overview data
- **Opportunities**: RFP/RFQ opportunity management and search
- **Proposals**: Proposal creation, management, and tracking
- **Workflows**: Automated workflow execution and monitoring

### Enterprise Features
- **Spring Boot**: Production-ready framework with built-in monitoring
- **AWS Integration**: Native AWS SDK v2 integration
- **CORS Support**: Cross-origin resource sharing configuration
- **Docker Support**: Containerized deployment ready
- **Health Monitoring**: Actuator endpoints for health checks
- **Metrics**: Prometheus-compatible metrics export
- **Logging**: Structured logging with configurable levels

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.8+
- Docker (optional, for containerized deployment)
- AWS CLI configured (for AWS service access)

## 🛠️ Quick Start

### Local Development

1. **Clone and navigate to the project**:
   ```bash
   cd java-api
   ```

2. **Build the application**:
   ```bash
   mvn clean compile
   ```

3. **Run the application**:
   ```bash
   mvn spring-boot:run
   - Health check: http://localhost:8080/api/health
   - Swagger UI: http://localhost:8080/api/swagger-ui.html (if enabled)

   ```bash
   docker-compose up --build
   ```

2. **Access services**:
   - API: http://localhost:8080/api
   - LocalStack (AWS services): http://localhost:4566
   - DynamoDB Admin: http://localhost:8001

## 📖 API Documentation

### Health & Monitoring
- `GET /health` - Health check and system status

### Dashboard
- `GET /dashboard/metrics` - Dashboard metrics and analytics

### Opportunities
- `GET /opportunities` - List opportunities with pagination and filtering
- `GET /opportunities/{id}` - Get specific opportunity details
- `GET /opportunities/search` - Search opportunities
### Proposals
- `GET /proposals` - List proposals with pagination
- `GET /proposals/{id}` - Get specific proposal
- `POST /proposals` - Create new proposal
- `PUT /proposals/{id}` - Update existing proposal
- `DELETE /proposals/{id}` - Delete proposal
- `GET /proposals/by-opportunity/{opportunityId}` - Get proposals for opportunity

### Workflows
- `POST /workflow/{step}` - Trigger workflow step (download, process, match, reports, notify)
- `GET /workflow/status` - Get current workflow status
- `GET /workflow/history` - Get workflow execution history

## ⚙️ Configuration

### Application Properties
Key configuration options in `application.yml`:

```yaml
rfp:
  api:
    aws:
      region: us-east-1
      environment: dev
      project-prefix: l3harris-qhan
    processing:
      match-threshold: 0.7
      max-results: 100
      company-name: "L3Harris Technologies"
    storage:
      enable-local-storage: true
      enable-cloud-storage: true
```

### Environment Variables
- `RFP_API_AWS_REGION` - AWS region
- `RFP_API_AWS_ENVIRONMENT` - Environment (dev/staging/prod)
- `RFP_API_AWS_PROJECT_PREFIX` - Project prefix for AWS resources
- `SPRING_PROFILES_ACTIVE` - Spring profiles to activate

## 🏗️ Architecture

### Technology Stack
- **Framework**: Spring Boot 3.2.0
- **Java Version**: 17
- **AWS SDK**: v2.21.29
- **Build Tool**: Maven
- **Containerization**: Docker with multi-stage builds

### AWS Services Integration
- **DynamoDB**: Proposal storage and management
- **S3**: Opportunity data and file storage
- **Lambda**: Workflow step execution
- **SQS**: Message queuing for workflows

### Design Patterns
- **REST API**: RESTful endpoint design
- **Service Layer**: Business logic separation
- **Configuration Properties**: Type-safe configuration
- **Dependency Injection**: Spring's IoC container

## 🚀 Deployment

### Local Deployment
```bash
# Build JAR
mvn clean package

# Run JAR
java -jar target/rfp-response-agent-api-*.jar
```

### Docker Deployment
```bash
# Build image
docker build -t l3harris/rfp-response-agent-api:1.0.0 .

# Run container
docker run -p 8080:8080 l3harris/rfp-response-agent-api:1.0.0
```

### AWS Deployment Options
1. **AWS ECS/Fargate**: Container orchestration
2. **AWS EKS**: Kubernetes deployment
3. **AWS Elastic Beanstalk**: Platform-as-a-Service
4. **AWS EC2**: Direct instance deployment

### Multi-Architecture Container Builds (Apple Silicon + ECS AMD64)

The ECS service expects an image that includes a linux/amd64 descriptor. When building from an Apple Silicon (arm64) Mac, a single-arch image will cause `CannotPullContainerError: image manifest does not contain a descriptor for platform (linux/amd64)`.

Use the enhanced build script:

```bash
cd java-api
# Build only the JAR
./build.sh

# Build local single-arch Docker image (for local use)
./build.sh --local

# Build & push multi-arch image to ECR (linux/amd64, linux/arm64)
./build.sh --dockerx --skip-tests
```

The multi-arch build script will:
- Ensure the ECR repo exists (`<BUCKET_PREFIX>-rfp-java-api`)
- Create / select a `docker buildx` builder named `multiarch`
- Push a manifest list tagged `:latest`

ECS Task Definition runtimePlatform (already set in `deploy-complete.sh`):
```json
"runtimePlatform": { "cpuArchitecture": "X86_64", "operatingSystemFamily": "LINUX" }
```

If you temporarily need to force amd64 locally (without multi-arch push):
```bash
docker build --platform linux/amd64 -t rfp-api:amd64 .
```

### ECS Deployment Flow

1. Run: `./deploy-complete.sh java-api`
2. Script builds & pushes multi-arch image (if not already) and registers new task definition
3. ECS service is updated with `--force-new-deployment`
4. Retrieve public IP:
   ```bash
   CLUSTER="dev-ecs-cluster" # or ${BUCKET_PREFIX}-ecs-cluster
   SERVICE="dev-java-api-service" # or ${BUCKET_PREFIX}-java-api-service
   TASK_ARN=$(aws ecs list-tasks --cluster "$CLUSTER" --service-name "$SERVICE" --query 'taskArns[0]' --output text)
   ENI=$(aws ecs describe-tasks --cluster "$CLUSTER" --tasks "$TASK_ARN" --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`].value' --output text)
   aws ec2 describe-network-interfaces --network-interface-ids "$ENI" --query 'NetworkInterfaces[0].Association.PublicIp' --output text
   ```

### Troubleshooting ECS Pulls

| Symptom | Cause | Fix |
|---------|-------|-----|
| CannotPullContainerError (no amd64 descriptor) | Built image only for arm64 | Rebuild with `./build.sh --dockerx` |
| Stuck in PENDING | No public subnets or security group rules | Ensure assignPublicIp=ENABLED & SG allows 8080/tcp |
| Health check failing | App not ready or wrong path | Confirm `/api/health` returns 200 |
| Old task definitions piling up | Frequent deploys | Deregister periodically (script already cleans on request) |

### Hardening / Next Steps (Optional)
- Add ALB + target group for stable DNS + health routing
- Add AWS WAF on ALB
- Externalize config via SSM Parameter Store or Secrets Manager
- Implement structured JSON logging shipped to CloudWatch Logs Insights
- Add OpenAPI auto-generation & Swagger UI (if not already enabled)
- Introduce CI pipeline (GitHub Actions) for build/test/push/deploy

## 📊 Monitoring & Observability

### Health Endpoints
- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics format

### Logging
- Structured JSON logging in production
- Configurable log levels per package
- AWS CloudWatch integration ready

## 🔒 Security Considerations

- **CORS Configuration**: Configurable cross-origin policies
- **Input Validation**: Request body validation
- **AWS IAM**: Role-based access to AWS services
- **Environment Variables**: Sensitive configuration externalized

## 🧪 Testing

### Run Tests
```bash
# Unit tests
mvn test

# Integration tests with TestContainers
mvn verify
```

### Test Coverage
- Unit tests for service layer
- Integration tests with LocalStack
- API endpoint testing

## 📈 Performance

### Optimization Features
- **Connection Pooling**: AWS SDK connection reuse
- **Lazy Loading**: On-demand resource initialization
- **Caching**: Strategic caching of frequently accessed data
- **Async Processing**: Non-blocking I/O for workflows

### JVM Tuning
Container-optimized JVM settings:
- G1 garbage collector
- Container-aware memory allocation
- String deduplication enabled

## 🤝 Integration with Existing System

This Java API seamlessly integrates with the existing Lambda-based architecture:

1. **API Compatibility**: Maintains the same REST endpoints as the Lambda API
2. **AWS Resource Access**: Uses the same DynamoDB tables and S3 buckets
3. **Workflow Integration**: Invokes existing Lambda functions for processing steps
4. **Data Format Compatibility**: Uses the same JSON schemas and data structures

## 📋 Next Steps

1. **Deploy to AWS**: Set up ECS/EKS deployment
2. **Add Authentication**: Implement AWS Cognito or OAuth2
3. **Enhance Monitoring**: Add distributed tracing with X-Ray
4. **Performance Testing**: Load testing and optimization
5. **CI/CD Pipeline**: Automated build and deployment pipeline

## 📞 Support

For questions or issues:
- Review the existing Lambda API implementation for reference
- Check AWS CloudWatch logs for deployment issues
- Refer to Spring Boot documentation for framework-specific questions