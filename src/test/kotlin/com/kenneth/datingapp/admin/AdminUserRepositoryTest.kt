package com.kenneth.datingapp.admin

import com.kenneth.datingapp.admin.internal.AdminUserRepository
import com.kenneth.datingapp.support.IntegrationTestBase
import com.kenneth.datingapp.support.TestFixtures.createAdminUser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AdminUserRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var adminUserRepository: AdminUserRepository

    @Test
    fun `findByEmail returns admin when exists`() {
        val admin = adminUserRepository.save(createAdminUser(email = "test@admin.com"))
        flushAndClear()

        val found = adminUserRepository.findByEmail("test@admin.com")
        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(admin.id)
    }

    @Test
    fun `findByEmail returns null when not exists`() {
        val found = adminUserRepository.findByEmail("nonexistent@admin.com")
        assertThat(found).isNull()
    }

    @Test
    fun `findByRole returns admins with given role`() {
        adminUserRepository.save(createAdminUser(role = AdminRole.REVIEWER))
        adminUserRepository.save(createAdminUser(role = AdminRole.REVIEWER))
        adminUserRepository.save(createAdminUser(role = AdminRole.SUPER_ADMIN))
        flushAndClear()

        val reviewers = adminUserRepository.findByRole(AdminRole.REVIEWER)
        assertThat(reviewers).hasSize(2)

        val superAdmins = adminUserRepository.findByRole(AdminRole.SUPER_ADMIN)
        assertThat(superAdmins).hasSize(1)
    }

    @Test
    fun `findByIsActive returns only active admins`() {
        val active = adminUserRepository.save(createAdminUser())
        val inactive = adminUserRepository.save(createAdminUser())
        inactive.deactivate()
        adminUserRepository.save(inactive)
        flushAndClear()

        val activeAdmins = adminUserRepository.findByIsActive(true)
        assertThat(activeAdmins).hasSize(1)
        assertThat(activeAdmins[0].id).isEqualTo(active.id)
    }
}
