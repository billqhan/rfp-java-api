# ECS Deployment Configuration Complete ✅

**Date:** November 22, 2025  
**Status:** ECS configured as primary deployment, EKS workflow disabled but kept in repo

## What Changed

### rfp-java-api

✅ **Primary Workflow: ci-cd-ecs.yml** (Active)
- Renamed to "CI/CD Pipeline" (primary)
- Uses standard `development` and `production` environments
- Triggers on push to main/develop branches
- Deploys to ECS Fargate

✅ **EKS Workflow: ci-cd.yml** (Disabled, Kept for Reference)
- Renamed to "CI/CD Pipeline (EKS - DISABLED)"
- Only triggers on manual `workflow_dispatch`
- Automatic triggers commented out
- Can be re-enabled by uncommenting trigger conditions

✅ **New Files Created**
- `update-task-definitions.sh` - Script to inject AWS account ID
- `QUICKSTART-ECS.md` - Fast setup guide with all commands
- `ECS-SETUP.md` - Detailed infrastructure guide
- `task-definition-dev.json` - Dev ECS task config
- `task-definition-prod.json` - Prod ECS task config

## Quick Setup Commands

### 1. Update Task Definitions
```bash
cd rfp-java-api
./update-task-definitions.sh
```

### 2. Create Minimal ECS Infrastructure
```bash
# Set your prefix
export ENV_PREFIX="dev"
export ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)

# Create cluster
aws ecs create-cluster --cluster-name ${ENV_PREFIX}-ecs-cluster --region us-east-1

# Create log group
aws logs create-log-group --log-group-name /ecs/${ENV_PREFIX}-java-api --region us-east-1

# Create IAM roles (see QUICKSTART-ECS.md for full commands)
# - ecsTaskExecutionRole
# - ecsTaskRole

# Register task definition
aws ecs register-task-definition --cli-input-json file://task-definition-dev.json

# Create service (requires VPC/subnet/security group - see QUICKSTART-ECS.md)
```

### 3. Configure GitHub Secrets
```bash
gh secret set AWS_ACCESS_KEY_ID --repo billqhan/rfp-java-api
gh secret set AWS_SECRET_ACCESS_KEY --repo billqhan/rfp-java-api
gh secret set AWS_REGION --body "us-east-1" --repo billqhan/rfp-java-api
gh secret set ECR_REGISTRY --body "${ACCOUNT_ID}.dkr.ecr.us-east-1.amazonaws.com" --repo billqhan/rfp-java-api
gh secret set ECS_CLUSTER_DEV --body "${ENV_PREFIX}-ecs-cluster" --repo billqhan/rfp-java-api
gh secret set ECS_SERVICE_DEV --body "${ENV_PREFIX}-java-api-service" --repo billqhan/rfp-java-api
gh secret set ECS_TASK_FAMILY_DEV --body "${ENV_PREFIX}-java-api-task" --repo billqhan/rfp-java-api

# Repeat for PROD secrets
```

### 4. Deploy
```bash
cd rfp-java-api

# Commit changes
git add .
git commit -m "feat: configure ECS as primary deployment"

# Deploy to dev
git checkout develop
git push origin develop

# Watch deployment
# https://github.com/billqhan/rfp-java-api/actions
```

## Workflow Behavior

| Event | ECS Workflow | EKS Workflow |
|-------|--------------|--------------|
| Push to `develop` | ✅ Deploys to dev ECS | ⏸️ No action |
| Push to `main` | ✅ Deploys to prod ECS | ⏸️ No action |
| Pull Request | ✅ Runs build/test | ⏸️ No action |
| Manual Trigger | ✅ Available | ✅ Available |

## Files Ready to Commit

```bash
cd rfp-java-api

# Modified files
modified:   .github/workflows/ci-cd.yml          # EKS disabled
modified:   .github/workflows/ci-cd-ecs.yml      # Primary workflow

# New files
new file:   task-definition-dev.json
new file:   task-definition-prod.json
new file:   update-task-definitions.sh
new file:   QUICKSTART-ECS.md
new file:   ECS-SETUP.md
```

## GitHub Environments Needed

1. **development** (for ECS dev deployments)
   - No protection rules
   - URL: Dev ECS endpoint

2. **production** (for ECS prod deployments)
   - Required reviewers: 1+
   - Deployment branch: `main` only
   - URL: Prod ECS endpoint

## Required GitHub Secrets

### Common (all repos)
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`

### rfp-java-api specific
- `ECR_REGISTRY`
- `ECS_CLUSTER_DEV`
- `ECS_CLUSTER_PROD`
- `ECS_SERVICE_DEV`
- `ECS_SERVICE_PROD`
- `ECS_TASK_FAMILY_DEV`
- `ECS_TASK_FAMILY_PROD`
- `API_ENDPOINT_PROD` (optional)

## How to Re-enable EKS (if needed later)

Edit `.github/workflows/ci-cd.yml`:

```yaml
name: CI/CD Pipeline (EKS)

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]
  workflow_dispatch:
```

Change ECS workflow environments back to `development-ecs` and `production-ecs` to avoid conflicts.

## Testing Checklist

- [ ] Task definitions updated with real AWS account ID
- [ ] ECS cluster created
- [ ] IAM roles created
- [ ] Log groups created
- [ ] Task definitions registered
- [ ] ECS service created
- [ ] All GitHub secrets configured
- [ ] GitHub environments created
- [ ] Pushed to develop branch
- [ ] Verified deployment in GitHub Actions
- [ ] Verified task running in ECS
- [ ] Tested API endpoint
- [ ] Checked CloudWatch logs

## Documentation

- **Quick Start**: `QUICKSTART-ECS.md` - All commands in one place
- **Detailed Setup**: `ECS-SETUP.md` - Complete infrastructure guide
- **Secrets Reference**: `../rfp-infrastructure/GITHUB-SECRETS-SETUP.md`
- **CI/CD Overview**: `../rfp-infrastructure/CI-CD-PIPELINES-COMPLETE.md`

## Support

If you encounter issues:
1. Check `QUICKSTART-ECS.md` for common setup commands
2. Review `ECS-SETUP.md` troubleshooting section
3. Verify all secrets: `gh secret list --repo billqhan/rfp-java-api`
4. Check CloudWatch logs: `aws logs tail /ecs/${ENV_PREFIX}-java-api --follow`

---

**Ready to deploy!** Follow `QUICKSTART-ECS.md` for step-by-step commands.
