package com.kenneth.datingapp.user

import com.kenneth.datingapp.support.TestFixtures.FIXED_NOW
import com.kenneth.datingapp.support.TestFixtures.createApprovalRequest
import com.kenneth.datingapp.support.TestFixtures.createUser
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

class ApprovalRequestTest {

    private val adminId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Nested
    inner class Approve {
        @Test
        fun `transitions PENDING to APPROVED and records admin, timestamp, and notes`() {
            val request = createApprovalRequest(user = createUser(), status = ApprovalRequestStatus.PENDING)

            request.approve(adminId, FIXED_NOW, notes = "looks good")

            assertThat(request.status).isEqualTo(ApprovalRequestStatus.APPROVED)
            assertThat(request.reviewedByAdminId).isEqualTo(adminId)
            assertThat(request.reviewedAt).isEqualTo(FIXED_NOW)
            assertThat(request.reviewNotes).isEqualTo("looks good")
            assertThat(request.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when status is APPROVED`() {
            val request = createApprovalRequest(user = createUser(), status = ApprovalRequestStatus.APPROVED)
            assertThatThrownBy { request.approve(adminId, FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("PENDING")
        }

        @Test
        fun `throws when status is REJECTED`() {
            val request = createApprovalRequest(user = createUser(), status = ApprovalRequestStatus.REJECTED)
            assertThatThrownBy { request.approve(adminId, FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("PENDING")
        }
    }

    @Nested
    inner class Reject {
        @Test
        fun `transitions PENDING to REJECTED and records admin, timestamp, and notes`() {
            val request = createApprovalRequest(user = createUser(), status = ApprovalRequestStatus.PENDING)

            request.reject(adminId, FIXED_NOW, notes = "photo unclear")

            assertThat(request.status).isEqualTo(ApprovalRequestStatus.REJECTED)
            assertThat(request.reviewedByAdminId).isEqualTo(adminId)
            assertThat(request.reviewedAt).isEqualTo(FIXED_NOW)
            assertThat(request.reviewNotes).isEqualTo("photo unclear")
            assertThat(request.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when status is APPROVED`() {
            val request = createApprovalRequest(user = createUser(), status = ApprovalRequestStatus.APPROVED)
            assertThatThrownBy { request.reject(adminId, FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("PENDING")
        }

        @Test
        fun `throws when status is REJECTED`() {
            val request = createApprovalRequest(user = createUser(), status = ApprovalRequestStatus.REJECTED)
            assertThatThrownBy { request.reject(adminId, FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("PENDING")
        }
    }
}
