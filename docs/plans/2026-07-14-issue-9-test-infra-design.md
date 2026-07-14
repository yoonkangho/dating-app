# Design — Issue #9: Test infrastructure and entity unit tests

Date: 2026-07-14
Issue: [#9 — Test infrastructure and entity unit tests](https://github.com/yoonkangho/dating-app/issues/9)
Milestone: Milestone 1 — Foundation
Branch: `issue-9-test-infra`

## Goal

Close out Milestone 1 by:

1. Filling remaining gaps in `TestFixtures` (matching/feedback/admin fixtures were partly added by PRs #48–#55; `AdminAction`, `UserWarning` still missing, and the existing fixtures need deterministic-time defaults + expanded `Meeting` signature).
2. Adding entity unit tests for the three state machines (`User`, `Ok`, `Meeting`) with deep coverage — every happy path and every illegal-source-status rejection.
3. Adding a `ModulithArchitectureTest` that enforces module boundaries via Spring Modulith.
4. Refactoring time-sensitive entity methods so tests are deterministic without wall-clock dependencies (pattern B1: pass `now: Instant` into mutating methods).

## Context

Milestone 1 arrived at this point via PRs #48–#55: `IntegrationTestBase`, `TestcontainersConfiguration`, an `AppConfig.clock()` Spring bean, `TestFixtures` with User/Profile/UserPhoto/ApprovalRequest/AdminUser/Ok/Meeting/MeetingProposal/MeetingEvent/Feedback/Rating/Dispute factories, and repository integration tests for every module. What remains for issue #9 is (a) the entity unit tests and (b) `ModulithArchitectureTest`, plus two missing fixtures (`AdminAction`, `UserWarning`) and a signature expansion on `createMeeting`.

Time-sensitive entity methods (`Ok.accept`, `Ok.isExpired`, `Meeting.finalize`, `Meeting.isProposalDeadlineMissed`, `User.approve`, etc.) currently call `Instant.now()` internally. Testing them reliably requires refactoring to inject time. We chose to refactor.

## Design decisions

### 1. Inject `now` into entity mutating methods (pattern B1)

Mutating methods take `now: Instant` as a parameter; entities do not depend on `Clock`. Services (Milestone 2+) will hold the already-wired `AppConfig.clock()` bean and pass `clock.instant()` at call sites. This matches the pattern already sketched in `skills/TESTING.md`.

Constructor default parameter values (e.g., `createdAt: Instant = Instant.now()`) are left unchanged — callers who care override them, and tests always do.

Rejected alternatives:
- **Pass `Clock` into methods.** Heavier abstraction inside plain-domain entities; unnecessary now that services own the clock.
- **Global `nowSupplier`.** Global mutable state; makes parallel tests fragile.

### 2. Deep test coverage for state transitions

Each mutating method gets a `@Nested` class in its entity's test file with:
- Happy path for every valid source status.
- One "throws when status is X" test for every illegal source status.
- Coverage of any additional invariants (e.g., `Ok.undo` requires caller is sender or recipient; `Meeting.finalize` requires date before `meetingDeadline`).
- Derived predicates (`Ok.isExpired`, `Meeting.canReschedule`, deadline-missed predicates) each get their own `@Nested` block.

Ballpark: ~100 unit tests across `UserTest` / `OkTest` / `MeetingTest`. All pure Kotlin, no Spring context.

Rejected alternative: shallow coverage (happy path + one smoke rejection). The state machines are the core domain invariants — cheap tests now, expensive to backfill through the service layer later.

### 3. Minimal `ModulithArchitectureTest`

Just `ApplicationModules.of(DatingAppApplication::class.java).verify()`. In Spring Modulith 2.x, `verify()` already covers boundary + `internal/` visibility + cycle detection.

Rejected extension: `Documenter(modules).writeDocumentation()`. Nice-to-have but leaves an unowned build artifact; can be added later if we want auto-generated module docs.

## Scope

### Modified

- `src/main/kotlin/com/kenneth/datingapp/user/User.kt`
  - `markVerified`, `releaseFromWaitlist`, `approve`, `reject`, `ban`, `unban`, `reactivate` — each accepts `now: Instant`.
- `src/main/kotlin/com/kenneth/datingapp/matching/Ok.kt`
  - `accept`, `decline`, `markExpired`, `undo` — accept `now: Instant`.
  - `isExpired(now: Instant): Boolean` — signature changes.
- `src/main/kotlin/com/kenneth/datingapp/matching/Meeting.kt`
  - `markProposalSubmitted`, `finalize`, `reschedule`, `cancel`, `complete` — accept `now: Instant`.
  - `isProposalDeadlineMissed(now)`, `isFinalizationDeadlineMissed(now)` — signature changes.
- `src/test/kotlin/com/kenneth/datingapp/support/TestFixtures.kt`
  - Add `FIXED_NOW: Instant = Instant.parse("2026-01-15T10:00:00Z")` constant.
  - Add `uuid(name: String): UUID` deterministic helper.
  - Add `createAdminAction`, `createUserWarning` factories.
  - Expand `createMeeting` signature so tests can override `proposalDeadline`, `finalizationDeadline`, `meetingDeadline`, `scheduledAt`, `locationName`, `locationAddress`, `isRescheduled`, `cancelledBy`, `cancelledAt`, `cancellationReason`.
  - Change existing time-using fixtures (`createOk`, `createMeeting`, `createMeetingProposal`) to derive time defaults from `FIXED_NOW` instead of `Instant.now()`.

### Added

- `src/test/kotlin/com/kenneth/datingapp/user/UserTest.kt` — ~40 tests, one `@Nested` per transition.
- `src/test/kotlin/com/kenneth/datingapp/matching/OkTest.kt` — ~25 tests, one `@Nested` per method + `IsExpired`.
- `src/test/kotlin/com/kenneth/datingapp/matching/MeetingTest.kt` — ~35 tests, one `@Nested` per method + deadline predicates.
- `src/test/kotlin/com/kenneth/datingapp/ModulithArchitectureTest.kt` — `modules.verify()`.

### Untouched

- `IntegrationTestBase.kt`, `TestcontainersConfiguration.kt`, existing repository integration tests.
- `MeetingProposal` (no time-dependent mutation), all feedback/admin entities (no state-machine methods to test here).

## Out of scope

- Wiring services to consume `Clock` — belongs to Milestone 2 (issue #10, UserService is the first consumer).
- MockK setup — no service tests yet, no dependency added to `build.gradle.kts`.
- `Documenter` output for Modulith — deferred.

## Verification gate

- `./gradlew test` passes.
- `ModulithArchitectureTest` passes (no boundary violations).
- No existing callers of refactored entity methods break — verified via grep; no service code exists yet, only repository tests that don't invoke mutating methods.
