package com.kenneth.datingapp.feedback

import com.kenneth.datingapp.user.User
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Rating given to a user after a meeting.
 * 
 * Only created when meeting_happened = true in the associated Feedback.
 * Separated from Feedback for easy aggregation (AVG queries).
 */
@Entity
@Table(
    name = "ratings",
    indexes = [
        Index(name = "idx_ratings_feedback_id", columnList = "feedback_id", unique = true),
        Index(name = "idx_ratings_rated_user", columnList = "rated_user"),
        Index(name = "idx_ratings_rated_by", columnList = "rated_by")
    ]
)
class Rating(
    @Id
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false, unique = true)
    val feedback: Feedback,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rated_user", nullable = false)
    val ratedUser: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rated_by", nullable = false)
    val ratedBy: User,

    @Column(nullable = false)
    val looks: Int,

    @Column(nullable = false)
    val attitude: Int,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    init {
        require(looks in 1..5) { "looks rating must be between 1 and 5" }
        require(attitude in 1..5) { "attitude rating must be between 1 and 5" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Rating) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
