# Branch Protection Settings (GitHub Flow)

Configure these branch protection rules to enforce the GitHub Flow workflow.

## Settings for `main` branch

Go to: **Settings → Branches → Add branch protection rule**

### Basic Settings
- **Branch name pattern:** `main`
- ✅ **Require a pull request before merging**
  - ✅ Require approvals: 1 (or more for your team)
  - ❌ Dismiss stale pull request approvals when new commits are pushed
  - ❌ Require review from Code Owners

### Merge Settings
- ✅ **Require status checks to pass before merging**
  - ✅ Require branches to be up to date before merging
  - Required status checks:
    - `Build and Test`

### Additional Recommended Settings
- ✅ **Do not allow bypassing the above settings**
- ✅ **Restrict who can push to matching branches** (optional)
- ❌ Allow force pushes
- ❌ Allow deletions

## Merge Button Settings

Go to: **Settings → General → Pull Requests**

- ❌ Allow merge commits
- ✅ **Allow squash merging** ← Cleans up branch commits into a single commit
- ✅ **Allow rebase merging** ← Preserves individual commits with linear history
- ✅ Always suggest updating pull request branches
- ✅ Automatically delete head branches

## Why These Settings?

| Setting | Benefit |
|---------|---------|
| Require PR before merging | All changes are reviewed before landing on main |
| Require status checks | CI must pass before merge |
| Squash or rebase merging | Clean, readable git history on main |
| Auto-delete branches | Keeps repo clean after PRs merge |
