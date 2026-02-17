# State Machines

This document defines all state transitions in the application.

---

## Overview

| State Machine | States | Terminal States |
|---------------|--------|-----------------|
| User Status | VERIFICATION_PENDING, WAITLISTED, REVIEW_PENDING, ACTIVE, REJECTED, BANNED | None (admin can always change) |
| Ok Status | PENDING, ACCEPTED, DECLINED, EXPIRED, UNDONE | DECLINED, EXPIRED, UNDONE |
| Meeting Status | PENDING_PROPOSAL, PENDING_FINALIZATION, SCHEDULED, COMPLETED, CANCELLED | COMPLETED, CANCELLED |
| Approval Request Status | PENDING, APPROVED, REJECTED | APPROVED, REJECTED |
| Dispute Status | OPEN, INVESTIGATING, RESOLVED | RESOLVED |

---

## 1. User Status

```
┌─────────────────────┐
│ VERIFICATION_PENDING│ (initial state on signup)
└──────────┬──────────┘
           │ identity verified (3rd party callback)
           ▼
┌─────────────────────┐
│     WAITLISTED      │
└──────────┬──────────┘
           │ admin releases from waitlist
           ▼
┌─────────────────────┐
│   REVIEW_PENDING    │
└──────────┬──────────┘
           │
     ┌─────┴─────┐
     │           │
     ▼           ▼
┌─────────┐ ┌─────────┐
│ ACTIVE  │◄┤REJECTED │ (admin can reactivate)
└────┬────┘ └─────────┘
     │ ▲
     │ │ admin unbans
     │ │
     ▼ │
┌─────────┐
│ BANNED  │
└─────────┘
```

### Transitions

| From | To | Trigger |
|------|----|---------|
| VERIFICATION_PENDING | WAITLISTED | 3rd party identity verification callback |
| WAITLISTED | REVIEW_PENDING | Admin releases from waitlist |
| REVIEW_PENDING | ACTIVE | Admin approves |
| REVIEW_PENDING | REJECTED | Admin rejects |
| REJECTED | ACTIVE | Admin reactivates |
| ACTIVE | BANNED | Admin bans |
| BANNED | ACTIVE | Admin unbans |

### Rules

| Rule | Description |
|------|-------------|
| Silent rejection | REJECTED users see "pending" status, not told they're rejected |
| Admin override | Admin can move REJECTED → ACTIVE or BANNED → ACTIVE |
| No self-service recovery | REJECTED/BANNED users cannot reapply themselves |

---

## 2. Ok Status

```
┌─────────────────────┐
│       PENDING       │ (woman sends ok)
└──────────┬──────────┘
           │
     ┌─────┼─────────────────┐
     │     │                 │
     │     │                 │ 7 days pass without response
     │     │                 ▼
     │     │          ┌─────────┐
     │     │          │ EXPIRED │
     │     │          └─────────┘
     │     │
     │     ├─────────────────┐
     │     │                 │
     │     │ man declines    │ man accepts
     │     ▼                 ▼
     │ ┌─────────┐    ┌──────────┐
     │ │DECLINED │    │ ACCEPTED │ (creates Meeting)
     │ └─────────┘    └────┬─────┘
     │                     │
     │                     │ either party undoes
     │                     │ (before meeting SCHEDULED)
     │                     ▼
     │               ┌─────────┐
     └──────────────►│  UNDONE │
                     └─────────┘
      either party undoes
      (while PENDING)
```

### Transitions

| From | To | Trigger | Notes |
|------|----|---------|-------|
| PENDING | ACCEPTED | Man accepts | Creates Meeting in PENDING_PROPOSAL status |
| PENDING | DECLINED | Man declines | Terminal |
| PENDING | EXPIRED | 7 days pass | Scheduled job. Man's profile marked `has_let_ok_expire` |
| PENDING | UNDONE | Either party undoes | `undone_by` tracks who. Undoer's profile marked `has_undone_ok` |
| ACCEPTED | UNDONE | Either party undoes | Only before Meeting reaches SCHEDULED status |

### Rules

| Rule | Description |
|------|-------------|
| One Ok per pair ever | Woman can only send one Ok to a specific man. Any terminal state (DECLINED, UNDONE, EXPIRED) blocks retry. |
| Max 3 active Oks | Woman can have at most 3 Oks in PENDING or ACCEPTED status (with non-completed meeting) |
| 7-day expiration | PENDING Oks expire automatically after 7 days |
| Undo penalty | Undoing marks profile with `has_undone_ok` flag (visible to others) |
| Expire penalty | Letting Ok expire marks man's profile with `has_let_ok_expire` flag |

---

## 3. Meeting Status

