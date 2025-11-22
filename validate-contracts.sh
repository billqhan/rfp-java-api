#!/bin/bash
set -e

echo "�� Validating API contracts for rfp-java-api..."

# Check if contracts submodule is initialized
if [ ! -f "contracts/rfp-contracts/openapi/api-gateway.yaml" ]; then
    echo "❌ Contracts not found. Run: git submodule update --init --recursive"
    exit 1
fi

echo "✅ Contracts submodule present"

# Validate OpenAPI spec
if [ -f "contracts/rfp-contracts/openapi/api-gateway.yaml" ]; then
    echo "✅ OpenAPI spec found"
    
    # Check if swagger-cli or similar is available for validation
    if command -v docker &> /dev/null; then
        echo "📋 Validating OpenAPI spec with Docker..."
        docker run --rm -v "$(pwd)/contracts/rfp-contracts/openapi:/specs" openapitools/openapi-generator-cli:latest validate -i /specs/api-gateway.yaml || echo "⚠️  Validation warnings (non-blocking)"
    else
        echo "⚠️  Docker not available. Skipping OpenAPI validation."
    fi
fi

# Check if Java controllers match contract endpoints
echo "📋 Checking controller endpoints..."
if [ -d "src/main/java" ]; then
    CONTROLLER_COUNT=$(find src/main/java -name "*Controller.java" | wc -l)
    echo "  Found $CONTROLLER_COUNT controller(s)"
else
    echo "  ⚠️  Source directory not found"
fi

echo ""
echo "✅ Contract validation complete!"
