package com.kenneth.datingapp.admin

import com.kenneth.datingapp.admin.internal.AdminActionRepository
import com.kenneth.datingapp.admin.internal.AdminUserRepository
import com.kenneth.datingapp.support.IntegrationTestBase
import com.kenneth.datingapp.support.TestFixtures.createAdminUser
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.internal.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class AdminActionRepositoryTest : IntegrationTestBase() {

    @Autowired private lateinit var adminActionRepository: AdminActionRepository
    @Autowired private lateinit var adminUserRepository: AdminUserRepository
    @Autowired private lateinit var userRepository: UserRepository

    @Test
    fun `findByTargetTypeAndTargetId returns actions for target`() {
        val admin = adminUserRepository.save(createAdminUser(role = AdminRole.SUPER_ADMIN))
        val user = userRepository.save(createUser())
        val action = AdminAction(
            admin = admin,
            actionType = AdminActionType.USER_BANNED,
            targetType = AdminTargetType.USER,
            targetId = user.id,
            details = mapOf("reason" to "Multiple no-shows")
        )
        adminActionRepository.save(action)
        flushAndClear()

        val found = adminActionRepository.findByTargetTypeAndTargetId(AdminTargetType.USER, user.id)
        assertThat(found).hasSize(1)
        assertThat(found[0].actionType).isEqualTo(AdminActionType.USER_BANNED)
    }

    @Test
    fun `findByAdminId returns actions by admin`() {
        val admin = adminUserRepository.save(createAdminUser(role = AdminRole.SUPER_ADMIN))
        val user = userRepository.save(createUser())
        adminActionRepository.save(AdminAction(
            admin = admin,
            actionType = AdminActionType.USER_BANNED,
            targetType = AdminTargetType.USER,
            targetId = user.id
        ))
        adminActionRepository.save(AdminAction(
            admin = admin,
            actionType = AdminActionType.USER_UNBANNED,
            targetType = AdminTargetType.USER,
            targetId = user.id
        ))
        flushAndClear()

        val actions = adminActionRepository.findByAdminId(admin.id)
        assertThat(actions).hasSize(2)
    }

    @Test
    fun `findByActionType returns actions of given type`() {
        val admin = adminUserRepository.save(createAdminUser(role = AdminRole.SUPER_ADMIN))
        val user = userRepository.save(createUser())
        adminActionRepository.save(AdminAction(
            admin = admin,
            actionType = AdminActionType.USER_BANNED,
            targetType = AdminTargetType.USER,
            targetId = user.id
        ))
        flushAndClear()

        assertThat(adminActionRepository.findByActionType(AdminActionType.USER_BANNED)).hasSize(1)
        assertThat(adminActionRepository.findByActionType(AdminActionType.USER_UNBANNED)).isEmpty()
    }
}
