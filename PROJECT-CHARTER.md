# Project Charter: Guaranteed Meeting Dating App

**Version:** 0.2  
**Last Updated:** 2026-02-15  
**Author:** Kenneth

> **Related Documents:**
> - [ERD.md](./ERD.md) — Database schema and relationships
> - [STATE-MACHINES.md](./STATE-MACHINES.md) — Detailed state transitions and business rules
> - [CLAUDE.md](./CLAUDE.md) — AI development context

---

## 1. Project Overview

### 1.1 Vision Statement

A dating app where every match leads to a real meeting. Quality over quantity through curated membership and structured outcomes.

### 1.2 Project Goals

| Goal | Type | Success Criteria |
|------|------|------------------|
| Fully functional dating app | Product | All core flows working end-to-end |
| Clean engineering | Technical | Maintainable codebase, clear architecture |
| Production deployment | Operational | Live and usable (scale not a concern) |
| Learn AI-assisted development | Learning | Develop effective workflows for AI collaboration |

### 1.3 Core Differentiators

1. **Curated membership** — Admin approval gates entry for quality control
2. **Asymmetric discovery** — Women browse first; men only see women who've expressed interest
3. **Structured outcomes** — Match → Schedule → Meet → Feedback. No endless chat.
4. **Reputation signals** — User behavior (responsiveness, reliability) is tracked and visible

---

## 2. User Flows

### 2.1 Sign-up & Approval Flow

```
Sign-up Form
    ↓
Identity Verification (3rd party, pass/fail)
    ↓ fail → Silent rejection (user sees "pending" indefinitely)
    ↓ pass
Waitlist
    ↓ manual release by admin (individual users)
Admin Review Queue
    ↓ Multiple admins review via separate backoffice
    ↓ approve → Active user (Profile created)
    ↓ reject → Silent rejection (user sees "pending" indefinitely)
```

**Approval Criteria by Gender:**

| Criteria | Women | Men |
|----------|-------|-----|
| Identity verification | System (auto) | System (auto) |
| Photo quality | Admin review | — |
| Bio quality | Admin review | Admin review |
| Income/Education | — | Admin review |

**Key Rules:**
- Rejection is final — no re-applications allowed
- Rejected users are never notified; they remain in "pending" state
- Admin can reactivate rejected users (REJECTED → ACTIVE)
- Admin can unban banned users (BANNED → ACTIVE)
- Waitlist release is manual, one user at a time
- Users on waitlist know only that they are waitlisted (no position shown)

### 2.2 Matching Flow

```
Woman browses men
    ↓ filters: age, location, height, education, profession, income bracket
    ↓
Woman "ok"s a man (max 3 active across all stages)
    ↓
Man receives immediate notification
    ↓
Man views woman's profile (queue order, FIFO)
    ↓ has 7 days to respond
    ↓
    ├── "ok" → Proceed to Meeting Arrangement
    ├── decline → Woman notified of rejection (terminal, no retry)
    └── no response (7 days) → "ok" expires (terminal, no retry)
```

**Key Rules:**
- **One ok per pair** — Woman can only send one ok to a specific man ever
- **Any terminal state blocks retry** — DECLINED, EXPIRED, UNDONE all prevent future ok to same man
- Women can have max 3 active "ok"s across all stages (pending + arranging)
- Men see interested women in queue order (first-in-first-out)
- Either party can undo "ok" before meeting is SCHEDULED:
    - Undoer's profile permanently shows "has undone before"
    - Meeting cancelled with reason `UNDO_BY_WOMAN` or `UNDO_BY_MAN`
- If man lets "ok" expire: profile permanently shows "has let ok expire"

### 2.3 Meeting Arrangement Flow

```
Mutual "ok" achieved
    ↓ creates Meeting (status: PENDING_PROPOSAL)
    ↓ (within 2 days)
Woman proposes 4 date options + preferred locations
    ↓ Meeting status: PENDING_FINALIZATION
    ↓ (within 2 days)
Man selects final date + finalizes meeting place
    ↓ Meeting status: SCHEDULED
Meeting happens
    ↓ Meeting status: COMPLETED
```

