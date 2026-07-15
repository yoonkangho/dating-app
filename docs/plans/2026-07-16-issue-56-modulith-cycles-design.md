# Design — Issue #56: Resolve Spring Modulith cyclic dependencies

Date: 2026-07-16
Issue: [#56 — Resolve cyclic dependencies between Spring Modulith modules](https://github.com/yoonkangho/dating-app/issues/56)
Milestone: Milestone 2 — User Module (blocker)
Branch: `issue-56-modulith-cycles`

## Goal

Restore strict `ApplicationModules.of(...).verify()` in `ModulithArchitectureTest` by
eliminating the four cyclic dependencies detected during #9.

## Root cause and analysis

The natural module hierarchy is `user ← matching ← feedback ← admin` (admin sits at
the top and audits every other module). All but two cross-module `@ManyToOne`
references already flow *down* this hierarchy:

- `matching → user` (Ok, Meeting, MeetingEvent reference User) — forward, OK
- `feedback → user` (Feedback, Dispute, Rating) — forward, OK
- `feedback → matching` (Feedback.meeting, Dispute.meeting) — forward, OK
- `admin → user` (UserWarning.user) — forward, OK
- `admin → feedback` (UserWarning.relatedDispute, UserWarning.relatedFeedback) — forward, OK

Only **two** arrows point the wrong direction, and they cause every reported cycle:

1. `user.ApprovalRequest.reviewedBy: AdminUser?` — creates `user ↔ admin`
2. `feedback.Dispute.resolvedBy: AdminUser?` — creates `feedback ↔ admin` and the
   multi-hop `user → admin → feedback → matching → user` cycle

Both fields are **nullable audit fields** set only after an admin acts. Neither is
needed at entity-construction time, so replacing the JPA `@ManyToOne` with a
`UUID?` column eliminates the compile-time coupling without changing behavior.

## Design decisions

### 1. Narrow surgical UUID conversion (not full refactor)

Convert only the two backward-flowing fields. Leave every other cross-module
`@ManyToOne` in place — they already flow with the hierarchy and provide
convenient JPA fetching.

Rejected alternatives:

- **Move `ApprovalRequest` to the `admin` module.** Not needed to break cycles.
  `ApprovalRequest` is also queried by users to see their own approval status,
  so its current location in `user` is defensible. Deferred as a separate
  question that can be re-litigated later.
- **Full UUID FK refactor across every module boundary.** Uniform but enormous
  churn. All forward-flowing refs are cheap and harmless; refactoring them
  provides no cycle-breaking benefit.
- **Named-interface / `@ApplicationModule(allowedDependencies=...)`.** Does not
  permit cycles. Would not resolve the violations.
- **Domain events replacing the audit refs.** Loses DB-level referential integrity
  and requires event listener infrastructure that lands in M6. Overkill for two
  nullable audit fields.

### 2. Preserve database FK constraints

The DB columns are already `UUID` (V2 for `approval_requests.reviewed_by`,
V4 for `disputes.resolved_by`), and V5 added deferred FKs to `admin_users(id)`
with `ON DELETE SET NULL`. No schema migration is needed. Hibernate treats
the field as a plain `UUID`; the database still enforces the FK.

### 3. Align method signatures with #9's `now: Instant` pattern

While changing `approve(admin: AdminUser, ...)` / `reject(admin: AdminUser, ...)` /
`resolve(admin: AdminUser, ...)` to accept `adminId: UUID`, also add `now: Instant`
consistent with the B1 pattern introduced in #9 for User/Ok/Meeting. This costs
one extra parameter per method but keeps the entity API consistent across the
codebase. Currently `ApprovalRequest.approve/reject/reopen` and
`Dispute.startInvestigation/resolve/reopen` all call `Instant.now()` internally.

### 4. Add entity unit tests for the affected methods

`ApprovalRequestTest` and `DisputeTest` are added to cover the state transitions
of the mutating methods, following the `@Nested`-per-method pattern established
by `UserTest`/`OkTest`/`MeetingTest`. #9 explicitly left these entities' state
machines untested; the signature change here is a natural moment to close that
gap.

### 5. Strict `verify()` in `ModulithArchitectureTest`

Drop the `"Cycle detected"` filter and simplify the test to a single
`modules.verify()` call. Rewrite the class KDoc to remove the deferral note.

## Scope

### Modified

- `src/main/kotlin/com/kenneth/datingapp/user/ApprovalRequest.kt`
  - Remove `import com.kenneth.datingapp.admin.AdminUser`.
  - `reviewedBy: AdminUser?` → `reviewedByAdminId: UUID?` with
    `@Column(name = "reviewed_by")`.
  - `approve(admin: AdminUser, notes: String?)` →
    `approve(adminId: UUID, now: Instant, notes: String?)`.
  - `reject(admin: AdminUser, notes: String?)` →
    `reject(adminId: UUID, now: Instant, notes: String?)`.
- `src/main/kotlin/com/kenneth/datingapp/feedback/Dispute.kt`
  - Remove `import com.kenneth.datingapp.admin.AdminUser`.
  - `resolvedBy: AdminUser?` → `resolvedByAdminId: UUID?` with
    `@Column(name = "resolved_by")`.
  - `startInvestigation()` → `startInvestigation(now: Instant)`.
  - `resolve(admin: AdminUser, resolution, notes)` →
    `resolve(adminId: UUID, resolution, now: Instant, notes: String?)`.
  - `reopen()` → `reopen(now: Instant)`; sets `resolvedByAdminId = null`.
- `src/test/kotlin/com/kenneth/datingapp/ModulithArchitectureTest.kt`
  - Drop cycle filter; single `modules.verify()` call.
  - Rewrite KDoc to remove #56 deferral reference.

### Added

- `src/test/kotlin/com/kenneth/datingapp/user/ApprovalRequestTest.kt` —
  `@Nested Approve`, `@Nested Reject`, covering happy path and rejection for
  each illegal source status.
- `src/test/kotlin/com/kenneth/datingapp/feedback/DisputeTest.kt` —
  `@Nested StartInvestigation`, `@Nested Resolve`, `@Nested Reopen`, covering
  happy path, illegal source status, and (for `Reopen`) verifying that
  `resolvedByAdminId`, `resolution`, `resolutionNotes`, and `resolvedAt` are
  all cleared.

### Untouched

- `TestFixtures.createApprovalRequest` / `createDispute` — neither takes
  admin parameters today; no signature change needed.
- Existing `ApprovalRequestRepositoryTest`, `DisputeRepositoryTest` — do not
  set the reviewer/resolver fields; no changes needed.
- All other cross-module `@ManyToOne` refs (matching→user, feedback→user,
  feedback→matching, admin→user, admin→feedback) — forward-flowing, not
  cyclic.
- Flyway migrations — DB schema already matches the new mapping.

## Out of scope

- Moving `ApprovalRequest` to the `admin` module.
- Converting other forward-flowing cross-module refs to UUID.
- Introducing domain events for audit-only links (belongs to M6).
- Service-layer wiring for `approve`/`reject`/`resolve` — no service code
  exists yet; UserService (M2 issue #10) and dispute service (M4) will be
  the first callers.

## Verification gate

- `./gradlew test` passes.
- `ModulithArchitectureTest.modules pass all boundary checks` passes with
  strict `verify()` and no cycle filter.
- `ApprovalRequestTest` and `DisputeTest` cover every mutating method.
- Existing repository integration tests still pass.
