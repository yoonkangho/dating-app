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
        fun `throws when status is DECLINED`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.DECLINED, expiresAt = futureExpiry)
            assertThatThrownBy { ok.accept(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is EXPIRED`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.EXPIRED, expiresAt = futureExpiry)
            assertThatThrownBy { ok.accept(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is UNDONE`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.UNDONE, expiresAt = futureExpiry)
            assertThatThrownBy { ok.accept(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

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
        @Test
        fun `transitions PENDING to DECLINED and sets timestamps`() {
            val ok = pendingOk()

            ok.decline(FIXED_NOW)

            assertThat(ok.status).isEqualTo(OkStatus.DECLINED)
            assertThat(ok.respondedAt).isEqualTo(FIXED_NOW)
            assertThat(ok.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when status is ACCEPTED`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.ACCEPTED, expiresAt = futureExpiry)
            assertThatThrownBy { ok.decline(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("PENDING")
        }

        @Test
        fun `throws when status is DECLINED`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.DECLINED, expiresAt = futureExpiry)
            assertThatThrownBy { ok.decline(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is EXPIRED`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.EXPIRED, expiresAt = futureExpiry)
            assertThatThrownBy { ok.decline(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is UNDONE`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.UNDONE, expiresAt = futureExpiry)
            assertThatThrownBy { ok.decline(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    inner class MarkExpired {
        @Test
        fun `transitions PENDING to EXPIRED and sets updatedAt when past expiresAt`() {
            val ok = pendingOk(expiresAt = pastExpiry)

            ok.markExpired(FIXED_NOW)

            assertThat(ok.status).isEqualTo(OkStatus.EXPIRED)
            assertThat(ok.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when PENDING but not yet expired`() {
            val ok = pendingOk(expiresAt = futureExpiry)
            assertThatThrownBy { ok.markExpired(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("not expired")
        }

        @Test
        fun `throws when status is ACCEPTED`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.ACCEPTED, expiresAt = pastExpiry)
            assertThatThrownBy { ok.markExpired(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("PENDING")
        }

        @Test
        fun `throws when status is DECLINED`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.DECLINED, expiresAt = pastExpiry)
            assertThatThrownBy { ok.markExpired(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is EXPIRED`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.EXPIRED, expiresAt = pastExpiry)
            assertThatThrownBy { ok.markExpired(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is UNDONE`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.UNDONE, expiresAt = pastExpiry)
            assertThatThrownBy { ok.markExpired(FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
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
        fun `undoes PENDING ok when called by recipient`() {
            val ok = pendingOk()
            ok.undo(recipient, FIXED_NOW)
            assertThat(ok.status).isEqualTo(OkStatus.UNDONE)
            assertThat(ok.undoneBy).isEqualTo(recipient)
            assertThat(ok.undoneAt).isEqualTo(FIXED_NOW)
            assertThat(ok.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `undoes ACCEPTED ok`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.ACCEPTED, expiresAt = futureExpiry)
            ok.undo(sender, FIXED_NOW)
            assertThat(ok.status).isEqualTo(OkStatus.UNDONE)
            assertThat(ok.undoneBy).isEqualTo(sender)
            assertThat(ok.undoneAt).isEqualTo(FIXED_NOW)
            assertThat(ok.updatedAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `throws when caller is neither sender nor recipient`() {
            val outsider = createUser()
            val ok = pendingOk()
            assertThatThrownBy { ok.undo(outsider, FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("sender or recipient")
        }

        @Test
        fun `throws when status is DECLINED`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.DECLINED, expiresAt = futureExpiry)
            assertThatThrownBy { ok.undo(sender, FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is EXPIRED`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.EXPIRED, expiresAt = futureExpiry)
            assertThatThrownBy { ok.undo(sender, FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `throws when status is UNDONE`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.UNDONE, expiresAt = futureExpiry)
            assertThatThrownBy { ok.undo(sender, FIXED_NOW) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
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
        fun `returns false when DECLINED even if past expiresAt`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.DECLINED, expiresAt = pastExpiry)
            assertThat(ok.isExpired(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when EXPIRED even if past expiresAt`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.EXPIRED, expiresAt = pastExpiry)
            assertThat(ok.isExpired(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when UNDONE even if past expiresAt`() {
            val ok = createOk(sender = sender, recipient = recipient,
                              status = OkStatus.UNDONE, expiresAt = pastExpiry)
            assertThat(ok.isExpired(FIXED_NOW)).isFalse()
        }

        @Test
        fun `returns false when PENDING and now equals expiresAt`() {
            val ok = pendingOk(expiresAt = FIXED_NOW)
            assertThat(ok.isExpired(FIXED_NOW)).isFalse()
        }
    }
}
