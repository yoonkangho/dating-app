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

        @Test
        fun `throws when status is PENDING_FINALIZATION`() {
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION,
                            finalizationDeadline = nearFutureFinalizationDeadline)
            assertThatThrownBy { m.markProposalSubmitted(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("PENDING_PROPOSAL")
        }

        @Test
        fun `throws when status is SCHEDULED`() {
            val m = meeting(status = MeetingStatus.SCHEDULED)
            assertThatThrownBy { m.markProposalSubmitted(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is COMPLETED`() {
            val m = meeting(status = MeetingStatus.COMPLETED)
            assertThatThrownBy { m.markProposalSubmitted(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is CANCELLED`() {
            val m = meeting(status = MeetingStatus.CANCELLED)
            assertThatThrownBy { m.markProposalSubmitted(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
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

        @Test
        fun `throws when selectedDate equals meetingDeadline`() {
            val deadline = FIXED_NOW.plus(Duration.ofDays(5))
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION,
                            finalizationDeadline = nearFutureFinalizationDeadline,
                            meetingDeadline = deadline)
            assertThatThrownBy { m.finalize(FIXED_NOW, deadline, "X", "Y") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is PENDING_PROPOSAL`() {
            val m = meeting(status = MeetingStatus.PENDING_PROPOSAL)
            assertThatThrownBy { m.finalize(FIXED_NOW, selectedDate, "X", "Y") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("PENDING_FINALIZATION")
        }

        @Test
        fun `throws when status is SCHEDULED`() {
            val m = meeting(status = MeetingStatus.SCHEDULED)
            assertThatThrownBy { m.finalize(FIXED_NOW, selectedDate, "X", "Y") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is COMPLETED`() {
            val m = meeting(status = MeetingStatus.COMPLETED)
            assertThatThrownBy { m.finalize(FIXED_NOW, selectedDate, "X", "Y") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is CANCELLED`() {
            val m = meeting(status = MeetingStatus.CANCELLED)
            assertThatThrownBy { m.finalize(FIXED_NOW, selectedDate, "X", "Y") }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class CanReschedule {
        @Test
        fun `returns true when SCHEDULED and not yet rescheduled`() {
            val m = meeting(status = MeetingStatus.SCHEDULED, isRescheduled = false)
            assertThat(m.canReschedule()).isTrue()
        }

        @Test
        fun `returns false when SCHEDULED and already rescheduled`() {
            val m = meeting(status = MeetingStatus.SCHEDULED, isRescheduled = true)
            assertThat(m.canReschedule()).isFalse()
        }

        @Test
        fun `returns false when status is PENDING_PROPOSAL`() {
            val m = meeting(status = MeetingStatus.PENDING_PROPOSAL)
            assertThat(m.canReschedule()).isFalse()
        }

        @Test
        fun `returns false when status is PENDING_FINALIZATION`() {
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION,
                            finalizationDeadline = nearFutureFinalizationDeadline)
            assertThat(m.canReschedule()).isFalse()
        }

        @Test
        fun `returns false when status is COMPLETED`() {
            val m = meeting(status = MeetingStatus.COMPLETED)
            assertThat(m.canReschedule()).isFalse()
        }

        @Test
        fun `returns false when status is CANCELLED`() {
            val m = meeting(status = MeetingStatus.CANCELLED)
            assertThat(m.canReschedule()).isFalse()
        }
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

        @Test
        fun `throws when already rescheduled`() {
            val m = meeting(status = MeetingStatus.SCHEDULED, isRescheduled = true)
            assertThatThrownBy { m.reschedule(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("rescheduled")
        }

        @Test
        fun `throws when status is PENDING_PROPOSAL`() {
            val m = meeting(status = MeetingStatus.PENDING_PROPOSAL)
            assertThatThrownBy { m.reschedule(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is PENDING_FINALIZATION`() {
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION,
                            finalizationDeadline = nearFutureFinalizationDeadline)
            assertThatThrownBy { m.reschedule(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is COMPLETED`() {
            val m = meeting(status = MeetingStatus.COMPLETED)
            assertThatThrownBy { m.reschedule(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is CANCELLED`() {
            val m = meeting(status = MeetingStatus.CANCELLED)
            assertThatThrownBy { m.reschedule(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
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

        @Test
        fun `cancels from PENDING_FINALIZATION`() {
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION,
                            finalizationDeadline = nearFutureFinalizationDeadline)
            m.cancel(FIXED_NOW, by = man, reason = CancellationReason.USER_CANCELLED)
            assertThat(m.status).isEqualTo(MeetingStatus.CANCELLED)
            assertThat(m.cancelledBy).isEqualTo(man)
            assertThat(m.cancelledAt).isEqualTo(FIXED_NOW)
            assertThat(m.cancellationReason).isEqualTo(CancellationReason.USER_CANCELLED)
            assertThat(m.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `cancels from SCHEDULED`() {
            val m = meeting(status = MeetingStatus.SCHEDULED)
            m.cancel(FIXED_NOW, by = woman, reason = CancellationReason.USER_CANCELLED)
            assertThat(m.status).isEqualTo(MeetingStatus.CANCELLED)
            assertThat(m.cancelledBy).isEqualTo(woman)
            assertThat(m.cancelledAt).isEqualTo(FIXED_NOW)
            assertThat(m.cancellationReason).isEqualTo(CancellationReason.USER_CANCELLED)
            assertThat(m.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `allows null actor for system-triggered cancellations`() {
            val m = meeting()
            m.cancel(FIXED_NOW, by = null, reason = CancellationReason.PROPOSAL_DEADLINE_MISSED)
            assertThat(m.cancelledBy).isNull()
        }

        @Test
        fun `throws when status is COMPLETED`() {
            val m = meeting(status = MeetingStatus.COMPLETED)
            assertThatThrownBy {
                m.cancel(FIXED_NOW, by = woman, reason = CancellationReason.USER_CANCELLED)
            }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("cancel")
        }

        @Test
        fun `throws when status is CANCELLED`() {
            val m = meeting(status = MeetingStatus.CANCELLED)
            assertThatThrownBy {
                m.cancel(FIXED_NOW, by = woman, reason = CancellationReason.USER_CANCELLED)
            }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class Complete {
        @Test
        fun `transitions SCHEDULED to COMPLETED and sets updatedAt`() {
            val m = meeting(status = MeetingStatus.SCHEDULED)
            m.complete(FIXED_NOW)
            assertThat(m.status).isEqualTo(MeetingStatus.COMPLETED)
            assertThat(m.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when status is PENDING_PROPOSAL`() {
            val m = meeting(status = MeetingStatus.PENDING_PROPOSAL)
            assertThatThrownBy { m.complete(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("SCHEDULED")
        }

        @Test
        fun `throws when status is PENDING_FINALIZATION`() {
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION,
                            finalizationDeadline = nearFutureFinalizationDeadline)
            assertThatThrownBy { m.complete(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is COMPLETED`() {
            val m = meeting(status = MeetingStatus.COMPLETED)
            assertThatThrownBy { m.complete(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is CANCELLED`() {
            val m = meeting(status = MeetingStatus.CANCELLED)
            assertThatThrownBy { m.complete(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class IsProposalDeadlineMissed {
        @Test
        fun `returns true when PENDING_PROPOSAL and past proposalDeadline`() {
            val m = meeting(status = MeetingStatus.PENDING_PROPOSAL,
                            proposalDeadline = pastProposalDeadline)
            assertThat(m.isProposalDeadlineMissed(FIXED_NOW)).isTrue()
        }

        @Test
        fun `returns false when PENDING_PROPOSAL and future proposalDeadline`() {
            val m = meeting(status = MeetingStatus.PENDING_PROPOSAL,
                            proposalDeadline = futureProposalDeadline)
            assertThat(m.isProposalDeadlineMissed(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when status is PENDING_FINALIZATION`() {
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION,
                            proposalDeadline = pastProposalDeadline,
                            finalizationDeadline = nearFutureFinalizationDeadline)
            assertThat(m.isProposalDeadlineMissed(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when status is SCHEDULED`() {
            val m = meeting(status = MeetingStatus.SCHEDULED,
                            proposalDeadline = pastProposalDeadline)
            assertThat(m.isProposalDeadlineMissed(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when status is COMPLETED`() {
            val m = meeting(status = MeetingStatus.COMPLETED,
                            proposalDeadline = pastProposalDeadline)
            assertThat(m.isProposalDeadlineMissed(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when status is CANCELLED`() {
            val m = meeting(status = MeetingStatus.CANCELLED,
                            proposalDeadline = pastProposalDeadline)
            assertThat(m.isProposalDeadlineMissed(FIXED_NOW)).isFalse()
        }
    }

    @Nested
    inner class IsFinalizationDeadlineMissed {
        @Test
        fun `returns true when PENDING_FINALIZATION and past finalizationDeadline`() {
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION,
                            finalizationDeadline = pastFinalizationDeadline)
            assertThat(m.isFinalizationDeadlineMissed(FIXED_NOW)).isTrue()
        }

        @Test
        fun `returns false when PENDING_FINALIZATION and future finalizationDeadline`() {
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION,
                            finalizationDeadline = nearFutureFinalizationDeadline)
            assertThat(m.isFinalizationDeadlineMissed(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when finalizationDeadline is null`() {
            val m = meeting(status = MeetingStatus.PENDING_FINALIZATION, finalizationDeadline = null)
            assertThat(m.isFinalizationDeadlineMissed(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when status is PENDING_PROPOSAL`() {
            val m = meeting(status = MeetingStatus.PENDING_PROPOSAL,
                            finalizationDeadline = pastFinalizationDeadline)
            assertThat(m.isFinalizationDeadlineMissed(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when status is SCHEDULED`() {
            val m = meeting(status = MeetingStatus.SCHEDULED,
                            finalizationDeadline = pastFinalizationDeadline)
            assertThat(m.isFinalizationDeadlineMissed(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when status is COMPLETED`() {
            val m = meeting(status = MeetingStatus.COMPLETED,
                            finalizationDeadline = pastFinalizationDeadline)
            assertThat(m.isFinalizationDeadlineMissed(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when status is CANCELLED`() {
            val m = meeting(status = MeetingStatus.CANCELLED,
                            finalizationDeadline = pastFinalizationDeadline)
            assertThat(m.isFinalizationDeadlineMissed(FIXED_NOW)).isFalse()
        }
    }
}
