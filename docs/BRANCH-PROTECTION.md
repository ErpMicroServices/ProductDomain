# Branch Protection Rules

This document describes the branch protection rules configured for the ProductDomain repository.

## Protected Branches

### Main Branch (`main`)

The main branch is the primary development branch and has the following protections:

#### Required Status Checks
- **Strict mode enabled**: The branch must be up to date before merging
- Required checks that must pass:
  - `code-quality`: Code quality checks (Checkstyle, PMD, SpotBugs)
  - `unit-tests`: All unit tests must pass
  - `integration-tests`: All integration tests must pass
  - `security-scan`: Security vulnerability scan must pass
  - `build-artifacts`: Build must complete successfully

#### Pull Request Requirements
- **Reviews required**: At least 1 approving review
- **Dismiss stale reviews**: Reviews are dismissed when new commits are pushed
- **Code owner reviews**: Required when files have CODEOWNERS
- **Conversation resolution**: All PR conversations must be resolved

#### Merge Restrictions
- **Linear history required**: No merge commits allowed
- **Force pushes blocked**: Cannot force push to main
- **Branch deletion blocked**: Cannot delete the main branch
- **Direct pushes blocked**: All changes must go through a PR

### Develop Branch (`develop`)

The develop branch serves as an integration branch and has similar protections to main:

- Same required status checks as main
- Same pull request requirements
- Same merge restrictions

### Production Branch (`production`)

The production branch represents the current production state and has enhanced protections:

#### Additional Requirements
- **2 approving reviews required** (instead of 1)
- **Require approval from someone other than the last pusher**
- **Additional status check**: `deploy-staging` must pass
- **Enforce admin restrictions**: Even admins must follow the rules
- **No fork syncing allowed**

## Repository Settings

### Merge Options
- ✅ **Squash and merge**: Enabled (default)
- ❌ **Create a merge commit**: Disabled
- ✅ **Rebase and merge**: Enabled

### Additional Settings
- **Delete branches after merge**: Enabled
- **Auto-merge**: Enabled for approved PRs
- **Update branch button**: Enabled
- **Squash merge defaults**: Uses PR title and body

## Code Ownership

The repository uses a CODEOWNERS file to automatically assign reviewers:

```
# Default owner
* @JimBarrows

# Module-specific owners
/api/ @JimBarrows
/database/ @JimBarrows
/ui-components/ @JimBarrows
/.github/ @JimBarrows
```

## Auto-Assignment

Pull requests are automatically assigned to:
- **Reviewers**: JimBarrows
- **Assignees**: JimBarrows

Skip auto-assignment by including `wip` or `draft` in the PR title.

## Setting Up Branch Protection

To configure branch protection rules:

1. Ensure you have the GitHub CLI installed:
   ```bash
   brew install gh  # macOS
   # or see https://cli.github.com for other platforms
   ```

2. Authenticate with GitHub:
   ```bash
   gh auth login
   ```

3. Run the setup script:
   ```bash
   ./.github/scripts/setup-branch-protection.sh
   ```

## Bypassing Protection (Emergency Only)

In emergency situations, repository admins can:

1. Temporarily disable branch protection
2. Make the necessary fix
3. Re-enable protection immediately

This should only be done for critical production fixes that cannot wait for the normal review process.

## Best Practices

1. **Keep PRs small**: Easier to review and less likely to have conflicts
2. **Write descriptive PR titles**: They become the commit message when squash merging
3. **Use draft PRs**: For work in progress that isn't ready for review
4. **Keep your branch up to date**: Use the "Update branch" button before merging
5. **Resolve conversations**: Address all feedback before merging
6. **Use conventional commits**: For consistent commit messages

## Workflow

1. Create a feature branch from `main` or `develop`
2. Make your changes and push to GitHub
3. Open a pull request
4. Wait for automated checks to pass
5. Request review from code owners
6. Address any feedback
7. Merge when approved and all checks pass

## Troubleshooting

### "Branch is out of date"
- Click the "Update branch" button on the PR
- Or manually rebase: `git rebase origin/main`

### "Required status checks haven't passed"
- Check the failing checks in the PR
- Fix any issues and push new commits
- Checks will re-run automatically

### "Waiting for code owner review"
- Check the CODEOWNERS file to see who needs to review
- Request review from the appropriate person

### "Conversations must be resolved"
- Address all review comments
- Mark conversations as resolved when done