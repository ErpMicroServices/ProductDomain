#!/bin/sh
#
# Install Git hooks for ProductDomain project
#

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo "Installing Git hooks for ProductDomain project..."

# Get the project root directory
PROJECT_ROOT=$(cd "$(dirname "$0")/../.." && pwd)
HOOKS_DIR="$PROJECT_ROOT/gradle/git-hooks"
GIT_HOOKS_DIR="$PROJECT_ROOT/.git/hooks"

# Check if .git directory exists
if [ ! -d "$PROJECT_ROOT/.git" ]; then
    echo "${RED}Error: Not a git repository${NC}"
    exit 1
fi

# Create hooks directory if it doesn't exist
mkdir -p "$GIT_HOOKS_DIR"

# Function to install a hook
install_hook() {
    HOOK_NAME=$1
    SOURCE_FILE="$HOOKS_DIR/$HOOK_NAME"
    TARGET_FILE="$GIT_HOOKS_DIR/$HOOK_NAME"
    
    if [ -f "$SOURCE_FILE" ]; then
        # Backup existing hook if it exists and is not a symlink
        if [ -f "$TARGET_FILE" ] && [ ! -L "$TARGET_FILE" ]; then
            echo "${YELLOW}Backing up existing $HOOK_NAME hook${NC}"
            mv "$TARGET_FILE" "$TARGET_FILE.backup.$(date +%Y%m%d%H%M%S)"
        fi
        
        # Create symlink
        ln -sf "$SOURCE_FILE" "$TARGET_FILE"
        chmod +x "$TARGET_FILE"
        echo "${GREEN}✓ Installed $HOOK_NAME hook${NC}"
    else
        echo "${YELLOW}⚠ $HOOK_NAME hook not found in $HOOKS_DIR${NC}"
    fi
}

# Install available hooks
install_hook "pre-commit"

# Create uninstall script
cat > "$HOOKS_DIR/uninstall-hooks.sh" << 'EOF'
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
EOF

chmod +x "$HOOKS_DIR/uninstall-hooks.sh"

echo ""
echo "${GREEN}Git hooks installation complete!${NC}"
echo ""
echo "The following hooks have been installed:"
echo "  - pre-commit: Runs quality checks before each commit"
echo ""
echo "To skip hooks temporarily, use: git commit --no-verify"
echo "To uninstall hooks, run: $HOOKS_DIR/uninstall-hooks.sh"
echo ""