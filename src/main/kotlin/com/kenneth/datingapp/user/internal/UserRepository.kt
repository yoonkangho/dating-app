package com.kenneth.datingapp.user.internal

import com.kenneth.datingapp.user.User
import com.kenneth.datingapp.user.UserStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun findByPhone(phone: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByPhone(phone: String): Boolean
    fun findByStatus(status: UserStatus): List<User>
}
