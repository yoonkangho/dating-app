package com.kenneth.datingapp.user

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * Unified approval request for all user-initiated changes requiring admin review.
 *
 * Request types:
 * - SIGN_UP: New user registration (payload contains all profile fields)
 * - PHOTO_ADD: Adding a new photo (target_id references UserPhoto)
 * - BIO_CHANGE: Updating bio (payload contains old_value and new_value)
 *
 * Provides full audit trail of all approval requests and their outcomes.
 *
 * `reviewedByAdminId` stores the reviewing admin's UUID rather than a JPA reference
 * to keep this entity free of a cross-module `@ManyToOne` back into `admin`. The FK
 * to `admin_users(id)` is enforced at the database level (see V5 migration).
 */
@Entity
@Table(
    name = "approval_requests",
    indexes = [
        Index(name = "idx_approval_requests_status", columnList = "status, request_type"),
        Index(name = "idx_approval_requests_user", columnList = "user_id")
    ]
)
class ApprovalRequest(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    val requestType: ApprovalRequestType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ApprovalRequestStatus = ApprovalRequestStatus.PENDING,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 20)
    val targetType: ApprovalTargetType? = null,

    @Column(name = "target_id")
    val targetId: UUID? = null,

    /**
     * JSON payload containing the request data.
     * Structure varies by request_type - see class documentation.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    val payload: Map<String, Any>,

    @Column(name = "reviewed_by")
    var reviewedByAdminId: UUID? = null,

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,

    @Column(name = "review_notes", length = 1000)
    var reviewNotes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null
) {
    /**
     * Approves the request.
     */
    fun approve(adminId: UUID, now: Instant, notes: String? = null) {
        require(status == ApprovalRequestStatus.PENDING) {
            "Can only approve PENDING requests, current: $status"
        }
        status = ApprovalRequestStatus.APPROVED
        reviewedByAdminId = adminId
        reviewedAt = now
        reviewNotes = notes
        updatedAt = now
    }

    /**
     * Rejects the request.
     */
    fun reject(adminId: UUID, now: Instant, notes: String? = null) {
        require(status == ApprovalRequestStatus.PENDING) {
            "Can only reject PENDING requests, current: $status"
        }
        status = ApprovalRequestStatus.REJECTED
        reviewedByAdminId = adminId
        reviewedAt = now
        reviewNotes = notes
        updatedAt = now
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApprovalRequest) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
