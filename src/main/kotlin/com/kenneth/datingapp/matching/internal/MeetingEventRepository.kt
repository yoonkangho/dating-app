package com.kenneth.datingapp.matching.internal

import com.kenneth.datingapp.matching.MeetingEvent
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MeetingEventRepository : JpaRepository<MeetingEvent, UUID> {
    fun findByMeetingIdOrderByCreatedAtAsc(meetingId: UUID): List<MeetingEvent>
}
