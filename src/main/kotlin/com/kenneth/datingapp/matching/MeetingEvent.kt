package com.kenneth.datingapp.matching

import com.kenneth.datingapp.user.User
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * Meeting event for audit logging. Tracks all changes to a meeting.
 */
@Entity
@Table(
    name = "meeting_events",
    indexes = [
        Index(name = "idx_meeting_events_meeting_id", columnList = "meeting_id")
    ]
)
class MeetingEvent(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    val meeting: Meeting,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    val eventType: MeetingEventType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    val actor: User?,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val details: Map<String, Any>? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeetingEvent) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
