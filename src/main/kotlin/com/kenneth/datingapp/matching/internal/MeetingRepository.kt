package com.kenneth.datingapp.matching.internal

import com.kenneth.datingapp.matching.Meeting
import com.kenneth.datingapp.matching.MeetingStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MeetingRepository : JpaRepository<Meeting, UUID> {
    fun findByOkId(okId: UUID): Meeting?
    fun findByStatus(status: MeetingStatus): List<Meeting>
    fun findByWomanIdOrManId(womanId: UUID, manId: UUID): List<Meeting>
}