```
┌─────────────────────┐
│  PENDING_PROPOSAL   │ (created when Ok accepted, or after reschedule)
└──────────┬──────────┘
           │
     ┌─────┴─────────────────┐
     │                       │
     │ woman submits         │ 2 days pass without proposal
     │ 4 proposals           │
     ▼                       ▼
┌────────────────────┐  ┌───────────┐
│PENDING_FINALIZATION│  │ CANCELLED │ reason: PROPOSAL_DEADLINE_MISSED
└──────────┬─────────┘  └───────────┘
           │
     ┌─────┴─────────────────┐
     │                       │
     │ man selects           │ 2 days pass without selection
     │ date & location       │
     ▼                       ▼
┌───────────┐           ┌───────────┐
│ SCHEDULED │           │ CANCELLED │ reason: FINALIZATION_DEADLINE_MISSED
└─────┬─────┘           └───────────┘
      │
      ├─── meeting time passes ──────────► COMPLETED
      │
      ├─── user explicitly cancels ──────► CANCELLED (USER_CANCELLED)
      │
      └─── either party requests ────────► PENDING_PROPOSAL (is_rescheduled = true)
           reschedule (if !is_rescheduled)  MeetingEvent: RESCHEDULE_REQUESTED
                                            (2-day proposal deadline restarts)
```

### Transitions

| From | To | Trigger |
|------|----|---------|
| PENDING_PROPOSAL | PENDING_FINALIZATION | Woman submits 4 proposals |
| PENDING_PROPOSAL | CANCELLED | 2 days pass (PROPOSAL_DEADLINE_MISSED) |
| PENDING_FINALIZATION | SCHEDULED | Man selects date & location |
| PENDING_FINALIZATION | CANCELLED | 2 days pass (FINALIZATION_DEADLINE_MISSED) |
| SCHEDULED | COMPLETED | Meeting time passes |
| SCHEDULED | CANCELLED | User cancels (USER_CANCELLED) |
| SCHEDULED | PENDING_PROPOSAL | Either party requests reschedule (if `is_rescheduled = false`) |

### Cancellation Reasons

| Reason | Trigger |
|--------|---------|
| `PROPOSAL_DEADLINE_MISSED` | Woman didn't propose within 2 days |
| `FINALIZATION_DEADLINE_MISSED` | Man didn't finalize within 2 days |
| `USER_CANCELLED` | User explicitly cancelled |
| `UNDO_BY_WOMAN` | Woman undid Ok (before SCHEDULED) |
| `UNDO_BY_MAN` | Man undid Ok (before SCHEDULED) |

### Reschedule Rules

| Rule | Description |
|------|-------------|
| Either party can request | Man or woman can initiate reschedule |
| Immediate effect | Request instantly voids current schedule, goes to PENDING_PROPOSAL |
| Max 1 reschedule | `is_rescheduled` boolean flag, can only reschedule once |
| Same deadlines | 2 days for proposal, 2 days for finalization |
| 3-week deadline enforced | Proposed dates must be within 3 weeks of original mutual ok — invalid requests rejected |
| Woman always proposes | Regardless of who requested reschedule |

---

## 4. Approval Request Status

```
┌─────────────────────┐
│       PENDING       │ (user submits request)
└──────────┬──────────┘
           │
     ┌─────┴─────┐
     │           │
     │ admin     │ admin
     │ approves  │ rejects
     ▼           ▼
┌──────────┐ ┌──────────┐
│ APPROVED │ │ REJECTED │
└──────────┘ └──────────┘

(both are terminal states)
```

### Transitions

| From | To | Trigger |
|------|----|---------|
| PENDING | APPROVED | Admin approves |
| PENDING | REJECTED | Admin rejects |

### Side Effects by Request Type

| Request Type | On APPROVED | On REJECTED |
|--------------|-------------|-------------|
| `SIGN_UP` | Create Profile with payload data, User → ACTIVE | User → REJECTED (cannot submit new request) |
| `PHOTO_ADD` | UserPhoto.is_active → true | UserPhoto kept with is_active = false (audit trail) |
| `BIO_CHANGE` | Profile.bio updated with new value | No change (old bio remains) |

### Rules

| Rule | Description |
|------|-------------|
| Terminal states | APPROVED and REJECTED are final, admin cannot change |
| No resubmission | Each request is independent, rejected request cannot be resubmitted |
| REJECTED user blocked | User with status REJECTED cannot submit new SIGN_UP request |

---

## 5. Dispute Status

```
┌─────────────────────┐
│        OPEN         │ (auto-created when feedback has no_show_reason = THEY_DID_NOT_SHOW)
└──────────┬──────────┘
           │
           │ admin starts investigation
           ▼
┌─────────────────────┐
│    INVESTIGATING    │
└──────────┬──────────┘
           │
           │ admin resolves
           ▼
┌─────────────────────┐
│      RESOLVED       │ (terminal)
└─────────────────────┘
```

### Transitions

| From | To | Trigger |
|------|----|--------|
| OPEN | INVESTIGATING | Admin starts investigation |
| INVESTIGATING | RESOLVED | Admin resolves with resolution type |

### Resolution Types

| Resolution | Description |
|------------|-------------|
| `NO_ACTION` | No wrongdoing found |
| `WARNING_ISSUED` | Warning issued to reported user (creates UserWarning) |
| `USER_BANNED` | Reported user banned |
| `INSUFFICIENT_EVIDENCE` | Cannot determine what happened |

### Rules

| Rule | Description |
|------|-------------|
| Must investigate first | Cannot skip OPEN → RESOLVED directly |
| Terminal state | RESOLVED cannot be reopened |
| Auto-created | Created automatically when feedback reports `THEY_DID_NOT_SHOW` |
