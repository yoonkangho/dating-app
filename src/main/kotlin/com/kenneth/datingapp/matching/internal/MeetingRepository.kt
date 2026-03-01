package com.kenneth.datingapp.matching.internal

import com.kenneth.datingapp.matching.Meeting
import com.kenneth.datingapp.matching.MeetingStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface MeetingRepository : JpaRepository<Meeting, UUID> {
    fun findByOkId(okId: UUID): Meeting?
    fun findByStatus(status: MeetingStatus): List<Meeting>
    fun findByWomanIdOrManId(womanId: UUID, manId: UUID): List<Meeting>

    @Query("SELECT m FROM Meeting m WHERE m.status = 'PENDING_PROPOSAL' AND m.proposalDeadline < :now")
    fun findOverdueProposals(@Param("now") now: Instant): List<Meeting>

    @Query("SELECT m FROM Meeting m WHERE m.status = 'PENDING_FINALIZATION' AND m.finalizationDeadline < :now")
    fun findOverdueFinalizations(@Param("now") now: Instant): List<Meeting>

    @Query("SELECT m FROM Meeting m WHERE m.status = 'SCHEDULED' AND m.scheduledAt < :cutoff")
    fun findCompletableMeetings(@Param("cutoff") cutoff: Instant): List<Meeting>

    @Query("SELECT m FROM Meeting m WHERE (m.woman.id = :userId OR m.man.id = :userId) AND m.status IN :statuses")
    fun findByParticipantAndStatusIn(@Param("userId") userId: UUID, @Param("statuses") statuses: List<MeetingStatus>): List<Meeting>
}
