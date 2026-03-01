package com.kenneth.datingapp.feedback.internal

import com.kenneth.datingapp.feedback.Feedback
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface FeedbackRepository : JpaRepository<Feedback, UUID> {
    fun findByMeetingIdAndSubmittedById(meetingId: UUID, submittedById: UUID): Feedback?
    fun findBySubmittedById(submittedById: UUID): List<Feedback>
    fun existsByMeetingIdAndSubmittedById(meetingId: UUID, submittedById: UUID): Boolean

    @Query("""
        SELECT f FROM Feedback f
        JOIN f.meeting m
        WHERE f.submittedBy.id = :userId
        ORDER BY f.createdAt DESC
    """)
    fun findByUserId(@Param("userId") userId: UUID): List<Feedback>

    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM Meeting m
        WHERE m.status = 'COMPLETED'
        AND m.scheduledAt < :cutoff
        AND (m.woman.id = :userId OR m.man.id = :userId)
        AND NOT EXISTS (
            SELECT f FROM Feedback f
            WHERE f.meeting = m AND f.submittedBy.id = :userId
        )
    """)
    fun hasPendingFeedback(@Param("userId") userId: UUID, @Param("cutoff") cutoff: Instant): Boolean
}
