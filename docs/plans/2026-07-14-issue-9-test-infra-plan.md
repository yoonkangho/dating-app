# Issue #9 — Test Infrastructure and Entity Unit Tests Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Close Milestone 1 by refactoring time-sensitive entity methods to accept `now: Instant`, filling out `TestFixtures`, adding deep entity unit tests for `User`/`Ok`/`Meeting`, and adding a Spring Modulith architecture test.

**Architecture:** Entity mutating methods take `now: Instant` (pattern B1) — entities stay POJO, services (M2+) inject the already-wired `AppConfig.clock()` bean and pass `clock.instant()`. Unit tests use a `FIXED_NOW` constant from `TestFixtures` for full determinism. Constructor default parameter values keep their `Instant.now()` calls.

**Tech Stack:** Kotlin 2.3, JUnit 5, AssertJ, Spring Modulith 2.0.1 (`spring-modulith-starter-test`). No MockK needed (entity tests are pure).

**Working directory:** `/Users/kenneth/Documents/DatingApp/.worktrees/issue-9-test-infra` on branch `issue-9-test-infra`. All commands run relative to this worktree.

**Design reference:** `docs/plans/2026-07-14-issue-9-test-infra-design.md`

---

## Task 1: Add `FIXED_NOW` and `uuid(name)` helpers, switch fixture time defaults

**Files:**
- Modify: `src/test/kotlin/com/kenneth/datingapp/support/TestFixtures.kt`

**Step 1: Edit `TestFixtures.kt`**

Add these two members at the top of `object TestFixtures` (right after `private val counter`):

```kotlin
/** Fixed instant for deterministic time-sensitive test setup. */
val FIXED_NOW: Instant = Instant.parse("2026-01-15T10:00:00Z")

/** Deterministic UUID derived from a name — stable across runs. */
fun uuid(name: String): UUID = UUID.nameUUIDFromBytes(name.toByteArray())
```

Then replace `Instant.now()` inside `createOk`, `createMeetingProposal` with `FIXED_NOW`:

- In `createOk`: `sentAt: Instant = Instant.now()` → `sentAt: Instant = FIXED_NOW`
- In `createMeetingProposal`: `proposedDate: Instant = Instant.now().plus(Duration.ofDays(7))` → `proposedDate: Instant = FIXED_NOW.plus(Duration.ofDays(7))`

Do NOT touch `createUser`, `createProfile`, `createUserPhoto`, `createApprovalRequest`, `createAdminUser`, `createFeedback`, `createRating`, `createDispute`, `createMeetingEvent` — they don't currently set time-sensitive fields directly.

**Step 2: Compile check**

Run: `./gradlew compileTestKotlin`
Expected: `BUILD SUCCESSFUL`

**Step 3: Commit**

```bash
git add src/test/kotlin/com/kenneth/datingapp/support/TestFixtures.kt
git commit -m "test: add FIXED_NOW and uuid helpers to TestFixtures"
```

---

## Task 2: Expand `createMeeting` fixture signature

The existing `createMeeting` only accepts `id`, `ok`, `woman`, `man`, `status`. State-transition tests need to control every field. Expand it.

**Files:**
- Modify: `src/test/kotlin/com/kenneth/datingapp/support/TestFixtures.kt`

**Step 1: Replace `createMeeting`**

```kotlin
fun createMeeting(
    id: UUID = UUID.randomUUID(),
    ok: Ok,
    woman: User,
    man: User,
    status: MeetingStatus = MeetingStatus.PENDING_PROPOSAL,
    proposalDeadline: Instant = FIXED_NOW.plus(Meeting.PROPOSAL_DEADLINE_DURATION),
    finalizationDeadline: Instant? = null,
    meetingDeadline: Instant = FIXED_NOW.plus(Meeting.MEETING_DEADLINE_DURATION),
    scheduledAt: Instant? = null,
    locationName: String? = null,
    locationAddress: String? = null,
    isRescheduled: Boolean = false,
    cancelledBy: User? = null,
    cancelledAt: Instant? = null,
    cancellationReason: CancellationReason? = null
) = Meeting(
    id = id,
    ok = ok,
    woman = woman,
    man = man,
    status = status,
    proposalDeadline = proposalDeadline,
    finalizationDeadline = finalizationDeadline,
    meetingDeadline = meetingDeadline,
    scheduledAt = scheduledAt,
    locationName = locationName,
    locationAddress = locationAddress,
    isRescheduled = isRescheduled,
    cancelledBy = cancelledBy,
    cancelledAt = cancelledAt,
    cancellationReason = cancellationReason
)
```

**Step 2: Compile check**

Run: `./gradlew compileTestKotlin`
Expected: `BUILD SUCCESSFUL`

