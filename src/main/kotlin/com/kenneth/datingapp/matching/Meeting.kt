package com.kenneth.datingapp.matching

import com.kenneth.datingapp.user.User
import jakarta.persistence.*
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Meeting entity representing a scheduled meeting between two matched users.
 * 
 * Created when an Ok is accepted (mutual match).
 * 
 * Deadlines:
 * - Woman must propose dates within 2 days of meeting creation
 * - Man must finalize within 2 days of proposal
 * - Meeting must occur within 3 weeks of meeting creation
 */
@Entity
@Table(
    name = "meetings",
    indexes = [
        Index(name = "idx_meetings_ok_id", columnList = "ok_id", unique = true),
        Index(name = "idx_meetings_status", columnList = "status"),
        Index(name = "idx_meetings_woman_id", columnList = "woman_id"),
        Index(name = "idx_meetings_man_id", columnList = "man_id")
    ]
)
class Meeting(
    @Id
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ok_id", nullable = false, unique = true)
    val ok: Ok,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "woman_id", nullable = false)
    val woman: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "man_id", nullable = false)
    val man: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: MeetingStatus = MeetingStatus.PENDING_PROPOSAL,

    @Column(name = "proposal_deadline", nullable = false)
    var proposalDeadline: Instant = Instant.now().plus(PROPOSAL_DEADLINE_DURATION),

    @Column(name = "finalization_deadline")
    var finalizationDeadline: Instant? = null,

    @Column(name = "meeting_deadline", nullable = false)
    val meetingDeadline: Instant = Instant.now().plus(MEETING_DEADLINE_DURATION),

    @Column(name = "scheduled_at")
    var scheduledAt: Instant? = null,

    @Column(name = "location_name", length = 200)
    var locationName: String? = null,

    @Column(name = "location_address", length = 500)
    var locationAddress: String? = null,

    @Column(name = "is_rescheduled", nullable = false)
    var isRescheduled: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    var cancelledBy: User? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: Instant? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_reason", length = 40)
    var cancellationReason: CancellationReason? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null
) {
    companion object {
        val PROPOSAL_DEADLINE_DURATION: Duration = Duration.ofDays(2)
        val FINALIZATION_DEADLINE_DURATION: Duration = Duration.ofDays(2)
        val MEETING_DEADLINE_DURATION: Duration = Duration.ofDays(21) // 3 weeks
    }

    /**
     * Marks that woman has submitted proposals. Sets finalization deadline.
     */
    fun markProposalSubmitted(now: Instant) {
        require(status == MeetingStatus.PENDING_PROPOSAL) {
            "Can only submit proposal in PENDING_PROPOSAL status, current: $status"
        }
        status = MeetingStatus.PENDING_FINALIZATION
        finalizationDeadline = now.plus(FINALIZATION_DEADLINE_DURATION)
        updatedAt = now
    }

    /**
     * Finalizes the meeting with selected date and location.
     */
    fun finalize(now: Instant, selectedDate: Instant, locationName: String, locationAddress: String) {
        require(status == MeetingStatus.PENDING_FINALIZATION) {
            "Can only finalize in PENDING_FINALIZATION status, current: $status"
        }
        require(selectedDate.isBefore(meetingDeadline)) {
            "Meeting date must be within 3 weeks of mutual ok"
        }
        status = MeetingStatus.SCHEDULED
        scheduledAt = selectedDate
        this.locationName = locationName
        this.locationAddress = locationAddress
        updatedAt = now
    }

    /**
     * Checks if rescheduling is allowed.
     */
    fun canReschedule(): Boolean =
        status == MeetingStatus.SCHEDULED && !isRescheduled

    /**
     * Initiates a reschedule. Returns meeting to PENDING_PROPOSAL status.
     * Woman must propose new dates, man must finalize again.
     */
    fun reschedule(now: Instant) {
        require(canReschedule()) { "Cannot reschedule: already rescheduled or not in SCHEDULED status" }
        status = MeetingStatus.PENDING_PROPOSAL
        proposalDeadline = now.plus(PROPOSAL_DEADLINE_DURATION)
        finalizationDeadline = null
        scheduledAt = null
        locationName = null
        locationAddress = null
        isRescheduled = true
        updatedAt = now
    }

    /**
     * Cancels the meeting.
     */
    fun cancel(now: Instant, by: User?, reason: CancellationReason) {
        require(status != MeetingStatus.COMPLETED && status != MeetingStatus.CANCELLED) {
            "Cannot cancel meeting in $status status"
        }
        status = MeetingStatus.CANCELLED
        cancelledBy = by
        cancelledAt = now
        cancellationReason = reason
        updatedAt = now
    }

    /**
     * Marks meeting as completed.
     */
    fun complete(now: Instant) {
        require(status == MeetingStatus.SCHEDULED) {
            "Can only complete SCHEDULED meeting, current: $status"
        }
        status = MeetingStatus.COMPLETED
        updatedAt = now
    }

    /**
     * Checks if proposal deadline has passed.
     */
    fun isProposalDeadlineMissed(now: Instant): Boolean =
        status == MeetingStatus.PENDING_PROPOSAL && now.isAfter(proposalDeadline)

    /**
     * Checks if finalization deadline has passed.
     */
    fun isFinalizationDeadlineMissed(now: Instant): Boolean =
        status == MeetingStatus.PENDING_FINALIZATION &&
        finalizationDeadline != null &&
        now.isAfter(finalizationDeadline)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Meeting) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
