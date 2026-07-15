package com.kenneth.datingapp.feedback

import com.kenneth.datingapp.matching.Meeting
import com.kenneth.datingapp.user.User
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Dispute triggered when a user reports the counterparty as no-show.
 *
 * If both users report each other as no-show, two separate disputes are created.
 *
 * `resolvedByAdminId` stores the resolving admin's UUID rather than a JPA reference
 * to keep this entity free of a cross-module `@ManyToOne` back into `admin`. The FK
 * to `admin_users(id)` is enforced at the database level (see V5 migration).
 */
@Entity
@Table(
    name = "disputes",
    indexes = [
        Index(name = "idx_disputes_status", columnList = "status"),
        Index(name = "idx_disputes_meeting_id", columnList = "meeting_id"),
        Index(name = "idx_disputes_reported_user", columnList = "reported_user")
    ]
)
class Dispute(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    val meeting: Meeting,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    val feedback: Feedback,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by", nullable = false)
    val reportedBy: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user", nullable = false)
    val reportedUser: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: DisputeStatus = DisputeStatus.OPEN,

    @Enumerated(EnumType.STRING)
    @Column(length = 25)
    var resolution: DisputeResolution? = null,

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    var resolutionNotes: String? = null,

    @Column(name = "resolved_by")
    var resolvedByAdminId: UUID? = null,

    @Column(name = "resolved_at")
    var resolvedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null
) {
    /**
     * Marks dispute as under investigation.
     */
    fun startInvestigation(now: Instant) {
        require(status == DisputeStatus.OPEN) {
            "Can only start investigation on OPEN dispute, current: $status"
        }
        status = DisputeStatus.INVESTIGATING
        updatedAt = now
    }

    /**
     * Resolves the dispute.
     */
    fun resolve(adminId: UUID, resolution: DisputeResolution, now: Instant, notes: String? = null) {
        require(status != DisputeStatus.RESOLVED) {
            "Dispute is already resolved"
        }
        status = DisputeStatus.RESOLVED
        this.resolution = resolution
        this.resolutionNotes = notes
        this.resolvedByAdminId = adminId
        this.resolvedAt = now
        updatedAt = now
    }

    /**
     * Reopens a resolved dispute.
     */
    fun reopen(now: Instant) {
        require(status == DisputeStatus.RESOLVED) {
            "Can only reopen RESOLVED dispute, current: $status"
        }
        status = DisputeStatus.OPEN
        resolution = null
        resolutionNotes = null
        resolvedByAdminId = null
        resolvedAt = null
        updatedAt = now
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Dispute) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
