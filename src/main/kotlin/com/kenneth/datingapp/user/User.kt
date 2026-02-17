package com.kenneth.datingapp.user

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * User account entity. Contains authentication and account-level information only.
 * Profile data (display name, bio, etc.) is stored in [Profile].
 */
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email", unique = true),
        Index(name = "idx_users_status", columnList = "status")
    ]
)
class User(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 255)
    val email: String,

    @Column(length = 50)
    val phone: String? = null,

    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: UserStatus = UserStatus.VERIFICATION_PENDING,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null
) {
    /**
     * Marks user as verified and moves to waitlist.
     */
    fun markVerified() {
        require(status == UserStatus.VERIFICATION_PENDING) {
            "Can only verify from VERIFICATION_PENDING status, current: $status"
        }
        status = UserStatus.WAITLISTED
        updatedAt = Instant.now()
    }

    /**
     * Releases user from waitlist for admin review.
     */
    fun releaseFromWaitlist() {
        require(status == UserStatus.WAITLISTED) {
            "Can only release from WAITLISTED status, current: $status"
        }
        status = UserStatus.REVIEW_PENDING
        updatedAt = Instant.now()
    }

    /**
     * Approves user after admin review.
     */
    fun approve() {
        require(status == UserStatus.REVIEW_PENDING) {
            "Can only approve from REVIEW_PENDING status, current: $status"
        }
        status = UserStatus.ACTIVE
        updatedAt = Instant.now()
    }

    /**
     * Rejects user. User will see "pending" indefinitely.
     */
    fun reject() {
        require(status == UserStatus.REVIEW_PENDING) {
            "Can only reject from REVIEW_PENDING status, current: $status"
        }
        status = UserStatus.REJECTED
        updatedAt = Instant.now()
    }

    /**
     * Bans user.
     */
    fun ban() {
        require(status == UserStatus.ACTIVE) {
            "Can only ban ACTIVE users, current: $status"
        }
        status = UserStatus.BANNED
        updatedAt = Instant.now()
    }

    /**
     * Unbans user (admin action). Returns user to ACTIVE status.
     */
    fun unban() {
        require(status == UserStatus.BANNED) {
            "Can only unban BANNED users, current: $status"
        }
        status = UserStatus.ACTIVE
        updatedAt = Instant.now()
    }

    /**
     * Reactivates a rejected user (admin action). Returns user to ACTIVE status.
     */
    fun reactivate() {
        require(status == UserStatus.REJECTED) {
            "Can only reactivate REJECTED users, current: $status"
        }
        status = UserStatus.ACTIVE
        updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
