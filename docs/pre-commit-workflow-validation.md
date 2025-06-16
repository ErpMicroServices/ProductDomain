# Pre-commit GitHub Actions Workflow Validation

## Overview

The ProductDomain project includes an enhanced pre-commit hook that validates GitHub Actions workflow files before allowing commits. This ensures that workflow files are syntactically correct and use the latest action versions, preventing CI/CD pipeline failures due to deprecated actions.

## Features

### 1. GitHub Actions Version Checking
- Detects deprecated action versions (e.g., `actions/checkout@v3`)
- Suggests the latest versions for all known actions
- Covers both official GitHub actions and popular third-party actions

### 2. YAML Syntax Validation
- Validates YAML syntax in workflow files
- Reports line numbers for syntax errors
- Ensures workflow files can be parsed correctly

### 3. Required Properties Validation
- Checks for required workflow properties (`name`, `on`, `jobs`)
- Helps maintain consistent workflow structure

### 4. Docker Action Validation
- Warns about using `:latest` tags in Docker actions
- Encourages specific version tags for reproducibility

### 5. Version Format Validation
- Ensures actions use proper semantic versioning (e.g., `@v4`)
- Detects branch references and suggests version tags
- Accepts commit SHAs with a warning

## Installation

To install the enhanced pre-commit hook:

```bash
./scripts/install-pre-commit.sh
```

This will:
1. Back up any existing pre-commit hook
2. Install the enhanced hook with workflow validation
3. Make the hook executable

## Usage

The pre-commit hook runs automatically when you attempt to commit changes. If any GitHub Actions workflow files are staged, they will be validated.

### Example Output

When deprecated actions are detected:
```
Running pre-commit quality checks...
Running GitHub Actions workflow validation...
Using bash validation (Java validator not built)
Checking .github/workflows/ci.yml...
✗ Found deprecated action: actions/checkout@v3
  → Update to: actions/checkout@v4
  Fix command: sed -i '' 's|actions/checkout@v3|actions/checkout@v4|g' .github/workflows/ci.yml

✗ Pre-commit checks failed!
Fix the issues above and try again.
To bypass these checks (not recommended), use: git commit --no-verify
```

### Batch Fixing Deprecated Actions

The hook provides a batch fix command when issues are found:

```bash
find .github/workflows -name '*.yml' -o -name '*.yaml' | while read f; do
  sed -i '' -e 's|actions/checkout@v3|actions/checkout@v4|g' \
            -e 's|actions/setup-java@v3|actions/setup-java@v4|g' \
            -e 's|actions/upload-artifact@v3|actions/upload-artifact@v4|g' "$f"
done
```

## Known Actions Database

The hook maintains a database of known actions and their latest versions:

### GitHub Official Actions (v4)
- `actions/checkout`
- `actions/setup-java`
- `actions/setup-node`
- `actions/setup-python`
- `actions/upload-artifact`
- `actions/download-artifact`
- `actions/cache`

### GitHub Official Actions (Other Versions)
- `actions/github-script@v7`
- `actions/setup-go@v5`

### Popular Third-Party Actions
- `docker/setup-buildx-action@v3`
- `docker/build-push-action@v5`
- `docker/login-action@v3`
- `docker/setup-qemu-action@v3`
- `docker/metadata-action@v5`
- `gradle/gradle-build-action@v2`
- `dorny/test-reporter@v1`
- `softprops/action-gh-release@v1`

## Advanced Features

### Java-based Validator

For better performance and more detailed validation, you can build and use the Java-based workflow validator:

```bash
# Build the validator
./gradlew jar

# The pre-commit hook will automatically use it when available
```

The Java validator provides:
- Faster validation for large workflows
- More detailed error messages
- Line-specific issue reporting
- Caching of version lookups

### Bypassing the Hook

If you need to commit without validation (not recommended):

```bash
git commit --no-verify
```

## Customization

### Adding New Deprecated Actions

To add new deprecated actions to the validation, edit the pre-commit hook and add entries to the `check_deprecated_action` function:

```bash
case "$action" in
    "your/action@old") replacement="your/action@new";;
    # ... other actions
esac
```

### Updating the Java Validator

To add new actions to the Java validator, edit `GitHubActionsVersionChecker.java` and update the `initializeKnownActions()` method:

```java
actions.put("your/action", "v2");
```

## Troubleshooting

### Hook Not Running

If the hook doesn't run:
1. Ensure it's executable: `chmod +x .git/hooks/pre-commit`
2. Check if workflows are staged: `git diff --cached --name-only`
3. Verify the hook is installed: `ls -la .git/hooks/pre-commit`

### False Positives

If the hook reports issues incorrectly:
1. Check if the action version is truly deprecated
2. Verify the YAML syntax is correct
3. Ensure the action reference format is correct (`owner/repo@version`)

### Performance Issues

If validation is slow:
1. Build the Java validator for better performance
2. Consider validating only changed files
3. Use the `--no-verify` flag for urgent commits

## Contributing

To contribute to the workflow validation:
1. Add new deprecated actions to the database
2. Improve error messages and suggestions
3. Add support for new workflow features
4. Submit pull requests with test coverage