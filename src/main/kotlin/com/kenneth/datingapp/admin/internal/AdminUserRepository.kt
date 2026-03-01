package com.kenneth.datingapp.admin.internal

import com.kenneth.datingapp.admin.AdminRole
import com.kenneth.datingapp.admin.AdminUser
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AdminUserRepository : JpaRepository<AdminUser, UUID> {
    fun findByEmail(email: String): AdminUser?
    fun findByRole(role: AdminRole): List<AdminUser>
    fun findByIsActive(isActive: Boolean): List<AdminUser>
}
