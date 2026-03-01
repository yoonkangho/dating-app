package com.kenneth.datingapp.admin.internal

import com.kenneth.datingapp.admin.UserWarning
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserWarningRepository : JpaRepository<UserWarning, UUID> {
    fun findByUserId(userId: UUID): List<UserWarning>
    fun countByUserId(userId: UUID): Long
}