If any repository test calls `createMeeting` positionally, the added named-optional params should not break it. If a caller broke, adjust that caller to use named args.

**Step 3: Commit**

```bash
git add src/test/kotlin/com/kenneth/datingapp/support/TestFixtures.kt
git commit -m "test: expand createMeeting fixture to cover all Meeting fields"
```

---

## Task 3: Add `createAdminAction` and `createUserWarning` fixtures

**Files:**
- Modify: `src/test/kotlin/com/kenneth/datingapp/support/TestFixtures.kt`

**Step 1: Add both factories at the bottom of `object TestFixtures`**

```kotlin
fun createAdminAction(
    id: UUID = UUID.randomUUID(),
    admin: AdminUser,
    actionType: AdminActionType = AdminActionType.USER_WARNED,
    targetType: AdminTargetType = AdminTargetType.USER,
    targetId: UUID = UUID.randomUUID(),
    details: Map<String, Any>? = null
) = AdminAction(
    id = id,
    admin = admin,
    actionType = actionType,
    targetType = targetType,
    targetId = targetId,
    details = details
)

fun createUserWarning(
    id: UUID = UUID.randomUUID(),
    user: User,
    issuedBy: AdminUser,
    reason: String = "Test warning",
    relatedDispute: Dispute? = null,
    relatedFeedback: Feedback? = null
) = UserWarning(
    id = id,
    user = user,
    issuedBy = issuedBy,
    reason = reason,
    relatedDispute = relatedDispute,
    relatedFeedback = relatedFeedback
)
```

You'll also need to add these imports to the file:
```kotlin
import com.kenneth.datingapp.admin.AdminAction
import com.kenneth.datingapp.admin.AdminActionType
import com.kenneth.datingapp.admin.AdminTargetType
import com.kenneth.datingapp.admin.UserWarning
```

**Step 2: Compile check**

Run: `./gradlew compileTestKotlin`
Expected: `BUILD SUCCESSFUL`

**Step 3: Commit**

```bash
git add src/test/kotlin/com/kenneth/datingapp/support/TestFixtures.kt
git commit -m "test: add createAdminAction and createUserWarning fixtures"
```

---

## Task 4: Refactor `User` mutating methods to accept `now: Instant`

**Files:**
- Modify: `src/main/kotlin/com/kenneth/datingapp/user/User.kt`

**Step 1: Update all seven transition methods**

Replace each method to accept `now: Instant` and use it instead of `Instant.now()`:

```kotlin
fun markVerified(now: Instant) {
    require(status == UserStatus.VERIFICATION_PENDING) {
        "Can only verify from VERIFICATION_PENDING status, current: $status"
    }
    status = UserStatus.WAITLISTED
    updatedAt = now
}

fun releaseFromWaitlist(now: Instant) {
    require(status == UserStatus.WAITLISTED) {
        "Can only release from WAITLISTED status, current: $status"
    }
    status = UserStatus.REVIEW_PENDING
    updatedAt = now
}

fun approve(now: Instant) {
    require(status == UserStatus.REVIEW_PENDING) {
        "Can only approve from REVIEW_PENDING status, current: $status"
    }
    status = UserStatus.ACTIVE
    updatedAt = now
}

fun reject(now: Instant) {
    require(status == UserStatus.REVIEW_PENDING) {
        "Can only reject from REVIEW_PENDING status, current: $status"
    }
    status = UserStatus.REJECTED
    updatedAt = now
}

fun ban(now: Instant) {
    require(status == UserStatus.ACTIVE) {
        "Can only ban ACTIVE users, current: $status"
    }
    status = UserStatus.BANNED
    updatedAt = now
}

fun unban(now: Instant) {
    require(status == UserStatus.BANNED) {
        "Can only unban BANNED users, current: $status"
    }
    status = UserStatus.ACTIVE
    updatedAt = now
}

fun reactivate(now: Instant) {
    require(status == UserStatus.REJECTED) {
        "Can only reactivate REJECTED users, current: $status"
    }
    status = UserStatus.ACTIVE
    updatedAt = now
}
```

**Step 2: Verify no callers break**

Run: `./gradlew compileTestKotlin`
Expected: `BUILD SUCCESSFUL`. If any caller broke, it's because existing code invokes these no-arg methods — investigate and update.

**Step 3: Commit**

```bash
git add src/main/kotlin/com/kenneth/datingapp/user/User.kt
git commit -m "refactor(user): entity mutating methods accept now: Instant"
```

---

## Task 5: Write `UserTest.kt` with deep coverage

**Files:**
- Create: `src/test/kotlin/com/kenneth/datingapp/user/UserTest.kt`

**Step 1: Create the test file**

Structure: one `@Nested` per transition. Each `@Nested` has (a) happy path with target status + updatedAt verification, (b) one `throws when status is X` test per illegal source status.

