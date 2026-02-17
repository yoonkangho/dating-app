# Claude Code Task Workflow

This guide explains how to use GitHub Issues to manage parallel Claude Code sessions.

## Quick Start

### 1. Create a Task
- Go to **Issues → New Issue → Task**
- Keep tasks small (< 250 lines ideally)
- Add clear acceptance criteria

### 2. Assign to Claude Code Session
```bash
# Create a worktree for the task
git worktree add ../DatingApp-issue-42 -b feature/issue-42

# Navigate and start Claude Code
cd ../DatingApp-issue-42
claude

# In Claude Code, reference the issue:
> Implement issue #42: [paste description and acceptance criteria]
```

### 3. Track Progress
- Add label `in-progress` when starting
- Claude Code commits reference the issue: `feat: add validation (refs #42)`
- PR description includes `Closes #42`

### 4. Complete
- PR merged → Issue auto-closes
- Worktree cleaned up: `git worktree remove ../DatingApp-issue-42`

---

## Recommended Labels

| Label | Color | Purpose |
|-------|-------|---------|
| `task` | `#0E8A16` | Feature implementation |
| `bug` | `#D73A4A` | Bug fix |
| `in-progress` | `#FBCA04` | Currently being worked on |
| `blocked` | `#B60205` | Waiting on something |
| `size/XS` | `#3CBF00` | < 50 lines |
| `size/S` | `#5D9801` | 50-100 lines |
| `size/M` | `#FBCA04` | 100-250 lines |
| `size/L` | `#E99695` | 250-500 lines (consider splitting) |

---

## Parallel Sessions Example

```
Session 1 (Terminal 1)          Session 2 (Terminal 2)
─────────────────────          ─────────────────────
cd ../DatingApp-issue-42        cd ../DatingApp-issue-43
claude                          claude
> Work on user registration     > Work on email service

        ↓                               ↓
   PR #101                         PR #102
   Closes #42                      Closes #43
        ↓                               ↓
        └──────── Both merge to main ────────┘
```

---

## Tips for Good Tasks

✅ **Good task:**
> Implement POST /api/users/register endpoint
> - Accepts email, password
> - Validates email format
> - Hashes password
> - Returns 201 with user ID

❌ **Too big:**
> Implement entire user management system

❌ **Too vague:**
> Fix user stuff

---

## Using @claude in Issues

You can ask Claude to help plan or implement directly in issue comments:

```
@claude Can you break this task into smaller subtasks?

@claude What files would need to change for this?

@claude Please implement this task and create a PR
```

---

## Project Board Setup

Create a Project with these columns:

| Backlog | To Do | In Progress | In Review | Done |
|---------|-------|-------------|-----------|------|
| Future tasks | Ready to start | Being worked on | PR open | Merged |

Automate with built-in workflows:
- New issues → Backlog
- PR opened → In Review  
- PR merged → Done
