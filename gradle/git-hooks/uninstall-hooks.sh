#!/bin/sh
#
# Uninstall Git hooks for ProductDomain project
#

# Colors for output
GREEN='\033[0;32m'
NC='\033[0m' # No Color

echo "Uninstalling Git hooks..."

PROJECT_ROOT=$(cd "$(dirname "$0")/../.." && pwd)
GIT_HOOKS_DIR="$PROJECT_ROOT/.git/hooks"

# Remove symlinks
for hook in pre-commit; do
    if [ -L "$GIT_HOOKS_DIR/$hook" ]; then
        rm "$GIT_HOOKS_DIR/$hook"
        echo "${GREEN}✓ Removed $hook hook${NC}"
    fi
done

echo "${GREEN}Git hooks uninstalled${NC}"
