package com.kenneth.datingapp.user

import com.kenneth.datingapp.support.IntegrationTestBase
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.internal.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `findByEmail returns user when exists`() {
        userRepository.save(createUser(email = "jane@example.com"))
        flushAndClear()

        val found = userRepository.findByEmail("jane@example.com")

        assertThat(found).isNotNull
        assertThat(found!!.email).isEqualTo("jane@example.com")
    }

    @Test
    fun `findByEmail returns null when not exists`() {
        val found = userRepository.findByEmail("nonexistent@example.com")

        assertThat(found).isNull()
    }

    @Test
    fun `findByPhone returns user when exists`() {
        userRepository.save(createUser(phone = "010-1234-5678"))
        flushAndClear()

        val found = userRepository.findByPhone("010-1234-5678")

        assertThat(found).isNotNull
        assertThat(found!!.phone).isEqualTo("010-1234-5678")
    }

    @Test
    fun `findByPhone returns null when not exists`() {
        val found = userRepository.findByPhone("000-0000-0000")

        assertThat(found).isNull()
    }

    @Test
    fun `existsByEmail returns true when email exists`() {
        userRepository.save(createUser(email = "exists@example.com"))
        flushAndClear()

        assertThat(userRepository.existsByEmail("exists@example.com")).isTrue()
    }

    @Test
    fun `existsByEmail returns false when email does not exist`() {
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse()
    }

    @Test
    fun `existsByPhone returns true when phone exists`() {
        userRepository.save(createUser(phone = "010-1234-5678"))
        flushAndClear()

        assertThat(userRepository.existsByPhone("010-1234-5678")).isTrue()
    }

    @Test
    fun `existsByPhone returns false when phone does not exist`() {
        assertThat(userRepository.existsByPhone("000-0000-0000")).isFalse()
    }

    @Test
    fun `findByStatus returns only users with matching status`() {
        userRepository.save(createUser(status = UserStatus.ACTIVE))
        userRepository.save(createUser(status = UserStatus.ACTIVE))
        userRepository.save(createUser(status = UserStatus.BANNED))
        flushAndClear()

        val active = userRepository.findByStatus(UserStatus.ACTIVE)

        assertThat(active).hasSize(2)
        assertThat(active).allMatch { it.status == UserStatus.ACTIVE }
    }

    @Test
    fun `findByStatus returns empty list when no matching users`() {
        userRepository.save(createUser(status = UserStatus.ACTIVE))
        flushAndClear()

        val banned = userRepository.findByStatus(UserStatus.BANNED)

        assertThat(banned).isEmpty()
    }
}
