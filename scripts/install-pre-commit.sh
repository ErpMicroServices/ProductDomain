#!/bin/bash
#
# Install the enhanced pre-commit hook with GitHub Actions validation
#

PROJECT_ROOT=$(git rev-parse --show-toplevel)
HOOK_SOURCE="$PROJECT_ROOT/scripts/pre-commit-with-workflow-validation"
HOOK_DEST="$PROJECT_ROOT/.git/hooks/pre-commit"

echo "Installing enhanced pre-commit hook..."

# Backup existing hook if it exists
if [ -f "$HOOK_DEST" ]; then
    echo "Backing up existing pre-commit hook to pre-commit.backup"
    cp "$HOOK_DEST" "$HOOK_DEST.backup"
fi

# Install new hook
cp "$HOOK_SOURCE" "$HOOK_DEST"
chmod +x "$HOOK_DEST"

echo "✓ Pre-commit hook installed successfully!"
echo ""
echo "The hook will now:"
echo "  - Validate GitHub Actions workflow files"
echo "  - Check for deprecated action versions"
echo "  - Validate YAML syntax"
echo "  - Run existing code quality checks"
echo ""
echo "To build the Java workflow validator for better performance:"
echo "  ./gradlew :jar"
echo ""
echo "To bypass the hook (not recommended):"
echo "  git commit --no-verify"