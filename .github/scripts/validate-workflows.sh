#!/bin/bash

# Script to validate GitHub Actions workflows

set -euo pipefail

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

WORKFLOWS_DIR=".github/workflows"
ERRORS=0

echo -e "${GREEN}Validating GitHub Actions workflows...${NC}\n"

# Function to check if a file exists
check_file_exists() {
    local file=$1
    if [[ ! -f "$file" ]]; then
        echo -e "${RED}✗ File not found: $file${NC}"
        ((ERRORS++))
        return 1
    fi
    return 0
}

# Function to check required workflow keys
check_workflow_structure() {
    local file=$1
    echo -e "${YELLOW}Checking $file...${NC}"
    
    # Check for required top-level keys
    if ! grep -q "^name:" "$file"; then
        echo -e "${RED}  ✗ Missing 'name' field${NC}"
        ((ERRORS++))
    fi
    
    if ! grep -q "^on:" "$file"; then
        echo -e "${RED}  ✗ Missing 'on' trigger field${NC}"
        ((ERRORS++))
    fi
    
    if ! grep -q "^jobs:" "$file"; then
        echo -e "${RED}  ✗ Missing 'jobs' field${NC}"
        ((ERRORS++))
    fi
    
    # Check for proper indentation (should use spaces, not tabs)
    if grep -q $'\t' "$file"; then
        echo -e "${RED}  ✗ File contains tabs (should use spaces)${NC}"
        ((ERRORS++))
    fi
    
    # Check for deprecated actions
    if grep -q "actions/checkout@v[1-3]" "$file"; then
        echo -e "${RED}  ✗ Using deprecated checkout action version${NC}"
        ((ERRORS++))
    fi
    
    if grep -q "actions/setup-java@v[1-3]" "$file"; then
        echo -e "${RED}  ✗ Using deprecated setup-java action version${NC}"
        ((ERRORS++))
    fi
    
    if grep -q "actions/upload-artifact@v[1-3]" "$file"; then
        echo -e "${RED}  ✗ Using deprecated upload-artifact action version${NC}"
        ((ERRORS++))
    fi
    
    if grep -q "actions/download-artifact@v[1-3]" "$file"; then
        echo -e "${RED}  ✗ Using deprecated download-artifact action version${NC}"
        ((ERRORS++))
    fi
    
    echo -e "${GREEN}  ✓ Basic structure check complete${NC}"
}

# Function to validate specific workflow requirements
validate_main_ci() {
    local file="$WORKFLOWS_DIR/main-ci.yml"
    echo -e "\n${YELLOW}Validating Main CI workflow...${NC}"
    
    if ! check_file_exists "$file"; then
        return
    fi
    
    # Check for required jobs
    local required_jobs=("code-quality" "unit-tests" "integration-tests" "security-scan" "build-artifacts")
    for job in "${required_jobs[@]}"; do
        if ! grep -q "  $job:" "$file"; then
            echo -e "${RED}  ✗ Missing required job: $job${NC}"
            ((ERRORS++))
        else
            echo -e "${GREEN}  ✓ Found job: $job${NC}"
        fi
    done
    
    # Check for PR comment functionality
    if ! grep -q "actions/github-script" "$file"; then
        echo -e "${YELLOW}  ⚠ Missing PR comment functionality${NC}"
    fi
}

validate_docker_build() {
    local file="$WORKFLOWS_DIR/docker-build.yml"
    echo -e "\n${YELLOW}Validating Docker Build workflow...${NC}"
    
    if ! check_file_exists "$file"; then
        return
    fi
    
    # Check for Docker-specific actions
    if ! grep -q "docker/setup-buildx-action" "$file"; then
        echo -e "${RED}  ✗ Missing Docker buildx setup${NC}"
        ((ERRORS++))
    fi
    
    if ! grep -q "docker/login-action" "$file"; then
        echo -e "${RED}  ✗ Missing Docker login action${NC}"
        ((ERRORS++))
    fi
    
    if ! grep -q "docker/build-push-action" "$file"; then
        echo -e "${RED}  ✗ Missing Docker build-push action${NC}"
        ((ERRORS++))
    fi
    
    # Check for multi-platform builds
    if ! grep -q "platforms:" "$file"; then
        echo -e "${YELLOW}  ⚠ No multi-platform build configuration${NC}"
    fi
}

validate_deployment() {
    local file="$WORKFLOWS_DIR/deploy.yml"
    echo -e "\n${YELLOW}Validating Deployment workflow...${NC}"
    
    if ! check_file_exists "$file"; then
        return
    fi
    
    # Check for environment configuration
    if ! grep -q "environment:" "$file"; then
        echo -e "${RED}  ✗ Missing environment configuration${NC}"
        ((ERRORS++))
    fi
    
    # Check for manual trigger
    if ! grep -q "workflow_dispatch:" "$file"; then
        echo -e "${RED}  ✗ Missing manual trigger (workflow_dispatch)${NC}"
        ((ERRORS++))
    fi
}

validate_matrix_build() {
    local file="$WORKFLOWS_DIR/matrix-build.yml"
    echo -e "\n${YELLOW}Validating Matrix Build workflow...${NC}"
    
    if ! check_file_exists "$file"; then
        return
    fi
    
    # Check for matrix strategy
    if ! grep -q "strategy:" "$file"; then
        echo -e "${RED}  ✗ Missing matrix strategy${NC}"
        ((ERRORS++))
    fi
    
    if ! grep -q "matrix:" "$file"; then
        echo -e "${RED}  ✗ Missing matrix configuration${NC}"
        ((ERRORS++))
    fi
}

# Function to check for security best practices
check_security_practices() {
    echo -e "\n${YELLOW}Checking security best practices...${NC}"
    
    for file in "$WORKFLOWS_DIR"/*.yml; do
        if [[ -f "$file" ]]; then
            # Check for hardcoded secrets
            if grep -E "(password|token|key|secret)\\s*:\\s*['\"]?[A-Za-z0-9]+" "$file" | grep -v "secrets\." | grep -v "{{"; then
                echo -e "${RED}  ✗ Possible hardcoded secret in $file${NC}"
                ((ERRORS++))
            fi
            
            # Check for proper permissions
            if grep -q "permissions:" "$file"; then
                echo -e "${GREEN}  ✓ Permissions defined in $(basename "$file")${NC}"
            else
                echo -e "${YELLOW}  ⚠ No explicit permissions in $(basename "$file")${NC}"
            fi
        fi
    done
}

# Main validation
main() {
    # Check if workflows directory exists
    if [[ ! -d "$WORKFLOWS_DIR" ]]; then
        echo -e "${RED}✗ Workflows directory not found: $WORKFLOWS_DIR${NC}"
        exit 1
    fi
    
    # Validate each workflow file structure
    for file in "$WORKFLOWS_DIR"/*.yml; do
        if [[ -f "$file" ]]; then
            check_workflow_structure "$file"
        fi
    done
    
    # Validate specific workflows
    validate_main_ci
    validate_docker_build
    validate_deployment
    validate_matrix_build
    
    # Check security practices
    check_security_practices
    
    # Summary
    echo -e "\n${GREEN}=====================================

${NC}"
    if [[ $ERRORS -eq 0 ]]; then
        echo -e "${GREEN}✓ All validations passed!${NC}"
        exit 0
    else
        echo -e "${RED}✗ Found $ERRORS errors in workflow files${NC}"
        echo -e "${YELLOW}Please fix the errors above before committing${NC}"
        exit 1
    fi
}

# Run main function
main "$@"