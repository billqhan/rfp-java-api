#!/bin/bash

# RFP Response Agent Java API Build Script

set -euo pipefail

echo "🚀 Building RFP Response Agent Java API..."

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Optional: load project env for ECR defaults
if [[ -f "$ROOT_DIR/.env.dev" ]]; then
    # shellcheck disable=SC1090
    source "$ROOT_DIR/.env.dev"
fi

REGION=${REGION:-${AWS_REGION:-us-east-1}}
BUCKET_PREFIX=${BUCKET_PREFIX:-dev}
REPO_NAME="${BUCKET_PREFIX}-rfp-java-api"

usage() {
    cat <<USAGE
Usage: $(basename "$0") [options]

Options:
    --local          Build only the JAR (default) and a local single-arch Docker image 'rfp-api:local'
    --dockerx        Build and PUSH multi-arch image to ECR (linux/amd64,linux/arm64)
    --skip-tests     Skip running unit tests during Maven build
    -h, --help       Show this help

Examples:
    # Build JAR + local Docker image
    ./build.sh --local

    # Build and push multi-arch image to ECR
    ./build.sh --dockerx
USAGE
}

MODE="jar-only"
SKIP_TESTS="false"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --local)
            MODE="local-docker"
            shift
            ;;
        --dockerx)
            MODE="ecr-multiarch"
            shift
            ;;
        --skip-tests)
            SKIP_TESTS="true"
            shift
            ;;
        -h|--help)
            usage; exit 0
            ;;
        *)
            echo "Unknown option: $1"; usage; exit 1
            ;;
    esac
done

# Check if Maven is installed
if ! command -v mvn >/dev/null 2>&1; then
    echo "❌ Maven is not installed. Please install Maven 3.8+ to continue."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | sed -n 's/.*version "\([0-9][0-9]*\.[0-9].*\)".*/\1/p' | head -1)
if [[ -z "$JAVA_VERSION" ]]; then
    echo "❌ Unable to determine Java version (need 17+)."; exit 1
fi
if [[ ${JAVA_VERSION%%.*} -lt 17 ]]; then
    echo "❌ Java 17 or higher is required. Current version: $JAVA_VERSION"; exit 1
fi
echo "✅ Java version: $JAVA_VERSION"

MVN_TEST_FLAG=""
[[ "$SKIP_TESTS" == "true" ]] && MVN_TEST_FLAG="-DskipTests"

# Clean, compile, test, package
echo "🧹 Cleaning previous builds..."; mvn clean
echo "🔨 Compiling sources..."; mvn compile
if [[ "$SKIP_TESTS" != "true" ]]; then
    echo "🧪 Running tests..."; mvn test
fi
echo "📦 Packaging application..."; mvn package $MVN_TEST_FLAG

# Check if JAR was created
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
if [[ ! -f "$JAR_FILE" ]]; then
    echo "❌ Build failed! JAR file not found."; exit 1
fi

echo "✅ Build successful! JAR created: $JAR_FILE"
echo "📊 Local endpoints:"
echo "   • Health: http://localhost:8080/api/health"
echo "   • Dashboard: http://localhost:8080/api/dashboard/metrics"

if [[ "$MODE" == "jar-only" ]]; then
    echo "ℹ️  JAR build complete. Use '--local' to build a Docker image or '--dockerx' to push a multi-arch image to ECR."
    exit 0
fi

# Docker present?
if ! command -v docker >/dev/null 2>&1; then
    echo "❌ Docker is required for container builds."; exit 1
fi

if [[ "$MODE" == "local-docker" ]]; then
    echo "🐳 Building local single-arch Docker image: rfp-api:local"
    docker build -t rfp-api:local .
    echo "✅ Local image built: rfp-api:local"
    exit 0
fi

if [[ "$MODE" == "ecr-multiarch" ]]; then
    # Require AWS CLI for ECR, get account id
    if ! command -v aws >/dev/null 2>&1; then
        echo "❌ AWS CLI is required for pushing to ECR."; exit 1
    fi
    AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    IMAGE_URI="$AWS_ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$REPO_NAME:latest"

    echo "🔐 Logging into ECR: $AWS_ACCOUNT_ID @ $REGION"
    aws ecr describe-repositories --repository-names "$REPO_NAME" --region "$REGION" >/dev/null 2>&1 || \
        aws ecr create-repository --repository-name "$REPO_NAME" --region "$REGION" >/dev/null
    aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"

    # Ensure buildx builder
    if ! docker buildx inspect multiarch >/dev/null 2>&1; then
        echo "🧰 Creating docker buildx builder 'multiarch'"
        docker buildx create --name multiarch --driver docker-container --use
    else
        docker buildx use multiarch
    fi

    # Enable emulation (optional)
    docker run --rm --privileged tonistiigi/binfmt --install all >/dev/null 2>&1 || true

    echo "🏗️  Building and pushing multi-arch image to ECR: $IMAGE_URI"
    docker buildx build \
        --platform linux/amd64,linux/arm64 \
        --progress plain \
        -t "$IMAGE_URI" \
        --push .

    echo "✅ Pushed multi-arch image: $IMAGE_URI"
    echo "ℹ️  Deploy with: ./deploy-complete.sh java-api"
fi

exit 0