# Pre-commit Hooks

The ProductDomain project uses pre-commit hooks to ensure code quality and prevent common issues before they reach the repository.

## Enhanced Pre-commit Hook Features

### GitHub Actions Workflow Validation (NEW)
- **Deprecated Action Detection**: Automatically detects and reports deprecated GitHub Actions versions
- **YAML Syntax Validation**: Ensures workflow files have valid YAML syntax
- **Version Format Checking**: Validates that actions use proper semantic versioning
- **Automated Fix Suggestions**: Provides commands to update deprecated actions

### Code Quality Checks
- **Java**: Checkstyle, PMD, SpotBugs, compilation checks
- **TypeScript/React**: ESLint, TypeScript compilation, Prettier formatting
- **Test Execution**: Runs relevant tests for modified code

### General Validations
- **Debugging Statements**: Detects console.log, System.out.print, TODO, FIXME
- **Large Files**: Warns about files larger than 1MB

## Quick Start

Install the enhanced pre-commit hook:
```bash
./scripts/install-pre-commit.sh
```

## Example: Fixing Deprecated Actions

When the hook detects deprecated actions:
```
✗ Found deprecated action: actions/checkout@v3
  → Update to: actions/checkout@v4
  Fix command: sed -i '' 's|actions/checkout@v3|actions/checkout@v4|g' .github/workflows/ci.yml
```

Apply all fixes at once:
```bash
find .github/workflows -name '*.yml' -o -name '*.yaml' | while read f; do
  sed -i '' -e 's|actions/checkout@v3|actions/checkout@v4|g' "$f"
  # ... other replacements
done
```

## Bypass Hook (Not Recommended)

If absolutely necessary:
```bash
git commit --no-verify
```

For detailed documentation, see [docs/pre-commit-workflow-validation.md](pre-commit-workflow-validation.md)