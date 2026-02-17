# Entity Relationship Diagram: Guaranteed Meeting Dating App

**Version:** 1.1  
**Last Updated:** 2026-02-15  
**Author:** Kenneth

> **Note:** For detailed state transitions and business rules, see [STATE-MACHINES.md](./STATE-MACHINES.md)

---

## Overview

| Domain | Tables |
|--------|--------|
| User | `users`, `profiles`, `user_photos`, `approval_requests` |
| Matching | `oks`, `meetings`, `meeting_proposals`, `meeting_events` |
| Feedback | `feedbacks`, `ratings`, `disputes` |
| Admin | `admin_users`, `admin_actions`, `user_warnings` |

**Total: 13 tables**

---

## Complete Schema

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                      USER DOMAIN                                        │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌──────────────────────┐       1:1        ┌──────────────────────┐                    │
│  │        users         │─────────────────▶│      profiles        │                    │
│  ├──────────────────────┤                  ├──────────────────────┤                    │
│  │ id (PK)              │                  │ id (PK)              │                    │
│  │ email (unique)       │                  │ user_id (FK, unique) │                    │
│  │ phone                │                  │                      │                    │
│  │ password_hash        │                  │ -- Immutable --      │                    │
│  │ status               │                  │ display_name         │                    │
│  │ created_at           │                  │ birth_date           │                    │
│  │ updated_at           │                  │ gender               │                    │
│  └──────────────────────┘                  │ height_cm            │                    │
│            │                               │ profession           │                    │
│            │                               │ education            │                    │
│            │                               │ income_bracket       │                    │
│            │                               │ asset_bracket        │                    │
│            │                               │                      │                    │
│            │                               │ -- Mutable --        │                    │
│            │                               │ bio                  │                    │
│            │                               │                      │                    │
│            │                               │ -- Reputation --     │                    │
│            │                               │ has_undone_ok        │                    │
│            │                               │ has_let_ok_expire    │                    │
│            │                               │                      │                    │
│            │                               │ created_at           │                    │
│            │                               │ updated_at           │                    │
│            │                               └──────────────────────┘                    │
│            │                                          │                                │
│            │                                          │ 1:N                            │
│            │                                          ▼                                │
│            │                               ┌──────────────────────┐                    │
│            │                               │     user_photos      │                    │
│            │                               ├──────────────────────┤                    │
│            │                               │ id (PK)              │                    │
│            │                               │ profile_id (FK)      │                    │
│            │                               │ url                  │                    │
│            │                               │ display_order        │                    │
│            │                               │ is_primary           │                    │
│            │                               │ is_active            │                    │
│            │                               │ created_at           │                    │
│            │                               │ updated_at           │                    │
│            │                               └──────────────────────┘                    │
│            │                                                                           │
│            │ 1:N                                                                       │
│            ▼                                                                           │
│  ┌──────────────────────────────────────────────────────────────────┐                 │
│  │                      approval_requests                           │                 │
│  ├──────────────────────────────────────────────────────────────────┤                 │
│  │ id (PK)                                                          │                 │
│  │ user_id (FK)               -- who submitted the request          │                 │
│  │ request_type               -- SIGN_UP, PHOTO_ADD, BIO_CHANGE     │                 │
│  │ status                     -- PENDING, APPROVED, REJECTED        │                 │
│  │ target_type                -- PROFILE, PHOTO (nullable)          │                 │
│  │ target_id                  -- profile_id or photo_id (nullable)  │                 │
│  │ payload (JSONB)            -- submitted data                     │                 │
│  │ reviewed_by (FK)           -- AdminUser (nullable)               │                 │
│  │ reviewed_at                -- (nullable)                         │                 │
│  │ review_notes               -- (nullable)                         │                 │
│  │ created_at                                                       │                 │
│  │ updated_at                                                       │                 │
│  └──────────────────────────────────────────────────────────────────┘                 │
│                                                                                        │
│  ApprovalRequest Payload Examples:                                                     │
│                                                                                        │
│  SIGN_UP:                                                                              │
│  {                                                                                     │
│    "display_name": "Jane",                                                             │
│    "birth_date": "1995-03-15",                                                         │
│    "gender": "FEMALE",                                                                 │
│    "height_cm": 165,                                                                   │
│    "profession": "Engineer",                                                           │
│    "education": "BACHELORS",                                                           │
│    "income_bracket": "100K_150K",                                                      │
│    "asset_bracket": "100K_500K",                                                       │
│    "bio": "Hello world"                                                                │
│  }                                                                                     │
│                                                                                        │
│  PHOTO_ADD:                                                                            │
│  {                                                                                     │
│    "url": "https://...",                                                               │
│    "display_order": 2,                                                                 │
│    "is_primary": false                                                                 │
│  }                                                                                     │
│                                                                                        │
│  BIO_CHANGE:                                                                           │
│  {                                                                                     │
│    "old_value": "Hello world",                                                         │
│    "new_value": "Updated bio text"                                                     │
│  }                                                                                     │
│                                                                                        │
└────────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                    MATCHING DOMAIN                                      │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌──────────────────────┐                                                              │
│  │         oks          │                                                              │
│  ├──────────────────────┤                                                              │
│  │ id (PK)              │                                                              │
│  │ sender_id (FK)       │───────▶ users (woman who initiated)                          │
│  │ recipient_id (FK)    │───────▶ users (man who receives)                             │
│  │ status               │        [PENDING, ACCEPTED, DECLINED, EXPIRED, UNDONE]        │
│  │ sent_at              │                                                              │
│  │ expires_at           │        (sent_at + 7 days)                                    │
│  │ responded_at         │        (nullable)                                            │
│  │ undone_by (FK)       │───────▶ users (nullable)                                     │
│  │ undone_at            │        (nullable)                                            │
│  │ created_at           │                                                              │
│  │ updated_at           │                                                              │
│  └──────────────────────┘                                                              │
│            │                                                                            │
│            │ 1:1                                                                        │
│            ▼                                                                            │
│  ┌──────────────────────┐                                                              │
│  │       meetings       │                                                              │
│  ├──────────────────────┤                                                              │
│  │ id (PK)              │                                                              │
│  │ ok_id (FK, unique)   │                                                              │
│  │ woman_id (FK)        │───────▶ users                                                │
│  │ man_id (FK)          │───────▶ users                                                │
│  │ status               │        [PENDING_PROPOSAL, PENDING_FINALIZATION,              │
│  │                      │         SCHEDULED, COMPLETED, CANCELLED]                     │
│  │ proposal_deadline    │        (created_at + 2 days)                                 │
│  │ finalization_deadline│        (nullable)                                            │
│  │ meeting_deadline     │        (created_at + 3 weeks)                                │
│  │ scheduled_at         │        (nullable)                                            │
│  │ location_name        │        (nullable)                                            │
│  │ location_address     │        (nullable)                                            │
│  │ is_rescheduled       │        (boolean, default false)                              │
│  │ cancelled_by (FK)    │───────▶ users (nullable)                                     │
│  │ cancelled_at         │        (nullable)                                            │
│  │ cancellation_reason  │        (nullable)                                            │
│  │ created_at           │                                                              │
│  │ updated_at           │                                                              │
│  └──────────────────────┘                                                              │
│            │                                                                            │
│            │ 1:N                                                                        │
│            ├───────────────────────────────────────┐                                   │
│            ▼                                       ▼                                   │
│  ┌──────────────────────┐              ┌──────────────────────┐                        │
│  │  meeting_proposals   │              │   meeting_events     │                        │
│  ├──────────────────────┤              ├──────────────────────┤                        │
│  │ id (PK)              │              │ id (PK)              │                        │
│  │ meeting_id (FK)      │              │ meeting_id (FK)      │                        │
│  │ proposed_date        │              │ event_type           │                        │
│  │ proposed_location    │              │ actor_id (FK)        │───────▶ users          │
│  │ is_selected          │              │ details (JSONB)      │                        │
│  │ created_at           │              │ created_at           │                        │
│  └──────────────────────┘              └──────────────────────┘                        │
│                                                                                         │
│  MeetingEvent Examples:                                                                │
│                                                                                         │
│  PROPOSAL_SUBMITTED:                                                                   │
│  { "proposal_count": 4 }                                                               │
│                                                                                         │
│  MEETING_FINALIZED:                                                                    │
│  {                                                                                     │
│    "selected_proposal_id": "<uuid>",                                                   │
│    "scheduled_at": "2025-02-01T18:00:00Z",                                             │
│    "location_name": "Coffee Shop",                                                     │
│    "location_address": "123 Main St"                                                   │
│  }                                                                                     │
│                                                                                         │
│  RESCHEDULE_REQUESTED:                                                                 │
│  {                                                                                     │
│    "original_scheduled_at": "2025-02-01T18:00:00Z",                                    │
│    "requester": "woman" | "man"                                                        │
│  }                                                                                     │
│                                                                                         │
│  MEETING_CANCELLED:                                                                    │
│  { "reason": "USER_CANCELLED", "cancelled_by": "<uuid>" }                              │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                   FEEDBACK DOMAIN                                       │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌──────────────────────┐                                                              │
│  │      feedbacks       │                                                              │
│  ├──────────────────────┤                                                              │
│  │ id (PK)              │                                                              │
│  │ meeting_id (FK)      │───────▶ meetings                                             │
│  │ submitted_by (FK)    │───────▶ users                                                │
│  │ about_user (FK)      │───────▶ users                                                │
│  │                      │                                                              │
│  │ meeting_happened     │        (boolean)                                             │
│  │ no_show_reason       │        (nullable)                                            │
│  │                      │                                                              │
│  │ comment              │        (nullable)                                            │
│  │                      │                                                              │
│  │ red_flag_categories  │        (VARCHAR[], nullable)                                 │
│  │ red_flag_description │        (nullable)                                            │
│  │                      │                                                              │
│  │ wants_contact_exchange│       (boolean)                                             │
│  │ contact_shared_at    │        (nullable)                                            │
│  │                      │                                                              │
│  │ created_at           │                                                              │
│  └──────────────────────┘                                                              │
│            │                                                                            │
│            │ 1:1 (only if meeting_happened = true)                                     │
│            ▼                                                                            │
│  ┌──────────────────────┐                                                              │
│  │       ratings        │                                                              │
│  ├──────────────────────┤                                                              │
│  │ id (PK)              │                                                              │
│  │ feedback_id (FK, uq) │───────▶ feedbacks                                            │
│  │ rated_user (FK)      │───────▶ users                                                │
│  │ rated_by (FK)        │───────▶ users                                                │
│  │ looks                │        (1-5)                                                 │
│  │ attitude             │        (1-5)                                                 │
│  │ created_at           │                                                              │
│  └──────────────────────┘                                                              │
│                                                                                         │
│                                                                                         │
│  ┌──────────────────────┐                                                              │
│  │      disputes        │                                                              │
│  ├──────────────────────┤                                                              │
│  │ id (PK)              │                                                              │
│  │ meeting_id (FK)      │───────▶ meetings                                             │
│  │ feedback_id (FK)     │───────▶ feedbacks                                            │
│  │ reported_by (FK)     │───────▶ users                                                │
│  │ reported_user (FK)   │───────▶ users                                                │
│  │ status               │        [OPEN, INVESTIGATING, RESOLVED]                       │
│  │ resolution           │        (nullable)                                            │
│  │ resolution_notes     │        (nullable)                                            │
│  │ resolved_by (FK)     │───────▶ admin_users (nullable)                               │
│  │ resolved_at          │        (nullable)                                            │
│  │ created_at           │                                                              │
│  │ updated_at           │                                                              │
│  └──────────────────────┘                                                              │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                     ADMIN DOMAIN                                        │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌──────────────────────┐                                                              │
│  │     admin_users      │                                                              │
│  ├──────────────────────┤                                                              │
│  │ id (PK)              │                                                              │
│  │ email (unique)       │                                                              │
│  │ name                 │                                                              │
│  │ password_hash        │                                                              │
│  │ role                 │        [REVIEWER, MODERATOR, SUPER_ADMIN]                    │
│  │ is_active            │                                                              │
│  │ created_at           │                                                              │
│  │ updated_at           │                                                              │
│  └──────────────────────┘                                                              │
│            │                                                                            │
│            │ 1:N                                                                        │
│            ├───────────────────────────────────────┐                                   │
│            ▼                                       ▼                                   │
│  ┌──────────────────────┐              ┌──────────────────────┐                        │
│  │    admin_actions     │              │   user_warnings      │                        │
│  ├──────────────────────┤              ├──────────────────────┤                        │
│  │ id (PK)              │              │ id (PK)              │                        │
│  │ admin_id (FK)        │              │ user_id (FK)         │───────▶ users          │
│  │ action_type          │              │ issued_by (FK)       │───────▶ admin_users    │
│  │ target_type          │              │ reason               │                        │
│  │ target_id            │              │ related_dispute_id   │───────▶ disputes       │
│  │ details (JSONB)      │              │ related_feedback_id  │───────▶ feedbacks      │
│  │ created_at           │              │ created_at           │                        │
│  └──────────────────────┘              └──────────────────────┘                        │
│                                                                                         │
│  AdminAction Examples:                                                                 │
│                                                                                         │
│  APPROVAL_REQUEST_APPROVED:                                                            │
│  {                                                                                     │
│    "request_type": "SIGN_UP",                                                          │
│    "user_id": "<uuid>"                                                                 │
│  }                                                                                     │
│                                                                                         │
│  APPROVAL_REQUEST_REJECTED:                                                            │
│  {                                                                                     │
│    "request_type": "PHOTO_ADD",                                                        │
│    "user_id": "<uuid>",                                                                │
│    "reason": "Photo does not meet guidelines"                                          │
│  }                                                                                     │
│                                                                                         │
│  USER_RELEASED_FROM_WAITLIST:                                                          │
│  {                                                                                     │
│    "criteria": "Seoul, age 25-30"                                                      │
│  }                                                                                     │
│                                                                                         │
│  USER_WARNED:                                                                          │
│  {                                                                                     │
│    "reason": "Inappropriate behavior reported",                                        │
│    "related_dispute_id": "<uuid>"                                                      │
│  }                                                                                     │
│                                                                                         │
│  USER_BANNED:                                                                          │
│  {                                                                                     │
│    "reason": "Multiple no-shows"                                                       │
│  }                                                                                     │
│                                                                                         │
│  USER_UNBANNED:                                                                        │
│  {                                                                                     │
│    "reason": "Appeal accepted"                                                         │
│  }                                                                                     │
│                                                                                         │
│  USER_REACTIVATED:                                                                     │
│  {                                                                                     │
│    "from_status": "REJECTED",                                                          │
│    "reason": "Manual review"                                                           │
│  }                                                                                     │
│                                                                                         │
│  DISPUTE_RESOLVED:                                                                     │
│  {                                                                                     │
│    "resolution": "WARNING_ISSUED",                                                     │
│    "notes": "First offense, warning issued",                                           │
│    "affected_user_id": "<uuid>"                                                        │
│  }                                                                                     │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## All Enum Values

