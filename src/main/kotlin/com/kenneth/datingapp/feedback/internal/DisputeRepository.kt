package com.kenneth.datingapp.feedback.internal

import com.kenneth.datingapp.feedback.Dispute
import com.kenneth.datingapp.feedback.DisputeStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DisputeRepository : JpaRepository<Dispute, UUID> {
    fun findByStatus(status: DisputeStatus): List<Dispute>
    fun findByMeetingId(meetingId: UUID): List<Dispute>
    fun findByReportedUserId(reportedUserId: UUID): List<Dispute>
    fun findByReportedById(reportedById: UUID): List<Dispute>
}
