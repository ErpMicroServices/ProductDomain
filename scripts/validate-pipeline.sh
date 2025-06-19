#!/bin/bash

# Pipeline Validation Script
# This script validates all CI/CD checks locally before pushing
# It ensures that code will pass GitHub Actions checks

set -e  # Exit on any error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    case $1 in
        "info")
            echo -e "${BLUE}[INFO]${NC} $2"
            ;;
        "success")
            echo -e "${GREEN}[✓]${NC} $2"
            ;;
        "warning")
            echo -e "${YELLOW}[!]${NC} $2"
            ;;
        "error")
            echo -e "${RED}[✗]${NC} $2"
            ;;
    esac
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check Java version
check_java_version() {
    print_status "info" "Checking Java version..."
    
    if ! command_exists java; then
        print_status "error" "Java is not installed!"
        return 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d. -f1)
    if [ "$JAVA_VERSION" -eq 21 ]; then
        print_status "success" "Java 21 detected"
    else
        print_status "warning" "Java $JAVA_VERSION detected. CI/CD uses Java 21"
        print_status "info" "Consider using: export JAVA_HOME=\$(/usr/libexec/java_home -v 21)"
    fi
}

# Check if running in git repository
check_git_repo() {
    print_status "info" "Checking git repository..."
    
    if [ ! -d .git ]; then
        print_status "error" "Not in a git repository!"
        exit 1
    fi
    
    # Check for uncommitted changes
    if ! git diff-index --quiet HEAD --; then
        print_status "warning" "You have uncommitted changes"
        git status --short
    else
        print_status "success" "Working directory is clean"
    fi
}

# Run compilation
run_compilation() {
    print_status "info" "Running compilation..."
    
    if ./gradlew clean compileJava compileTestJava; then
        print_status "success" "Compilation successful"
    else
        print_status "error" "Compilation failed!"
        return 1
    fi
}

# Run checkstyle
run_checkstyle() {
    print_status "info" "Running Checkstyle..."
    
    if ./gradlew checkstyleMain checkstyleTest; then
        print_status "success" "Checkstyle passed"
    else
        print_status "error" "Checkstyle violations found!"
        print_status "info" "Check reports in */build/reports/checkstyle/"
        return 1
    fi
}

# Run PMD
run_pmd() {
    print_status "info" "Running PMD..."
    
    if ./gradlew pmdMain pmdTest; then
        print_status "success" "PMD passed"
    else
        print_status "error" "PMD violations found!"
        print_status "info" "Check reports in */build/reports/pmd/"
        return 1
    fi
}

# Run SpotBugs
run_spotbugs() {
    print_status "info" "Running SpotBugs..."
    
    if ./gradlew spotbugsMain spotbugsTest; then
        print_status "success" "SpotBugs passed"
    else
        print_status "error" "SpotBugs violations found!"
        print_status "info" "Check reports in */build/reports/spotbugs/"
        return 1
    fi
}

# Run unit tests
run_unit_tests() {
    print_status "info" "Running unit tests..."
    
    # Set environment for CI profile if database tests exist
    export SPRING_PROFILES_ACTIVE=ci
    
    if ./gradlew test; then
        print_status "success" "Unit tests passed"
    else
        print_status "error" "Unit tests failed!"
        print_status "info" "Check reports in */build/reports/tests/"
        return 1
    fi
}

# Run integration tests (if available)
run_integration_tests() {
    print_status "info" "Checking for integration tests..."
    
    if ./gradlew tasks | grep -q "integrationTest"; then
        print_status "info" "Running integration tests..."
        if ./gradlew integrationTest; then
            print_status "success" "Integration tests passed"
        else
            print_status "error" "Integration tests failed!"
            return 1
        fi
    else
        print_status "info" "No integration tests configured"
    fi
}

# Check for security vulnerabilities
run_security_check() {
    print_status "info" "Running OWASP dependency check..."
    
    if ./gradlew dependencyCheckAnalyze; then
        print_status "success" "Security scan completed"
        
        # Check for high severity vulnerabilities
        if grep -q "VULNERABILITY" build/reports/dependency-check/dependency-check-report.html 2>/dev/null; then
            print_status "warning" "Vulnerabilities found in dependencies"
            print_status "info" "Check report at build/reports/dependency-check/dependency-check-report.html"
        fi
    else
        print_status "warning" "Security scan failed (non-critical)"
    fi
}

# Build artifacts
run_build() {
    print_status "info" "Building artifacts..."
    
    if ./gradlew build -x test; then
        print_status "success" "Build successful"
    else
        print_status "error" "Build failed!"
        return 1
    fi
}

# Main execution
main() {
    echo "======================================"
    echo "   Local Pipeline Validation Script   "
    echo "======================================"
    echo ""
    
    # Track failures
    FAILED_CHECKS=()
    
    # Run all checks
    check_java_version || FAILED_CHECKS+=("Java version")
    check_git_repo
    
    print_status "info" "Starting validation checks..."
    echo ""
    
    # Compilation must pass
    if ! run_compilation; then
        print_status "error" "Compilation failed - stopping validation"
        exit 1
    fi
    
    # Quality checks
    run_checkstyle || FAILED_CHECKS+=("Checkstyle")
    run_pmd || FAILED_CHECKS+=("PMD")
    run_spotbugs || FAILED_CHECKS+=("SpotBugs")
    
    # Tests
    run_unit_tests || FAILED_CHECKS+=("Unit tests")
    run_integration_tests || FAILED_CHECKS+=("Integration tests")
    
    # Security
    run_security_check || true  # Don't fail on security check
    
    # Build
    run_build || FAILED_CHECKS+=("Build")
    
    echo ""
    echo "======================================"
    echo "         Validation Summary           "
    echo "======================================"
    
    if [ ${#FAILED_CHECKS[@]} -eq 0 ]; then
        print_status "success" "All checks passed! ✨"
        print_status "info" "Your code is ready for CI/CD"
        exit 0
    else
        print_status "error" "The following checks failed:"
        for check in "${FAILED_CHECKS[@]}"; do
            echo "  - $check"
        done
        print_status "info" "Fix these issues before pushing to avoid CI/CD failures"
        exit 1
    fi
}

# Run main function
main "$@"