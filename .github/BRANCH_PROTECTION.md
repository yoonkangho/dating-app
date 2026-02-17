# Branch Protection Settings for Trunk-Based Development

To fully enforce trunk-based development, configure these branch protection rules in GitHub:

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
    - `Enforce Trunk-Based Development`
    - `Build and Test`

- ✅ **Require linear history** ← This enforces fast-forward merges only!

### Additional Recommended Settings
- ✅ **Do not allow bypassing the above settings**
- ✅ **Restrict who can push to matching branches** (optional)
- ❌ Allow force pushes
- ❌ Allow deletions

## Merge Button Settings

Go to: **Settings → General → Pull Requests**

- ❌ Allow merge commits
- ❌ Allow squash merging (optional - some teams prefer this)
- ✅ **Allow rebase merging** ← This is the fast-forward merge!
- ✅ Always suggest updating pull request branches
- ✅ Automatically delete head branches

## Why These Settings?

| Setting | Trunk-Based Benefit |
|---------|-------------------|
| Require linear history | Enforces fast-forward merges, no merge commits |
| Require up-to-date branches | Forces rebase before merge |
| Allow rebase merging only | Clean, linear git history |
| Auto-delete branches | Keeps repo clean, encourages short-lived branches |
