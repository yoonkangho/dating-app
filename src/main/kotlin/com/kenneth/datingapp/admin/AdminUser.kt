package com.kenneth.datingapp.admin

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Admin user for backoffice operations.
 */
@Entity
@Table(
    name = "admin_users",
    indexes = [
        Index(name = "idx_admin_users_email", columnList = "email", unique = true)
    ]
)
class AdminUser(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 255)
    val email: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: AdminRole,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null
) {
    /**
     * Checks if admin has the given role or higher.
     */
    fun hasRole(requiredRole: AdminRole): Boolean {
        return when (requiredRole) {
            AdminRole.REVIEWER -> true
            AdminRole.MODERATOR -> role == AdminRole.MODERATOR || role == AdminRole.SUPER_ADMIN
            AdminRole.SUPER_ADMIN -> role == AdminRole.SUPER_ADMIN
        }
    }

    /**
     * Deactivates the admin account.
     */
    fun deactivate() {
        isActive = false
        updatedAt = Instant.now()
    }

    /**
     * Reactivates the admin account.
     */
    fun activate() {
        isActive = true
        updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdminUser) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