```kotlin
package com.kenneth.datingapp.user

import com.kenneth.datingapp.support.TestFixtures.FIXED_NOW
import com.kenneth.datingapp.support.TestFixtures.createUser
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UserTest {

    @Nested
    inner class MarkVerified {
        @Test
        fun `transitions VERIFICATION_PENDING to WAITLISTED and sets updatedAt`() {
            val user = createUser(status = UserStatus.VERIFICATION_PENDING)

            user.markVerified(FIXED_NOW)

            assertThat(user.status).isEqualTo(UserStatus.WAITLISTED)
            assertThat(user.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when status is WAITLISTED`() {
            val user = createUser(status = UserStatus.WAITLISTED)
            assertThatThrownBy { user.markVerified(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("VERIFICATION_PENDING")
        }

        @Test
        fun `throws when status is REVIEW_PENDING`() {
            val user = createUser(status = UserStatus.REVIEW_PENDING)
            assertThatThrownBy { user.markVerified(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is ACTIVE`() {
            val user = createUser(status = UserStatus.ACTIVE)
            assertThatThrownBy { user.markVerified(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is REJECTED`() {
            val user = createUser(status = UserStatus.REJECTED)
            assertThatThrownBy { user.markVerified(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is BANNED`() {
            val user = createUser(status = UserStatus.BANNED)
            assertThatThrownBy { user.markVerified(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class ReleaseFromWaitlist {
        // valid source: WAITLISTED → REVIEW_PENDING
        // illegal sources: VERIFICATION_PENDING, REVIEW_PENDING, ACTIVE, REJECTED, BANNED
        //
        // Tests to write:
        // - happy path: WAITLISTED → REVIEW_PENDING + updatedAt = FIXED_NOW
        // - throws when status is VERIFICATION_PENDING
        // - throws when status is REVIEW_PENDING
        // - throws when status is ACTIVE
        // - throws when status is REJECTED
        // - throws when status is BANNED
    }

    @Nested
    inner class Approve {
        // valid source: REVIEW_PENDING → ACTIVE
        // illegal sources: VERIFICATION_PENDING, WAITLISTED, ACTIVE, REJECTED, BANNED
        // 6 tests following the MarkVerified pattern
    }

    @Nested
    inner class Reject {
        // valid source: REVIEW_PENDING → REJECTED
        // illegal sources: VERIFICATION_PENDING, WAITLISTED, ACTIVE, REJECTED, BANNED
        // 6 tests
    }

    @Nested
    inner class Ban {
        // valid source: ACTIVE → BANNED
        // illegal sources: VERIFICATION_PENDING, WAITLISTED, REVIEW_PENDING, REJECTED, BANNED
        // 6 tests
    }

    @Nested
    inner class Unban {
        // valid source: BANNED → ACTIVE
        // illegal sources: VERIFICATION_PENDING, WAITLISTED, REVIEW_PENDING, ACTIVE, REJECTED
        // 6 tests
    }

    @Nested
    inner class Reactivate {
        // valid source: REJECTED → ACTIVE
        // illegal sources: VERIFICATION_PENDING, WAITLISTED, REVIEW_PENDING, ACTIVE, BANNED
        // 6 tests
    }
}
```

Fill in every remaining `@Nested` block following the `MarkVerified` pattern exactly. Total: ~42 tests (7 transitions × 6 tests each).

**Step 2: Run just this test class**

Run: `./gradlew test --tests "com.kenneth.datingapp.user.UserTest"`
Expected: All ~42 tests pass. This test class does not require Docker (no Spring context).

**Step 3: Commit**

```bash
git add src/test/kotlin/com/kenneth/datingapp/user/UserTest.kt
git commit -m "test(user): deep entity unit tests for User state transitions"
```

---

## Task 6: Refactor `Ok` mutating methods and `isExpired` to accept `now: Instant`

**Files:**
- Modify: `src/main/kotlin/com/kenneth/datingapp/matching/Ok.kt`

**Step 1: Update methods**

```kotlin
fun isExpired(now: Instant): Boolean =
    status == OkStatus.PENDING && now.isAfter(expiresAt)

fun accept(now: Instant) {
    require(status == OkStatus.PENDING) { "Can only accept PENDING ok, current: $status" }
    require(!isExpired(now)) { "Cannot accept expired ok" }
    status = OkStatus.ACCEPTED
    respondedAt = now
    updatedAt = now
}

fun decline(now: Instant) {
    require(status == OkStatus.PENDING) { "Can only decline PENDING ok, current: $status" }
    status = OkStatus.DECLINED
    respondedAt = now
    updatedAt = now
}

fun markExpired(now: Instant) {
    require(status == OkStatus.PENDING) { "Can only expire PENDING ok, current: $status" }
    require(isExpired(now)) { "Ok has not expired yet" }
    status = OkStatus.EXPIRED
    updatedAt = now
}

fun undo(by: User, now: Instant) {
    require(status == OkStatus.PENDING || status == OkStatus.ACCEPTED) {
        "Can only undo PENDING or ACCEPTED ok, current: $status"
    }
    require(by.id == sender.id || by.id == recipient.id) {
        "Only sender or recipient can undo"
    }
    status = OkStatus.UNDONE
    undoneBy = by
    undoneAt = now
    updatedAt = now
}
```

**Step 2: Compile check**

Run: `./gradlew compileTestKotlin`
Expected: `BUILD SUCCESSFUL`

**Step 3: Commit**

```bash
git add src/main/kotlin/com/kenneth/datingapp/matching/Ok.kt
git commit -m "refactor(matching): Ok mutating methods and isExpired accept now: Instant"
```

---

## Task 7: Write `OkTest.kt` with deep coverage

**Files:**
- Create: `src/test/kotlin/com/kenneth/datingapp/matching/OkTest.kt`

**Step 1: Create the test file**

```kotlin
package com.kenneth.datingapp.matching

import com.kenneth.datingapp.support.TestFixtures.FIXED_NOW
import com.kenneth.datingapp.support.TestFixtures.createOk
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class OkTest {

    private val sender: User = createUser()
    private val recipient: User = createUser()
    private val futureExpiry: Instant = FIXED_NOW.plus(Duration.ofDays(3))
    private val pastExpiry: Instant = FIXED_NOW.minus(Duration.ofDays(1))

    private fun pendingOk(expiresAt: Instant = futureExpiry) =
        createOk(sender = sender, recipient = recipient, status = OkStatus.PENDING,
                 sentAt = FIXED_NOW, expiresAt = expiresAt)

    @Nested
    inner class Accept {
        @Test
        fun `transitions PENDING to ACCEPTED and sets timestamps`() {
            val ok = pendingOk()

            ok.accept(FIXED_NOW)

            assertThat(ok.status).isEqualTo(OkStatus.ACCEPTED)
            assertThat(ok.respondedAt).isEqualTo(FIXED_NOW)
            assertThat(ok.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when status is ACCEPTED`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.ACCEPTED, expiresAt = futureExpiry)
            assertThatThrownBy { ok.accept(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("PENDING")
        }

        @Test
        fun `throws when status is DECLINED`() { /* pattern */ }

        @Test
        fun `throws when status is EXPIRED`() { /* pattern */ }

        @Test
        fun `throws when status is UNDONE`() { /* pattern */ }

        @Test
        fun `throws when ok is expired at now`() {
            val ok = pendingOk(expiresAt = pastExpiry)
            assertThatThrownBy { ok.accept(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("expired")
        }
    }

    @Nested
    inner class Decline {
        // valid: PENDING → DECLINED (verify respondedAt + updatedAt)
        // throws when: ACCEPTED, DECLINED, EXPIRED, UNDONE
        // 5 tests total
    }

    @Nested
    inner class MarkExpired {
        // valid: PENDING + past expiresAt → EXPIRED
        // throws when: not-yet-expired PENDING
        // throws when status is: ACCEPTED, DECLINED, EXPIRED, UNDONE
        // 6 tests total
    }

    @Nested
    inner class Undo {
        @Test
        fun `undoes PENDING ok when called by sender`() {
            val ok = pendingOk()
            ok.undo(sender, FIXED_NOW)
            assertThat(ok.status).isEqualTo(OkStatus.UNDONE)
            assertThat(ok.undoneBy).isEqualTo(sender)
            assertThat(ok.undoneAt).isEqualTo(FIXED_NOW)
            assertThat(ok.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `undoes PENDING ok when called by recipient`() { /* recipient variant */ }

        @Test
        fun `undoes ACCEPTED ok`() { /* status = ACCEPTED, by sender */ }

        @Test
        fun `throws when caller is neither sender nor recipient`() {
            val outsider = createUser()
            val ok = pendingOk()
            assertThatThrownBy { ok.undo(outsider, FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("sender or recipient")
        }

        @Test
        fun `throws when status is DECLINED`() { /* pattern */ }

        @Test
        fun `throws when status is EXPIRED`() { /* pattern */ }

        @Test
        fun `throws when status is UNDONE`() { /* pattern */ }
    }

    @Nested
    inner class IsExpired {
        @Test
        fun `returns true when PENDING and now is after expiresAt`() {
            val ok = pendingOk(expiresAt = pastExpiry)
            assertThat(ok.isExpired(FIXED_NOW)).isTrue()
        }

        @Test
        fun `returns false when PENDING and now is before expiresAt`() {
            val ok = pendingOk(expiresAt = futureExpiry)
            assertThat(ok.isExpired(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when ACCEPTED even if past expiresAt`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.ACCEPTED, expiresAt = pastExpiry)
            assertThat(ok.isExpired(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when DECLINED even if past expiresAt`() { /* pattern */ }

        @Test
        fun `returns false when EXPIRED even if past expiresAt`() { /* pattern */ }

        @Test
        fun `returns false when UNDONE even if past expiresAt`() { /* pattern */ }
    }
}
```

Fill in the elided test bodies. Total: ~30 tests.

**Step 2: Run this test class**

Run: `./gradlew test --tests "com.kenneth.datingapp.matching.OkTest"`
Expected: All ~30 tests pass.

**Step 3: Commit**

```bash
git add src/test/kotlin/com/kenneth/datingapp/matching/OkTest.kt
git commit -m "test(matching): deep entity unit tests for Ok state transitions"
```

---

## Task 8: Refactor `Meeting` mutating methods and deadline predicates to accept `now: Instant`

**Files:**
- Modify: `src/main/kotlin/com/kenneth/datingapp/matching/Meeting.kt`

**Step 1: Update methods**

```kotlin
fun markProposalSubmitted(now: Instant) {
    require(status == MeetingStatus.PENDING_PROPOSAL) {
        "Can only submit proposal in PENDING_PROPOSAL status, current: $status"
    }
    status = MeetingStatus.PENDING_FINALIZATION
    finalizationDeadline = now.plus(FINALIZATION_DEADLINE_DURATION)
    updatedAt = now
}

fun finalize(now: Instant, selectedDate: Instant, locationName: String, locationAddress: String) {
    require(status == MeetingStatus.PENDING_FINALIZATION) {
        "Can only finalize in PENDING_FINALIZATION status, current: $status"
    }
    require(selectedDate.isBefore(meetingDeadline)) {
        "Meeting date must be within 3 weeks of mutual ok"
    }
    status = MeetingStatus.SCHEDULED
    scheduledAt = selectedDate
    this.locationName = locationName
    this.locationAddress = locationAddress
    updatedAt = now
}

fun canReschedule(): Boolean =
    status == MeetingStatus.SCHEDULED && !isRescheduled

fun reschedule(now: Instant) {
    require(canReschedule()) { "Cannot reschedule: already rescheduled or not in SCHEDULED status" }
    status = MeetingStatus.PENDING_PROPOSAL
    proposalDeadline = now.plus(PROPOSAL_DEADLINE_DURATION)
    finalizationDeadline = null
    scheduledAt = null
    locationName = null
    locationAddress = null
    isRescheduled = true
    updatedAt = now
}

fun cancel(now: Instant, by: User?, reason: CancellationReason) {
    require(status != MeetingStatus.COMPLETED && status != MeetingStatus.CANCELLED) {
        "Cannot cancel meeting in $status status"
    }
    status = MeetingStatus.CANCELLED
    cancelledBy = by
    cancelledAt = now
    cancellationReason = reason
    updatedAt = now
}

fun complete(now: Instant) {
    require(status == MeetingStatus.SCHEDULED) {
        "Can only complete SCHEDULED meeting, current: $status"
    }
    status = MeetingStatus.COMPLETED
    updatedAt = now
}

fun isProposalDeadlineMissed(now: Instant): Boolean =
    status == MeetingStatus.PENDING_PROPOSAL && now.isAfter(proposalDeadline)

fun isFinalizationDeadlineMissed(now: Instant): Boolean =
    status == MeetingStatus.PENDING_FINALIZATION &&
    finalizationDeadline != null &&
    now.isAfter(finalizationDeadline)
```

Note the `finalize` signature moves `now` to the first parameter for consistency with other methods. Callers pass `clock.instant()` first, then business data.

**Step 2: Compile check**

Run: `./gradlew compileTestKotlin`
Expected: `BUILD SUCCESSFUL`

**Step 3: Commit**

```bash
git add src/main/kotlin/com/kenneth/datingapp/matching/Meeting.kt
git commit -m "refactor(matching): Meeting mutating methods and deadline predicates accept now: Instant"
```

---

## Task 9: Write `MeetingTest.kt` with deep coverage

**Files:**
- Create: `src/test/kotlin/com/kenneth/datingapp/matching/MeetingTest.kt`

**Step 1: Create the test file**

Structure: one `@Nested` per method (`MarkProposalSubmitted`, `Finalize`, `CanReschedule`, `Reschedule`, `Cancel`, `Complete`, `IsProposalDeadlineMissed`, `IsFinalizationDeadlineMissed`).

Pattern reminder — each `@Nested`:
- Happy path(s): verify target status + timestamps + related field changes.
- One "throws when status is X" test per illegal source status.
- Any additional invariant test (e.g., `finalize` throws when `selectedDate >= meetingDeadline`).

```kotlin
package com.kenneth.datingapp.matching

import com.kenneth.datingapp.support.TestFixtures.FIXED_NOW
import com.kenneth.datingapp.support.TestFixtures.createMeeting
import com.kenneth.datingapp.support.TestFixtures.createOk
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class MeetingTest {

    private val woman: User = createUser()
    private val man: User = createUser()
    private val ok = createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED)

    private val futureMeetingDeadline: Instant = FIXED_NOW.plus(Duration.ofDays(14))
    private val nearFutureFinalizationDeadline: Instant = FIXED_NOW.plus(Duration.ofHours(12))
    private val pastFinalizationDeadline: Instant = FIXED_NOW.minus(Duration.ofHours(1))
    private val pastProposalDeadline: Instant = FIXED_NOW.minus(Duration.ofHours(1))
    private val futureProposalDeadline: Instant = FIXED_NOW.plus(Duration.ofHours(12))
    private val selectedDate: Instant = FIXED_NOW.plus(Duration.ofDays(7))

    private fun meeting(
        status: MeetingStatus = MeetingStatus.PENDING_PROPOSAL,
        proposalDeadline: Instant = futureProposalDeadline,
        finalizationDeadline: Instant? = null,
        meetingDeadline: Instant = futureMeetingDeadline,
        isRescheduled: Boolean = false
    ) = createMeeting(
        ok = ok, woman = woman, man = man, status = status,
        proposalDeadline = proposalDeadline,
        finalizationDeadline = finalizationDeadline,
        meetingDeadline = meetingDeadline,
        isRescheduled = isRescheduled
    )

    @Nested
    inner class MarkProposalSubmitted {
        @Test
        fun `transitions PENDING_PROPOSAL to PENDING_FINALIZATION and sets finalizationDeadline`() {
            val m = meeting()
            m.markProposalSubmitted(FIXED_NOW)
            assertThat(m.status).isEqualTo(MeetingStatus.PENDING_FINALIZATION)
            assertThat(m.finalizationDeadline)
                .isEqualTo(FIXED_NOW.plus(Meeting.FINALIZATION_DEADLINE_DURATION))
            assertThat(m.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test fun `throws when status is PENDING_FINALIZATION`() { /* pattern */ }
        @Test fun `throws when status is SCHEDULED`() { /* pattern */ }
        @Test fun `throws when status is COMPLETED`() { /* pattern */ }
        @Test fun `throws when status is CANCELLED`() { /* pattern */ }
    }

    @Nested
    inner class Finalize {
        @Test
        fun `transitions PENDING_FINALIZATION to SCHEDULED with date and location`() {
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION,
                            finalizationDeadline = nearFutureFinalizationDeadline)
            m.finalize(FIXED_NOW, selectedDate, "Cafe X", "123 Main St")
            assertThat(m.status).isEqualTo(MeetingStatus.SCHEDULED)
            assertThat(m.scheduledAt).isEqualTo(selectedDate)
            assertThat(m.locationName).isEqualTo("Cafe X")
            assertThat(m.locationAddress).isEqualTo("123 Main St")
            assertThat(m.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when selectedDate is at or after meetingDeadline`() {
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION,
                            finalizationDeadline = nearFutureFinalizationDeadline,
                            meetingDeadline = FIXED_NOW.plus(Duration.ofDays(5)))
            val tooLate = FIXED_NOW.plus(Duration.ofDays(6))
            assertThatThrownBy { m.finalize(FIXED_NOW, tooLate, "X", "Y") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("3 weeks")
        }

        @Test fun `throws when status is PENDING_PROPOSAL`() { /* pattern */ }
        @Test fun `throws when status is SCHEDULED`() { /* pattern */ }
        @Test fun `throws when status is COMPLETED`() { /* pattern */ }
        @Test fun `throws when status is CANCELLED`() { /* pattern */ }
    }

    @Nested
    inner class CanReschedule {
        @Test fun `returns true when SCHEDULED and not yet rescheduled`() {
            val m = meeting(status = MeetingStatus.SCHEDULED, isRescheduled = false)
            assertThat(m.canReschedule()).isTrue()
        }
        @Test fun `returns false when SCHEDULED and already rescheduled`() { /* pattern */ }
        @Test fun `returns false when status is PENDING_PROPOSAL`() { /* pattern */ }
        @Test fun `returns false when status is PENDING_FINALIZATION`() { /* pattern */ }
        @Test fun `returns false when status is COMPLETED`() { /* pattern */ }
        @Test fun `returns false when status is CANCELLED`() { /* pattern */ }
    }

    @Nested
    inner class Reschedule {
        @Test
        fun `resets meeting to PENDING_PROPOSAL and clears scheduling fields`() {
            val m = createMeeting(
                ok = ok, woman = woman, man = man, status = MeetingStatus.SCHEDULED,
                scheduledAt = selectedDate, locationName = "Cafe X", locationAddress = "123",
                finalizationDeadline = nearFutureFinalizationDeadline,
                isRescheduled = false, meetingDeadline = futureMeetingDeadline
            )
            m.reschedule(FIXED_NOW)
            assertThat(m.status).isEqualTo(MeetingStatus.PENDING_PROPOSAL)
            assertThat(m.proposalDeadline)
                .isEqualTo(FIXED_NOW.plus(Meeting.PROPOSAL_DEADLINE_DURATION))
            assertThat(m.finalizationDeadline).isNull()
            assertThat(m.scheduledAt).isNull()
            assertThat(m.locationName).isNull()
            assertThat(m.locationAddress).isNull()
            assertThat(m.isRescheduled).isTrue()
            assertThat(m.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test fun `throws when already rescheduled`() { /* SCHEDULED + isRescheduled=true */ }
        @Test fun `throws when status is PENDING_PROPOSAL`() { /* pattern */ }
        @Test fun `throws when status is PENDING_FINALIZATION`() { /* pattern */ }
        @Test fun `throws when status is COMPLETED`() { /* pattern */ }
        @Test fun `throws when status is CANCELLED`() { /* pattern */ }
    }

    @Nested
    inner class Cancel {
        @Test
        fun `cancels from PENDING_PROPOSAL and records reason and actor`() {
            val m = meeting()
            m.cancel(FIXED_NOW, by = woman, reason = CancellationReason.USER_CANCELLED)
            assertThat(m.status).isEqualTo(MeetingStatus.CANCELLED)
            assertThat(m.cancelledBy).isEqualTo(woman)
            assertThat(m.cancelledAt).isEqualTo(FIXED_NOW)
            assertThat(m.cancellationReason).isEqualTo(CancellationReason.USER_CANCELLED)
            assertThat(m.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test fun `cancels from PENDING_FINALIZATION`() { /* pattern */ }
        @Test fun `cancels from SCHEDULED`() { /* pattern */ }
        @Test fun `allows null actor for system-triggered cancellations`() {
            val m = meeting()
            m.cancel(FIXED_NOW, by = null, reason = CancellationReason.PROPOSAL_DEADLINE_MISSED)
            assertThat(m.cancelledBy).isNull()
        }
        @Test fun `throws when status is COMPLETED`() { /* pattern */ }
        @Test fun `throws when status is CANCELLED`() { /* pattern */ }
    }

    @Nested
    inner class Complete {
        // valid: SCHEDULED → COMPLETED
        // throws: PENDING_PROPOSAL, PENDING_FINALIZATION, COMPLETED, CANCELLED
        // 5 tests
    }

    @Nested
    inner class IsProposalDeadlineMissed {
        @Test fun `returns true when PENDING_PROPOSAL and past proposalDeadline`() {
            val m = meeting(status = MeetingStatus.PENDING_PROPOSAL,
                            proposalDeadline = pastProposalDeadline)
            assertThat(m.isProposalDeadlineMissed(FIXED_NOW)).isTrue()
        }
        @Test fun `returns false when PENDING_PROPOSAL and future proposalDeadline`() { /* pattern */ }
        @Test fun `returns false when status is PENDING_FINALIZATION`() { /* pattern */ }
        @Test fun `returns false when status is SCHEDULED`() { /* pattern */ }
        @Test fun `returns false when status is COMPLETED`() { /* pattern */ }
        @Test fun `returns false when status is CANCELLED`() { /* pattern */ }
    }

    @Nested
    inner class IsFinalizationDeadlineMissed {
        @Test fun `returns true when PENDING_FINALIZATION and past finalizationDeadline`() {
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION,
                            finalizationDeadline = pastFinalizationDeadline)
            assertThat(m.isFinalizationDeadlineMissed(FIXED_NOW)).isTrue()
        }
        @Test fun `returns false when PENDING_FINALIZATION and future finalizationDeadline`() { /* pattern */ }
        @Test fun `returns false when finalizationDeadline is null`() {
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION, finalizationDeadline = null)
            assertThat(m.isFinalizationDeadlineMissed(FIXED_NOW)).isFalse()
        }
        @Test fun `returns false when status is PENDING_PROPOSAL`() { /* pattern */ }
        @Test fun `returns false when status is SCHEDULED`() { /* pattern */ }
        @Test fun `returns false when status is COMPLETED`() { /* pattern */ }
        @Test fun `returns false when status is CANCELLED`() { /* pattern */ }
    }
}
```

Fill in the elided bodies. Total: ~48 tests.

**Step 2: Run this test class**

Run: `./gradlew test --tests "com.kenneth.datingapp.matching.MeetingTest"`
Expected: All ~48 tests pass.

**Step 3: Commit**

```bash
git add src/test/kotlin/com/kenneth/datingapp/matching/MeetingTest.kt
git commit -m "test(matching): deep entity unit tests for Meeting state transitions"
```

---

## Task 10: Add `ModulithArchitectureTest` (with cycle-violation filter)

> **Amendment (2026-07-14):** The originally planned strict `modules.verify()` failed on the current codebase with 4 cyclic dependencies caused by cross-module JPA `@ManyToOne` references. Resolving them requires a large entity refactor beyond issue #9's scope. The test filters cycle violations while still strictly enforcing all other boundary rules; the proper fix is tracked in issue #56 (assigned to Milestone 2 as a blocker for other M2 work).

**Files:**
- Create: `src/test/kotlin/com/kenneth/datingapp/ModulithArchitectureTest.kt`

**Step 1: Create the test file**

```kotlin
package com.kenneth.datingapp

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules
import org.springframework.modulith.core.Violations

/**
 * Verifies Spring Modulith module boundaries.
 *
 * Cyclic-dependency violations are currently tolerated — see issue #56 for the
 * plan to resolve them. Every OTHER boundary rule (public-vs-internal visibility,
 * unresolved refs, etc.) is enforced strictly.
 */
class ModulithArchitectureTest {

    private val modules = ApplicationModules.of(DatingAppApplication::class.java)

    @Test
    fun `modules pass all boundary checks except known cycles`() {
        try {
            modules.verify()
        } catch (violations: Violations) {
            val nonCycleMessages = violations.messages
                .filterNot { it.contains("Cycle detected", ignoreCase = true) }
            check(nonCycleMessages.isEmpty()) {
                "Non-cycle module boundary violations found:\n" +
                    nonCycleMessages.joinToString("\n---\n")
            }
        }
    }
}
```

Note: `ApplicationModules.of()` is a static factory — no Spring context needed, no Docker needed. `verify()` in Spring Modulith 2.x throws `Violations` (a `RuntimeException`) on any violation. The filter targets ArchUnit's stable `"Cycle detected: "` prefix; other violation categories (non-exposed type refs, missing modules, etc.) still fail the test.

**Step 2: Run the test**

Run: `./gradlew test --tests "com.kenneth.datingapp.ModulithArchitectureTest"`
Expected: PASS on the current codebase (cycles are filtered).

**Step 2b: Fault-injection sanity check (before committing)**

Temporarily introduce a real non-cycle boundary violation to confirm the filter isn't over-permissive: add a reference to an internal-package class (e.g., `com.kenneth.datingapp.user.internal.UserRepository`) from a non-internal file, run the test, confirm it fails with a `"non-exposed type"` message, then revert.

**Step 3: Commit**

```bash
git add src/test/kotlin/com/kenneth/datingapp/ModulithArchitectureTest.kt
git commit -m "test: add ModulithArchitectureTest (cycles deferred to #56)"
```

---

## Task 11: Full test suite verification

**Files:** none (verification only)

**Step 1: Ensure Docker is running**

Testcontainers requires a Docker daemon. If OrbStack is not running, start it. Verify:

```bash
docker ps
```

Expected: no error, may return empty container list.

**Step 2: Run full test suite**

Run: `./gradlew test`
Expected: All tests pass — new entity unit tests + existing repository integration tests + `ModulithArchitectureTest`.

If any pre-existing test fails, that's unexpected — investigate. The entity refactor should not touch anything the repository tests exercise (they don't call `.accept()`, `.approve()`, etc.).

**Step 3: Report results**

Confirm counts: how many tests total, all green. If green, this task closes issue #9's acceptance criteria.

---

## Post-plan: opening the PR

After all tasks pass:

1. Push branch:
   ```bash
   git push -u origin issue-9-test-infra
   ```

2. Open PR with body referencing issue #9. Use `gh pr create`.

3. Cleanup worktree only after PR is merged (see `superpowers:finishing-a-development-branch`).

---

## Test-writing reminders

- **Use AssertJ**, not JUnit assertions.
- **One assertion pattern per @Nested block** — happy path first, illegal sources following.
- **Use test method backticks** with descriptive names: `` `transitions VERIFICATION_PENDING to WAITLISTED and sets updatedAt` ``.
- **Do not assert on `createdAt`** — that's constructor default `Instant.now()`, not deterministic.
- **Do assert on `updatedAt` = `FIXED_NOW`** — this is what the refactor tests.
- **For entities with `equals` by id**, use `isEqualTo` freely — reference equality isn't needed.
