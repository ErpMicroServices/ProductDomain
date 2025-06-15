#!/bin/bash

# Script to configure branch protection rules for ProductDomain repository
# This script uses the GitHub CLI (gh) to set up branch protection

set -euo pipefail

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Repository details
OWNER="ErpMicroServices"
REPO="ProductDomain"

echo -e "${GREEN}Setting up branch protection rules for ${OWNER}/${REPO}${NC}"

# Function to setup branch protection
setup_branch_protection() {
    local branch=$1
    echo -e "\n${YELLOW}Configuring protection for branch: ${branch}${NC}"
    
    # Create the branch protection rule
    gh api \
        --method PUT \
        -H "Accept: application/vnd.github+json" \
        -H "X-GitHub-Api-Version: 2022-11-28" \
        "/repos/${OWNER}/${REPO}/branches/${branch}/protection" \
        -f "required_status_checks[strict]=true" \
        -f "required_status_checks[contexts][]=code-quality" \
        -f "required_status_checks[contexts][]=unit-tests" \
        -f "required_status_checks[contexts][]=integration-tests" \
        -f "required_status_checks[contexts][]=security-scan" \
        -f "required_status_checks[contexts][]=build-artifacts" \
        -f "enforce_admins=false" \
        -f "required_pull_request_reviews[dismiss_stale_reviews]=true" \
        -f "required_pull_request_reviews[require_code_owner_reviews]=true" \
        -f "required_pull_request_reviews[required_approving_review_count]=1" \
        -f "required_pull_request_reviews[require_last_push_approval]=false" \
        -f "required_pull_request_reviews[bypass_pull_request_allowances][users][]=" \
        -f "required_pull_request_reviews[bypass_pull_request_allowances][teams][]=" \
        -f "restrictions=null" \
        -f "required_linear_history=true" \
        -f "allow_force_pushes=false" \
        -f "allow_deletions=false" \
        -f "block_creations=false" \
        -f "required_conversation_resolution=true" \
        -f "lock_branch=false" \
        -f "allow_fork_syncing=true"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Branch protection configured for ${branch}${NC}"
    else
        echo -e "${RED}✗ Failed to configure protection for ${branch}${NC}"
        return 1
    fi
}

# Function to setup additional protection for production branch
setup_production_protection() {
    local branch="production"
    echo -e "\n${YELLOW}Configuring enhanced protection for branch: ${branch}${NC}"
    
    # Create the branch if it doesn't exist
    gh api \
        --method GET \
        "/repos/${OWNER}/${REPO}/branches/${branch}" \
        >/dev/null 2>&1 || {
        echo "Creating production branch..."
        gh api \
            --method POST \
            "/repos/${OWNER}/${REPO}/git/refs" \
            -f "ref=refs/heads/${branch}" \
            -f "sha=$(git rev-parse main)"
    }
    
    # Create stricter protection rule for production
    gh api \
        --method PUT \
        -H "Accept: application/vnd.github+json" \
        -H "X-GitHub-Api-Version: 2022-11-28" \
        "/repos/${OWNER}/${REPO}/branches/${branch}/protection" \
        -f "required_status_checks[strict]=true" \
        -f "required_status_checks[contexts][]=code-quality" \
        -f "required_status_checks[contexts][]=unit-tests" \
        -f "required_status_checks[contexts][]=integration-tests" \
        -f "required_status_checks[contexts][]=security-scan" \
        -f "required_status_checks[contexts][]=build-artifacts" \
        -f "required_status_checks[contexts][]=deploy-staging" \
        -f "enforce_admins=true" \
        -f "required_pull_request_reviews[dismiss_stale_reviews]=true" \
        -f "required_pull_request_reviews[require_code_owner_reviews]=true" \
        -f "required_pull_request_reviews[required_approving_review_count]=2" \
        -f "required_pull_request_reviews[require_last_push_approval]=true" \
        -f "restrictions=null" \
        -f "required_linear_history=true" \
        -f "allow_force_pushes=false" \
        -f "allow_deletions=false" \
        -f "block_creations=false" \
        -f "required_conversation_resolution=true" \
        -f "lock_branch=false" \
        -f "allow_fork_syncing=false"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Enhanced branch protection configured for ${branch}${NC}"
    else
        echo -e "${RED}✗ Failed to configure protection for ${branch}${NC}"
        return 1
    fi
}

