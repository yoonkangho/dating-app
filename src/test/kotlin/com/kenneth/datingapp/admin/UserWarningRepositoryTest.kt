package com.kenneth.datingapp.admin

import com.kenneth.datingapp.admin.internal.AdminUserRepository
import com.kenneth.datingapp.admin.internal.UserWarningRepository
import com.kenneth.datingapp.support.IntegrationTestBase
import com.kenneth.datingapp.support.TestFixtures.createAdminUser
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.internal.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserWarningRepositoryTest : IntegrationTestBase() {

    @Autowired private lateinit var userWarningRepository: UserWarningRepository
    @Autowired private lateinit var adminUserRepository: AdminUserRepository
    @Autowired private lateinit var userRepository: UserRepository

    @Test
    fun `findByUserId returns warnings for user`() {
        val admin = adminUserRepository.save(createAdminUser(role = AdminRole.MODERATOR))
        val user = userRepository.save(createUser())
        userWarningRepository.save(UserWarning(user = user, issuedBy = admin, reason = "First warning"))
        userWarningRepository.save(UserWarning(user = user, issuedBy = admin, reason = "Second warning"))
        flushAndClear()

        val warnings = userWarningRepository.findByUserId(user.id)
        assertThat(warnings).hasSize(2)
    }

    @Test
    fun `countByUserId returns correct count`() {
        val admin = adminUserRepository.save(createAdminUser(role = AdminRole.MODERATOR))
        val user = userRepository.save(createUser())
        userWarningRepository.save(UserWarning(user = user, issuedBy = admin, reason = "Warning 1"))
        userWarningRepository.save(UserWarning(user = user, issuedBy = admin, reason = "Warning 2"))
        userWarningRepository.save(UserWarning(user = user, issuedBy = admin, reason = "Warning 3"))
        flushAndClear()

        assertThat(userWarningRepository.countByUserId(user.id)).isEqualTo(3)
    }

    @Test
    fun `findByUserId returns empty when no warnings`() {
        val user = userRepository.save(createUser())
        flushAndClear()

        assertThat(userWarningRepository.findByUserId(user.id)).isEmpty()
    }
}
