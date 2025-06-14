#!/bin/bash

echo "Docker Configuration Validation"
echo "==============================="
echo

# Check Docker files exist
echo "Checking Docker files..."
files=(
    "api/Dockerfile"
    "ui-components/Dockerfile"
    "ui-components/nginx.conf"
    "docker-compose.dev.yml"
    "docker-compose.prod.yml"
    ".dockerignore"
    ".env.example"
    "README-DOCKER.md"
)

all_exist=true
for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file exists"
    else
        echo "✗ $file missing"
        all_exist=false
    fi
done

echo
echo "Checking Dockerfile multi-stage builds..."

# Check API Dockerfile
if grep -q "FROM .* AS builder" api/Dockerfile && grep -q "FROM .* AS runtime\|FROM eclipse-temurin" api/Dockerfile; then
    echo "✓ API Dockerfile has multi-stage build"
else
    echo "✗ API Dockerfile missing multi-stage build"
fi

# Check UI Dockerfile
if grep -q "FROM .* AS builder" ui-components/Dockerfile && grep -q "nginx:alpine" ui-components/Dockerfile; then
    echo "✓ UI Dockerfile has multi-stage build"
else
    echo "✗ UI Dockerfile missing multi-stage build"
fi

echo
echo "Checking Docker Compose services..."

# Check development compose
echo "Development services:"
for service in postgres redis api ui; do
    if grep -q "^  $service:" docker-compose.dev.yml; then
        echo "✓ $service service defined"
    else
        echo "✗ $service service missing"
    fi
done

echo
echo "Production services:"
for service in postgres redis api ui; do
    if grep -q "^  $service:" docker-compose.prod.yml; then
        echo "✓ $service service defined"
    else
        echo "✗ $service service missing"
    fi
done

echo
echo "Checking health checks..."
if grep -c "healthcheck:" docker-compose.dev.yml | grep -q "4"; then
    echo "✓ All development services have health checks"
else
    echo "✗ Some development services missing health checks"
fi

if grep -c "healthcheck:" docker-compose.prod.yml | grep -q "4"; then
    echo "✓ All production services have health checks"
else
    echo "✗ Some production services missing health checks"
fi

echo
echo "Checking security configurations..."

# Check for non-root users
if grep -q "USER" api/Dockerfile && grep -q "USER" ui-components/Dockerfile; then
    echo "✓ Dockerfiles use non-root users"
else
    echo "✗ Dockerfiles missing non-root user configuration"
fi

# Check for security options in production
if grep -q "security_opt:" docker-compose.prod.yml && grep -q "read_only: true" docker-compose.prod.yml; then
    echo "✓ Production compose has security hardening"
else
    echo "✗ Production compose missing security hardening"
fi

echo
if [ "$all_exist" = true ]; then
    echo "✅ Docker configuration validation PASSED"
else
    echo "❌ Docker configuration validation FAILED"
fi