# Function to create CODEOWNERS file
create_codeowners() {
    echo -e "\n${YELLOW}Creating CODEOWNERS file${NC}"
    
    mkdir -p .github
    cat > .github/CODEOWNERS << 'EOF'
# Code Owners for ProductDomain

# Default owners for everything in the repo
* @JimBarrows

# API module
/api/ @JimBarrows

# Database module
/database/ @JimBarrows

# UI Components
/ui-components/ @JimBarrows

# CI/CD and GitHub configuration
/.github/ @JimBarrows

# Build configuration
/build.gradle @JimBarrows
/settings.gradle @JimBarrows
*.gradle @JimBarrows

# Documentation
/docs/ @JimBarrows
*.md @JimBarrows

# Docker configuration
Dockerfile @JimBarrows
docker-compose*.yml @JimBarrows

# Security-sensitive files
/config/ @JimBarrows
*.properties @JimBarrows
*.yml @JimBarrows
*.yaml @JimBarrows
EOF

    echo -e "${GREEN}✓ CODEOWNERS file created${NC}"
}

# Function to create auto-assignment configuration
create_auto_assignment() {
    echo -e "\n${YELLOW}Creating auto-assignment configuration${NC}"
    
    mkdir -p .github
    cat > .github/auto-assign.yml << 'EOF'
# Auto-assign configuration for PRs

# Set to true to add reviewers to pull requests
addReviewers: true

# Set to true to add assignees to pull requests
addAssignees: true

# A list of reviewers to be added to pull requests
reviewers:
  - JimBarrows

# A number of reviewers added to the pull request
# Set 0 to add all the reviewers
numberOfReviewers: 1

# A list of assignees to be added to pull requests
assignees:
  - JimBarrows

# A number of assignees to add to the pull request
# Set to 0 to add all of the assignees
numberOfAssignees: 1

# A list of keywords to skip the auto assignment
skipKeywords:
  - wip
  - draft
EOF

    echo -e "${GREEN}✓ Auto-assignment configuration created${NC}"
}

# Function to setup repository settings
setup_repository_settings() {
    echo -e "\n${YELLOW}Configuring repository settings${NC}"
    
    # Update repository settings
    gh api \
        --method PATCH \
        -H "Accept: application/vnd.github+json" \
        -H "X-GitHub-Api-Version: 2022-11-28" \
        "/repos/${OWNER}/${REPO}" \
        -f "has_issues=true" \
        -f "has_projects=true" \
        -f "has_wiki=false" \
        -f "allow_squash_merge=true" \
        -f "allow_merge_commit=false" \
        -f "allow_rebase_merge=true" \
        -f "delete_branch_on_merge=true" \
        -f "allow_auto_merge=true" \
        -f "allow_update_branch=true" \
        -f "use_squash_pr_title_as_default=true" \
        -f "squash_merge_commit_title=PR_TITLE" \
        -f "squash_merge_commit_message=PR_BODY"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Repository settings updated${NC}"
    else
        echo -e "${RED}✗ Failed to update repository settings${NC}"
    fi
}

# Main execution
main() {
    echo -e "${GREEN}Starting branch protection setup...${NC}"
    
    # Check if gh CLI is installed
    if ! command -v gh &> /dev/null; then
        echo -e "${RED}GitHub CLI (gh) is not installed. Please install it first.${NC}"
        exit 1
    fi
    
    # Check if authenticated
    if ! gh auth status &> /dev/null; then
        echo -e "${RED}Not authenticated with GitHub. Please run 'gh auth login' first.${NC}"
        exit 1
    fi
    
    # Create CODEOWNERS file
    create_codeowners
    
    # Create auto-assignment configuration
    create_auto_assignment
    
    # Setup repository settings
    setup_repository_settings
    
    # Setup branch protection for main branch
    setup_branch_protection "main"
    
    # Setup branch protection for develop branch (if exists)
    gh api "/repos/${OWNER}/${REPO}/branches/develop" >/dev/null 2>&1 && \
        setup_branch_protection "develop"
    
    # Setup enhanced protection for production branch
    setup_production_protection
    
    echo -e "\n${GREEN}Branch protection setup completed!${NC}"
    echo -e "${YELLOW}Note: Some settings may require repository admin permissions.${NC}"
}

# Run main function
main "$@"