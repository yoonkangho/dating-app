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
        @Test
        fun `transitions WAITLISTED to REVIEW_PENDING and sets updatedAt`() {
            val user = createUser(status = UserStatus.WAITLISTED)

            user.releaseFromWaitlist(FIXED_NOW)

            assertThat(user.status).isEqualTo(UserStatus.REVIEW_PENDING)
            assertThat(user.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when status is VERIFICATION_PENDING`() {
            val user = createUser(status = UserStatus.VERIFICATION_PENDING)
            assertThatThrownBy { user.releaseFromWaitlist(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("WAITLISTED")
        }

        @Test
        fun `throws when status is REVIEW_PENDING`() {
            val user = createUser(status = UserStatus.REVIEW_PENDING)
            assertThatThrownBy { user.releaseFromWaitlist(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is ACTIVE`() {
            val user = createUser(status = UserStatus.ACTIVE)
            assertThatThrownBy { user.releaseFromWaitlist(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is REJECTED`() {
            val user = createUser(status = UserStatus.REJECTED)
            assertThatThrownBy { user.releaseFromWaitlist(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is BANNED`() {
            val user = createUser(status = UserStatus.BANNED)
            assertThatThrownBy { user.releaseFromWaitlist(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class Approve {
        @Test
        fun `transitions REVIEW_PENDING to ACTIVE and sets updatedAt`() {
            val user = createUser(status = UserStatus.REVIEW_PENDING)

            user.approve(FIXED_NOW)

            assertThat(user.status).isEqualTo(UserStatus.ACTIVE)
            assertThat(user.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when status is VERIFICATION_PENDING`() {
            val user = createUser(status = UserStatus.VERIFICATION_PENDING)
            assertThatThrownBy { user.approve(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("REVIEW_PENDING")
        }

        @Test
        fun `throws when status is WAITLISTED`() {
            val user = createUser(status = UserStatus.WAITLISTED)
            assertThatThrownBy { user.approve(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is ACTIVE`() {
            val user = createUser(status = UserStatus.ACTIVE)
            assertThatThrownBy { user.approve(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is REJECTED`() {
            val user = createUser(status = UserStatus.REJECTED)
            assertThatThrownBy { user.approve(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is BANNED`() {
            val user = createUser(status = UserStatus.BANNED)
            assertThatThrownBy { user.approve(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class Reject {
        @Test
        fun `transitions REVIEW_PENDING to REJECTED and sets updatedAt`() {
            val user = createUser(status = UserStatus.REVIEW_PENDING)

            user.reject(FIXED_NOW)

            assertThat(user.status).isEqualTo(UserStatus.REJECTED)
            assertThat(user.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when status is VERIFICATION_PENDING`() {
            val user = createUser(status = UserStatus.VERIFICATION_PENDING)
            assertThatThrownBy { user.reject(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("REVIEW_PENDING")
        }

        @Test
        fun `throws when status is WAITLISTED`() {
            val user = createUser(status = UserStatus.WAITLISTED)
            assertThatThrownBy { user.reject(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is ACTIVE`() {
            val user = createUser(status = UserStatus.ACTIVE)
            assertThatThrownBy { user.reject(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is REJECTED`() {
            val user = createUser(status = UserStatus.REJECTED)
            assertThatThrownBy { user.reject(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is BANNED`() {
            val user = createUser(status = UserStatus.BANNED)
            assertThatThrownBy { user.reject(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class Ban {
        @Test
        fun `transitions ACTIVE to BANNED and sets updatedAt`() {
            val user = createUser(status = UserStatus.ACTIVE)

            user.ban(FIXED_NOW)

            assertThat(user.status).isEqualTo(UserStatus.BANNED)
            assertThat(user.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when status is VERIFICATION_PENDING`() {
            val user = createUser(status = UserStatus.VERIFICATION_PENDING)
            assertThatThrownBy { user.ban(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("ACTIVE")
        }

        @Test
        fun `throws when status is WAITLISTED`() {
            val user = createUser(status = UserStatus.WAITLISTED)
            assertThatThrownBy { user.ban(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is REVIEW_PENDING`() {
            val user = createUser(status = UserStatus.REVIEW_PENDING)
            assertThatThrownBy { user.ban(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is REJECTED`() {
            val user = createUser(status = UserStatus.REJECTED)
            assertThatThrownBy { user.ban(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is BANNED`() {
            val user = createUser(status = UserStatus.BANNED)
            assertThatThrownBy { user.ban(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class Unban {
        @Test
        fun `transitions BANNED to ACTIVE and sets updatedAt`() {
            val user = createUser(status = UserStatus.BANNED)

            user.unban(FIXED_NOW)

            assertThat(user.status).isEqualTo(UserStatus.ACTIVE)
            assertThat(user.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when status is VERIFICATION_PENDING`() {
            val user = createUser(status = UserStatus.VERIFICATION_PENDING)
            assertThatThrownBy { user.unban(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("BANNED")
        }

        @Test
        fun `throws when status is WAITLISTED`() {
            val user = createUser(status = UserStatus.WAITLISTED)
            assertThatThrownBy { user.unban(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is REVIEW_PENDING`() {
            val user = createUser(status = UserStatus.REVIEW_PENDING)
            assertThatThrownBy { user.unban(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is ACTIVE`() {
            val user = createUser(status = UserStatus.ACTIVE)
            assertThatThrownBy { user.unban(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is REJECTED`() {
            val user = createUser(status = UserStatus.REJECTED)
            assertThatThrownBy { user.unban(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class Reactivate {
        @Test
        fun `transitions REJECTED to ACTIVE and sets updatedAt`() {
            val user = createUser(status = UserStatus.REJECTED)

            user.reactivate(FIXED_NOW)

            assertThat(user.status).isEqualTo(UserStatus.ACTIVE)
            assertThat(user.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when status is VERIFICATION_PENDING`() {
            val user = createUser(status = UserStatus.VERIFICATION_PENDING)
            assertThatThrownBy { user.reactivate(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("REJECTED")
        }

        @Test
        fun `throws when status is WAITLISTED`() {
            val user = createUser(status = UserStatus.WAITLISTED)
            assertThatThrownBy { user.reactivate(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is REVIEW_PENDING`() {
            val user = createUser(status = UserStatus.REVIEW_PENDING)
            assertThatThrownBy { user.reactivate(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is ACTIVE`() {
            val user = createUser(status = UserStatus.ACTIVE)
            assertThatThrownBy { user.reactivate(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is BANNED`() {
            val user = createUser(status = UserStatus.BANNED)
            assertThatThrownBy { user.reactivate(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
