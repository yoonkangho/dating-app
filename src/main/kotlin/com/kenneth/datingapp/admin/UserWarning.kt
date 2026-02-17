package com.kenneth.datingapp.admin

import com.kenneth.datingapp.feedback.Dispute
import com.kenneth.datingapp.feedback.Feedback
import com.kenneth.datingapp.user.User
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Warning issued to a user by an admin.
 * Stored separately for easy count queries.
 */
@Entity
@Table(
    name = "user_warnings",
    indexes = [
        Index(name = "idx_user_warnings_user_id", columnList = "user_id"),
        Index(name = "idx_user_warnings_issued_by", columnList = "issued_by")
    ]
)
class UserWarning(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by", nullable = false)
    val issuedBy: AdminUser,

    @Column(nullable = false, columnDefinition = "TEXT")
    val reason: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_dispute_id")
    val relatedDispute: Dispute? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_feedback_id")
    val relatedFeedback: Feedback? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserWarning) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
