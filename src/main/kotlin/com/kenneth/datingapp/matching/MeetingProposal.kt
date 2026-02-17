package com.kenneth.datingapp.matching

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Meeting proposal - one of 4 date/location options proposed by the woman.
 */
@Entity
@Table(
    name = "meeting_proposals",
    indexes = [
        Index(name = "idx_meeting_proposals_meeting_id", columnList = "meeting_id")
    ]
)
class MeetingProposal(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    val meeting: Meeting,

    @Column(name = "proposed_date", nullable = false)
    val proposedDate: Instant,

    @Column(name = "proposed_location", nullable = false, length = 200)
    val proposedLocation: String,

    @Column(name = "is_selected", nullable = false)
    var isSelected: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    /**
     * Marks this proposal as the selected one.
     */
    fun select() {
        isSelected = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeetingProposal) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