| Entity | Field | Values |
|--------|-------|--------|
| users | status | `VERIFICATION_PENDING`, `WAITLISTED`, `REVIEW_PENDING`, `ACTIVE`, `REJECTED`, `BANNED` |
| profiles | gender | `MALE`, `FEMALE` |
| profiles | education | `HIGH_SCHOOL`, `BACHELORS`, `MASTERS`, `DOCTORATE` |
| profiles | income_bracket | `UNDER_50K`, `50K_100K`, `100K_150K`, `150K_200K`, `OVER_200K` |
| profiles | asset_bracket | `UNDER_100K`, `100K_500K`, `500K_1M`, `1M_5M`, `OVER_5M` |
| approval_requests | request_type | `SIGN_UP`, `PHOTO_ADD`, `BIO_CHANGE` |
| approval_requests | status | `PENDING`, `APPROVED`, `REJECTED` |
| approval_requests | target_type | `PROFILE`, `PHOTO` |
| oks | status | `PENDING`, `ACCEPTED`, `DECLINED`, `EXPIRED`, `UNDONE` |
| meetings | status | `PENDING_PROPOSAL`, `PENDING_FINALIZATION`, `SCHEDULED`, `COMPLETED`, `CANCELLED` |
| meetings | cancellation_reason | `PROPOSAL_DEADLINE_MISSED`, `FINALIZATION_DEADLINE_MISSED`, `USER_CANCELLED`, `UNDO_BY_WOMAN`, `UNDO_BY_MAN` |
| meeting_events | event_type | `PROPOSAL_SUBMITTED`, `MEETING_FINALIZED`, `RESCHEDULE_REQUESTED`, `MEETING_CANCELLED`, `MEETING_COMPLETED` |
| feedbacks | no_show_reason | `I_DID_NOT_SHOW`, `THEY_DID_NOT_SHOW`, `MUTUAL_CANCEL`, `OTHER` |
| feedbacks | red_flag_categories | `HARASSMENT`, `MISREPRESENTATION`, `INAPPROPRIATE_BEHAVIOR`, `MADE_UNCOMFORTABLE`, `PHOTOS_NOT_ACCURATE`, `OFFENSIVE_LANGUAGE`, `OTHER` |
| disputes | status | `OPEN`, `INVESTIGATING`, `RESOLVED` |
| disputes | resolution | `NO_ACTION`, `WARNING_ISSUED`, `USER_BANNED`, `INSUFFICIENT_EVIDENCE` |
| admin_users | role | `REVIEWER`, `MODERATOR`, `SUPER_ADMIN` |
| admin_actions | action_type | `APPROVAL_REQUEST_APPROVED`, `APPROVAL_REQUEST_REJECTED`, `USER_RELEASED_FROM_WAITLIST`, `USER_WARNED`, `USER_BANNED`, `USER_UNBANNED`, `USER_REACTIVATED`, `DISPUTE_RESOLVED`, `DISPUTE_REOPENED` |
| admin_actions | target_type | `USER`, `APPROVAL_REQUEST`, `DISPUTE` |