**Timelines:**

| Step | Deadline | Consequence of Missing |
|------|----------|------------------------|
| Woman proposes dates/locations | 2 days from mutual ok | Meeting cancelled (PROPOSAL_DEADLINE_MISSED) |
| Man finalizes date/place | 2 days from woman's proposal | Meeting cancelled (FINALIZATION_DEADLINE_MISSED) |
| Meeting occurs | Within 3 weeks of mutual ok | All proposals must be within this window |

**Rescheduling:**
- Either party can request reschedule (max 1 per match)
- Reschedule request immediately voids current schedule
- Meeting returns to PENDING_PROPOSAL status
- Woman must propose 4 new dates within 2 days
- Man must finalize within 2 days
- All new proposals must still be within 3 weeks of original mutual ok
- `is_rescheduled` flag set to true (cannot reschedule again)

**Cancellation:**
- User can explicitly cancel meeting → status CANCELLED (USER_CANCELLED)

**Communication:**
- Structured fields only (no free-form chat)
- Date/time selection + location input

### 2.4 Feedback Flow

```
Meeting scheduled time + 3 hours
    ↓
Meeting status: COMPLETED
    ↓
Both parties prompted for mandatory feedback
    ↓
User cannot use app until feedback submitted
    ↓
Feedback captured:
    - Did meeting happen? (y/n)
    - If no: reason (I_DID_NOT_SHOW / THEY_DID_NOT_SHOW / MUTUAL_CANCEL / OTHER)
    - If yes: Ratings (looks: 1-5, attitude: 1-5)
    - Free text comments
    - Red flags (multi-select categories + description)
    - Did you want to exchange contacts? (y/n, only if meeting happened)
    ↓
If contact exchange = yes → Phone number sent to counterparty immediately
    ↓
If no_show_reason = THEY_DID_NOT_SHOW → Dispute auto-created
    ↓
Admin investigates → Action taken
```

**Key Rules:**
- Feedback is mandatory — full app lockout until submitted
- Contact exchange only allowed if meeting happened
- Contact exchange is one-sided (if you say yes, your number is sent regardless of their choice)
- All feedback is reviewed by admins for moderation
- If both users claim the other didn't show, two separate disputes are created

---

## 3. Reputation & Trust System

### 3.1 Visible Profile Indicators

| Indicator | Trigger | Visibility |
|-----------|---------|------------|
| "Has undone an ok" | User withdrew an "ok" before meeting SCHEDULED | Shown to potential matches |
| "Has let ok expire" | Man did not respond within 7 days | Shown to potential matches |

### 3.2 Internal Tracking (Admin-Only)

| Metric | Trigger |
|--------|---------|
| Reschedule requests | User requested to reschedule |
| Meeting cancellations | User cancelled meeting |
| Feedback ratings received | Aggregated from counterparties |
| Dispute outcomes | Results of no-show investigations |
| Warnings issued | Admin warnings |

---

## 4. Admin System

### 4.1 Admin Roles

| Role | Permissions |
|------|-------------|
| REVIEWER | Approve/reject approval requests (sign-ups, photos, bio changes) |
| MODERATOR | REVIEWER + resolve disputes, warn users |
| SUPER_ADMIN | All permissions + ban/unban users, reactivate rejected users, manage admins, release waitlist |

### 4.2 Admin Actions (Audited)

All admin actions are logged with admin ID, timestamp, and details:

- `APPROVAL_REQUEST_APPROVED` / `APPROVAL_REQUEST_REJECTED`
- `USER_RELEASED_FROM_WAITLIST`
- `USER_WARNED`
- `USER_BANNED` / `USER_UNBANNED`
- `USER_REACTIVATED` (REJECTED → ACTIVE)
- `DISPUTE_RESOLVED` / `DISPUTE_REOPENED`

### 4.3 Unified Approval Queue

Single queue handles all approval types:
- Sign-up requests (creates Profile on approval)
- Photo additions (activates photo on approval)
- Bio changes (updates bio on approval)

