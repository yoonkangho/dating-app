package com.kenneth.datingapp.matching

import com.kenneth.datingapp.matching.internal.OkRepository
import com.kenneth.datingapp.support.IntegrationTestBase
import com.kenneth.datingapp.support.TestFixtures.createOk
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.UserStatus
import com.kenneth.datingapp.user.internal.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant

class OkRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var okRepository: OkRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `countBySenderIdAndStatusIn returns correct count`() {
        val sender = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val recipient1 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val recipient2 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val recipient3 = userRepository.save(createUser(status = UserStatus.ACTIVE))

        okRepository.save(createOk(sender = sender, recipient = recipient1, status = OkStatus.PENDING))
        okRepository.save(createOk(sender = sender, recipient = recipient2, status = OkStatus.ACCEPTED))
        okRepository.save(createOk(sender = sender, recipient = recipient3, status = OkStatus.DECLINED))
        flushAndClear()

        val count = okRepository.countBySenderIdAndStatusIn(sender.id, listOf(OkStatus.PENDING, OkStatus.ACCEPTED))
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun `existsBySenderIdAndRecipientId returns true when ok exists`() {
        val sender = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val recipient = userRepository.save(createUser(status = UserStatus.ACTIVE))
        okRepository.save(createOk(sender = sender, recipient = recipient))
        flushAndClear()

        assertThat(okRepository.existsBySenderIdAndRecipientId(sender.id, recipient.id)).isTrue()
        assertThat(okRepository.existsBySenderIdAndRecipientId(recipient.id, sender.id)).isFalse()
    }

    @Test
    fun `findExpiredPendingOks returns only expired pending oks`() {
        val sender = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val recipient = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val recipient2 = userRepository.save(createUser(status = UserStatus.ACTIVE))

        val pastDate = Instant.now().minus(Duration.ofDays(8))
        okRepository.save(createOk(sender = sender, recipient = recipient, sentAt = pastDate, expiresAt = pastDate.plus(Duration.ofDays(7))))
        okRepository.save(createOk(sender = sender, recipient = recipient2)) // not expired
        flushAndClear()

        val expired = okRepository.findExpiredPendingOks(Instant.now())
        assertThat(expired).hasSize(1)
    }

    @Test
    fun `findBySenderIdAndRecipientId returns ok when exists`() {
        val sender = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val recipient = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok = okRepository.save(createOk(sender = sender, recipient = recipient))
        flushAndClear()

        val found = okRepository.findBySenderIdAndRecipientId(sender.id, recipient.id)
        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(ok.id)
    }
}