---

## All Relationships

| From | To | Type | FK Column |
|------|-----|------|-----------|
| profiles | users | N:1 | user_id |
| user_photos | profiles | N:1 | profile_id |
| approval_requests | users | N:1 | user_id |
| approval_requests | admin_users | N:1 | reviewed_by |
| oks | users | N:1 | sender_id |
| oks | users | N:1 | recipient_id |
| oks | users | N:1 | undone_by |
| meetings | oks | 1:1 | ok_id |
| meetings | users | N:1 | woman_id |
| meetings | users | N:1 | man_id |
| meetings | users | N:1 | cancelled_by |
| meeting_proposals | meetings | N:1 | meeting_id |
| meeting_events | meetings | N:1 | meeting_id |
| meeting_events | users | N:1 | actor_id |
| feedbacks | meetings | N:1 | meeting_id |
| feedbacks | users | N:1 | submitted_by |
| feedbacks | users | N:1 | about_user |
| ratings | feedbacks | 1:1 | feedback_id |
| ratings | users | N:1 | rated_user |
| ratings | users | N:1 | rated_by |
| disputes | meetings | N:1 | meeting_id |
| disputes | feedbacks | N:1 | feedback_id |
| disputes | users | N:1 | reported_by |
| disputes | users | N:1 | reported_user |
| disputes | admin_users | N:1 | resolved_by |
| admin_actions | admin_users | N:1 | admin_id |
| user_warnings | users | N:1 | user_id |
| user_warnings | admin_users | N:1 | issued_by |
| user_warnings | disputes | N:1 | related_dispute_id |
| user_warnings | feedbacks | N:1 | related_feedback_id |

