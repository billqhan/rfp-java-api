[![CI/CD](https://github.com/billqhan/rfp-java-api/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/billqhan/rfp-java-api/actions)

# RFP Java API

A Java Spring Boot REST API service for the RFP Response Platform, providing endpoints for opportunity management, proposals, and workflows.

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

## 🚀 Quick Start - Automated Deployment

The easiest way to deploy this service is via the complete platform deployment:

```bash
# From rfp-infrastructure directory
cd ../rfp-infrastructure
./scripts/deploy-complete.sh dev
```

This will automatically:
1. Build the Docker container (multi-architecture)
2. Push to Amazon ECR
3. Create/update ECS service on Fargate
4. Integrate with Application Load Balancer
5. Configure health checks

## 📋 Prerequisites

- **Java 21**
- **Maven 3.9+**
- **Docker** (for containerized deployment)
- **AWS CLI** configured with appropriate credentials

## 🛠️ Local Development

### Build and Run Locally

```bash
# Build the application
mvn clean compile

# Run with Spring Boot
mvn spring-boot:run
```

Access at:
- Health check: http://localhost:8080/api/actuator/health
- API endpoints: http://localhost:8080/api/*

### Docker Compose (with LocalStack)

```bash
docker-compose up --build
```

Services:
- API: http://localhost:8080/api
- LocalStack: http://localhost:4566
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

### Automated Deployment (Recommended)

The service is deployed automatically via `deploy-complete.sh` from the rfp-infrastructure repo:

```bash
cd ../rfp-infrastructure
./scripts/deploy-complete.sh dev
```

This handles:
- Multi-architecture Docker build (linux/amd64, linux/arm64)
- ECR repository creation and image push
- ECS Fargate service creation/update
- ALB integration with target groups
- Health check configuration

### Manual ECS Deployment

If you need to deploy independently:

```bash
# Deploy ECS service only
./deploy-ecs.sh
```

The script will:
1. Build and push multi-arch Docker image to ECR
2. Register new ECS task definition
3. Create or update ECS service with ALB integration
4. Configure health checks on `/api/actuator/health`

### Build Options

```bash
# Build JAR only
./build.sh

# Build local Docker image (single architecture)
./build.sh --local

# Build and push multi-arch image to ECR
./build.sh --dockerx --skip-tests
```

### Troubleshooting

| Issue | Solution |
|-------|----------|
| CannotPullContainerError | Ensure multi-arch build with `./build.sh --dockerx` |
| Service stuck in PENDING | Check security group allows port 8080 and assignPublicIp=ENABLED |
| Health check failing | Verify `/api/actuator/health` returns HTTP 200 |
| ALB 502 errors | Check ECS task is running and security groups allow ALB → Task traffic |

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

## Architecture

- **Runtime**: ECS Fargate with Application Load Balancer
- **Container**: Multi-arch Docker (linux/amd64, linux/arm64)
- **Framework**: Spring Boot 3.2.0 with Java 21
- **AWS Integration**: DynamoDB, S3, Lambda via AWS SDK v2
- **Monitoring**: Spring Actuator with health endpoints

## License

Proprietary - All Rights Reserved