Approval request statuses: PENDING → APPROVED or REJECTED (both terminal)

---

## 5. Technical Architecture

### 5.1 Technology Stack

| Layer | Technology | Notes |
|-------|------------|-------|
| Backend | Kotlin 2.3.0 + Spring Boot 4.0.1 | Spring Modulith for modular architecture |
| Database | PostgreSQL | With Flyway migrations |
| Caching | Redis | Session, caching |
| Frontend (Mobile/Web) | TBD | PWA, React Native, or Flutter |
| Identity Verification | 3rd party service | Provider TBD; design for abstraction |
| Hosting | TBD | Cloud provider to be selected |

### 5.2 Module Structure

```
com.kenneth.datingapp
├── user/          # User accounts, profiles, photos, approval requests
├── matching/      # Ok flow, meetings, proposals, scheduling
├── feedback/      # Post-meeting feedback, ratings, disputes
└── admin/         # Admin users, actions, warnings
```

### 5.3 System Components

1. **User App** — Sign-up, browsing, matching, scheduling, feedback
2. **Admin Backoffice** — User approval, dispute resolution, moderation, analytics
3. **Notification Service** — Push notifications, reminders, alerts
4. **Identity Service** — Integration with 3rd party verification provider

---

## 6. Scope & Boundaries

### 6.1 In Scope (MVP)

- User registration and profile creation
- Identity verification integration (abstracted)
- Waitlist and admin approval workflow
- Asymmetric matching flow (women initiate)
- Structured meeting arrangement with rescheduling
- Mandatory feedback collection with ratings
- Contact exchange mechanism
- Reputation indicators
- Admin backoffice for approvals, disputes, moderation
- Full audit logging
- Basic notifications

### 6.2 Out of Scope (MVP)

- Scalability optimizations
- Payment/subscription system
- In-app messaging/chat
- Advanced matching algorithms
- Analytics dashboards
- Native mobile apps (if starting with PWA)
- Internationalization/localization
- Credit system (deferred)

### 6.3 Explicit Non-Goals

- Competing with mainstream dating apps on features
- Handling high scale (thousands of concurrent users)
- Monetization in initial version

---

## 7. AI-Assisted Development Goals

### 7.1 Learning Objectives

This project serves as a vehicle to learn effective AI-assisted software development:

1. **Skill definitions** — Create and refine custom skill files for consistent AI collaboration
2. **Context management** — Learn what context AI agents need to be maximally effective
3. **Task decomposition** — Practice breaking work into AI-friendly chunks
4. **Prompt patterns** — Develop reusable prompt templates for common tasks
5. **Quality workflows** — Establish review and validation patterns for AI-generated code

### 7.2 Documentation Structure

| Document | Purpose |
|----------|---------|
| `CLAUDE.md` | AI context entry point, build commands, architecture overview |
| `PROJECT-CHARTER.md` | Product requirements and user flows |
| `ERD.md` | Database schema, relationships, indexes |
| `STATE-MACHINES.md` | State transitions and business rules |
| `skills/BACKEND.md` | Kotlin/Spring Boot patterns |
| `skills/DATABASE.md` | PostgreSQL/JPA patterns |
| `skills/TESTING.md` | Testing patterns |

---

## 8. Open Questions

| Question | Status | Notes |
|----------|--------|-------|
| What identity verification provider? | TBD | Need to research cost, coverage, API quality |
| What frontend technology? | TBD | Depends on target platforms and your familiarity |
| What cloud platform? | TBD | Cost and complexity tradeoffs |
| Credit system details? | Deferred | How are credits earned/purchased? Costs for actions? |

---

## 9. Glossary

| Term | Definition |
|------|------------|
| Ok | Expression of interest in a potential match |
| Mutual ok | Both parties have "ok"d each other; creates Meeting |
| Active ok | An "ok" that is PENDING or ACCEPTED with non-completed meeting |
| Waitlist | Holding pool for verified users awaiting admin release |
| Backoffice | Separate admin application for management functions |
| Dispute | Investigation triggered by no-show report |
| Reschedule | Voiding current schedule and restarting proposal flow |