---

## Key Indexes

| Table | Index | Columns | Notes |
|-------|-------|---------|-------|
| users | idx_users_email | email | Unique, login lookup |
| users | idx_users_status | status | Filter by status |
| profiles | idx_profiles_user_id | user_id | Unique, 1:1 lookup |
| profiles | idx_profiles_gender_active | gender | Filter for matching (partial: where user is active) |
| user_photos | idx_user_photos_profile_active | profile_id, is_active | Get active photos |
| approval_requests | idx_approval_requests_status | status, request_type | Admin queue |
| oks | idx_oks_sender_status | sender_id, status | Woman's active oks |
| oks | idx_oks_recipient_status | recipient_id, status | Man's pending queue |
| oks | idx_oks_expires_at | expires_at | Expiration job |
| oks | idx_oks_pair | sender_id, recipient_id | One ok per pair constraint |
| meetings | idx_meetings_status | status | Active meetings |
| meetings | idx_meetings_deadlines | proposal_deadline, finalization_deadline | Deadline enforcement |
| feedbacks | idx_feedbacks_meeting | meeting_id | Get feedbacks for meeting |
| ratings | idx_ratings_rated_user | rated_user | Average calculation |
| disputes | idx_disputes_status | status | Admin queue |
| admin_actions | idx_admin_actions_target | target_type, target_id | Audit lookup |
| user_warnings | idx_user_warnings_user | user_id | Warning count |

