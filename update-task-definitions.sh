#!/bin/bash

# Script to update task definition templates with your AWS account ID
# Run this script before committing task definitions

set -e

echo "🔧 Updating ECS task definition templates..."

# Get AWS account ID
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text 2>/dev/null)

if [ -z "$ACCOUNT_ID" ]; then
    echo "❌ Error: Unable to get AWS account ID. Please ensure AWS CLI is configured."
    echo "Run: aws configure"
    exit 1
fi

echo "✓ AWS Account ID: $ACCOUNT_ID"

# Update dev task definition
if [ -f "task-definition-dev.json" ]; then
    sed -i.bak "s/YOUR_ACCOUNT_ID/$ACCOUNT_ID/g" task-definition-dev.json
    echo "✓ Updated task-definition-dev.json"
    rm -f task-definition-dev.json.bak
else
    echo "⚠ Warning: task-definition-dev.json not found"
fi

# Update prod task definition
if [ -f "task-definition-prod.json" ]; then
    sed -i.bak "s/YOUR_ACCOUNT_ID/$ACCOUNT_ID/g" task-definition-prod.json
    echo "✓ Updated task-definition-prod.json"
    rm -f task-definition-prod.json.bak
else
    echo "⚠ Warning: task-definition-prod.json not found"
fi

echo ""
echo "✅ Task definitions updated successfully!"
echo ""
echo "Next steps:"
echo "1. Review the updated task definitions"
echo "2. Verify IAM role ARNs are correct"
echo "3. Commit and push changes"
echo ""
echo "Commands:"
echo "  git add task-definition-*.json"
echo "  git commit -m 'chore: update task definitions with account ID'"
echo "  git push"
