package com.kenneth.datingapp.feedback

import com.kenneth.datingapp.matching.OkStatus
import com.kenneth.datingapp.support.TestFixtures.FIXED_NOW
import com.kenneth.datingapp.support.TestFixtures.createDispute
import com.kenneth.datingapp.support.TestFixtures.createFeedback
import com.kenneth.datingapp.support.TestFixtures.createMeeting
import com.kenneth.datingapp.support.TestFixtures.createOk
import com.kenneth.datingapp.support.TestFixtures.createUser
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

class DisputeTest {

    private val adminId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val laterNow = FIXED_NOW.plus(Duration.ofHours(2))

    private fun dispute(status: DisputeStatus = DisputeStatus.OPEN): Dispute {
        val woman = createUser()
        val man = createUser()
        val ok = createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED)
        val meeting = createMeeting(ok = ok, woman = woman, man = man)
        val feedback = createFeedback(meeting = meeting, submittedBy = woman, aboutUser = man,
                                       meetingHappened = false, noShowReason = NoShowReason.THEY_DID_NOT_SHOW)
        return createDispute(meeting = meeting, feedback = feedback,
                             reportedBy = woman, reportedUser = man, status = status)
    }

    @Nested
    inner class StartInvestigation {
        @Test
        fun `transitions OPEN to INVESTIGATING and sets updatedAt`() {
            val d = dispute(status = DisputeStatus.OPEN)

            d.startInvestigation(FIXED_NOW)

            assertThat(d.status).isEqualTo(DisputeStatus.INVESTIGATING)
            assertThat(d.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when status is INVESTIGATING`() {
            val d = dispute(status = DisputeStatus.INVESTIGATING)
            assertThatThrownBy { d.startInvestigation(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("OPEN")
        }

        @Test
        fun `throws when status is RESOLVED`() {
            val d = dispute(status = DisputeStatus.RESOLVED)
            assertThatThrownBy { d.startInvestigation(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("OPEN")
        }
    }

    @Nested
    inner class Resolve {
        @Test
        fun `transitions OPEN to RESOLVED and records admin, resolution, notes, timestamps`() {
            val d = dispute(status = DisputeStatus.OPEN)

            d.resolve(adminId, DisputeResolution.WARNING_ISSUED, FIXED_NOW, notes = "user warned")

            assertThat(d.status).isEqualTo(DisputeStatus.RESOLVED)
            assertThat(d.resolution).isEqualTo(DisputeResolution.WARNING_ISSUED)
            assertThat(d.resolutionNotes).isEqualTo("user warned")
            assertThat(d.resolvedByAdminId).isEqualTo(adminId)
            assertThat(d.resolvedAt).isEqualTo(FIXED_NOW)
            assertThat(d.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `transitions INVESTIGATING to RESOLVED`() {
            val d = dispute(status = DisputeStatus.INVESTIGATING)

            d.resolve(adminId, DisputeResolution.NO_ACTION, FIXED_NOW)

            assertThat(d.status).isEqualTo(DisputeStatus.RESOLVED)
            assertThat(d.resolution).isEqualTo(DisputeResolution.NO_ACTION)
            assertThat(d.resolvedByAdminId).isEqualTo(adminId)
        }

        @Test
        fun `throws when status is already RESOLVED`() {
            val d = dispute(status = DisputeStatus.RESOLVED)
            assertThatThrownBy { d.resolve(adminId, DisputeResolution.NO_ACTION, FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("already resolved")
        }
    }

    @Nested
    inner class Reopen {
        @Test
        fun `transitions RESOLVED to OPEN and clears resolution state`() {
            val d = dispute(status = DisputeStatus.OPEN)
            d.resolve(adminId, DisputeResolution.WARNING_ISSUED, FIXED_NOW, notes = "user warned")

            d.reopen(laterNow)

            assertThat(d.status).isEqualTo(DisputeStatus.OPEN)
            assertThat(d.resolution).isNull()
            assertThat(d.resolutionNotes).isNull()
            assertThat(d.resolvedByAdminId).isNull()
            assertThat(d.resolvedAt).isNull()
            assertThat(d.updatedAt).isEqualTo(laterNow)
        }

        @Test
        fun `throws when status is OPEN`() {
            val d = dispute(status = DisputeStatus.OPEN)
            assertThatThrownBy { d.reopen(laterNow) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("RESOLVED")
        }

        @Test
        fun `throws when status is INVESTIGATING`() {
            val d = dispute(status = DisputeStatus.INVESTIGATING)
            assertThatThrownBy { d.reopen(laterNow) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("RESOLVED")
        }
    }
}