---

## Validation Rules (Application Logic)

> **Note:** For detailed state transitions, see [STATE-MACHINES.md](./STATE-MACHINES.md)

### User Domain

| Rule | Description |
|------|-------------|
| Min 3 photos | User must have at least 3 active photos to be active |
| Photo removal | Allowed without approval, but cannot go below 3 photos |
| Immutable fields | display_name, birth_date, gender, height_cm, profession, education, income_bracket, asset_bracket cannot be changed after profile creation |
| Mutable fields | bio requires approval request to change |
| Admin override | Admin can reactivate REJECTED users or unban BANNED users |

### Matching Domain

| Rule | Description |
|------|-------------|
| Max 3 active oks | Woman can have at most 3 oks in PENDING or ACCEPTED status (with non-COMPLETED/CANCELLED meetings) |
| One ok per pair | Woman can only send one ok to a specific man ever (any terminal state blocks retry) |
| Ok expiration | 7 days from sent_at |
| Proposal deadline | 2 days from meeting creation |
| Finalization deadline | 2 days from proposal submission |
| Meeting deadline | 3 weeks from meeting creation (all proposals must be within this) |
| Max 1 reschedule | is_rescheduled boolean, reschedule returns to PENDING_PROPOSAL |
| Undo before SCHEDULED | Ok can only be undone before meeting reaches SCHEDULED status |

### Feedback Domain

| Rule | Description |
|------|-------------|
| Mandatory feedback | User cannot use app until feedback submitted (3h after scheduled_at) |
| Rating required | looks and attitude ratings required when meeting_happened = true |
| Contact exchange | Only allowed when meeting_happened = true |
| No-show dispute | Auto-create dispute when no_show_reason = THEY_DID_NOT_SHOW |
| Two disputes possible | If both users claim the other didn't show, two separate disputes are created |

### Admin Domain

| Role | Permissions |
|------|-------------|
| REVIEWER | Approve/reject approval requests |
| MODERATOR | REVIEWER + resolve disputes, warn users |
| SUPER_ADMIN | All permissions + ban/unban users, reactivate rejected users, manage admins, release waitlist |
