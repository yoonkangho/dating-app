package com.kenneth.datingapp.feedback

import com.kenneth.datingapp.matching.Meeting
import com.kenneth.datingapp.user.User
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * Feedback submitted by a user after a meeting.
 * 
 * Business rules:
 * - Mandatory: user is locked out until feedback is submitted
 * - Prompted 3 hours after scheduled meeting time
 * - If meeting happened: ratings are required, contact exchange is allowed
 * - If meeting didn't happen: no_show_reason is required
 * - If no_show_reason = THEY_DID_NOT_SHOW: auto-creates Dispute
 */
@Entity
@Table(
    name = "feedbacks",
    indexes = [
        Index(name = "idx_feedbacks_meeting_id", columnList = "meeting_id"),
        Index(name = "idx_feedbacks_submitted_by", columnList = "submitted_by"),
        Index(name = "idx_feedbacks_about_user", columnList = "about_user")
    ]
)
class Feedback(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    val meeting: Meeting,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitted_by", nullable = false)
    val submittedBy: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "about_user", nullable = false)
    val aboutUser: User,

    @Column(name = "meeting_happened", nullable = false)
    val meetingHappened: Boolean,

    @Enumerated(EnumType.STRING)
    @Column(name = "no_show_reason", length = 20)
    val noShowReason: NoShowReason? = null,

    @Column(columnDefinition = "TEXT")
    val comment: String? = null,

    /**
     * Red flag categories as PostgreSQL array.
     * Null or empty if no red flags reported.
     */
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "red_flag_categories", columnDefinition = "varchar(30)[]")
    val redFlagCategories: List<String>? = null,

    @Column(name = "red_flag_description", columnDefinition = "TEXT")
    val redFlagDescription: String? = null,

    @Column(name = "wants_contact_exchange", nullable = false)
    val wantsContactExchange: Boolean = false,

    @Column(name = "contact_shared_at")
    var contactSharedAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    init {
        if (!meetingHappened) {
            require(noShowReason != null) { "no_show_reason is required when meeting did not happen" }
            require(!wantsContactExchange) { "Cannot request contact exchange when meeting did not happen" }
        }
    }

    /**
     * Checks if this feedback reports the counterparty as no-show.
     */
    fun reportsNoShow(): Boolean = noShowReason == NoShowReason.THEY_DID_NOT_SHOW

    /**
     * Checks if red flags were reported.
     */
    fun hasRedFlags(): Boolean = !redFlagCategories.isNullOrEmpty()

    /**
     * Marks that contact was shared with counterparty.
     */
    fun markContactShared() {
        require(wantsContactExchange) { "User did not request contact exchange" }
        require(meetingHappened) { "Cannot share contact when meeting did not happen" }
        contactSharedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Feedback) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
