package com.kenneth.datingapp.feedback.internal

import com.kenneth.datingapp.feedback.Rating
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface RatingRepository : JpaRepository<Rating, UUID> {
    fun findByFeedbackId(feedbackId: UUID): Rating?

    @Query("""
        SELECT AVG(r.looks) FROM Rating r WHERE r.ratedUser.id = :userId
    """)
    fun averageLooksByRatedUserId(@Param("userId") userId: UUID): Double?

    @Query("""
        SELECT AVG(r.attitude) FROM Rating r WHERE r.ratedUser.id = :userId
    """)
    fun averageAttitudeByRatedUserId(@Param("userId") userId: UUID): Double?

    fun findByRatedUserId(ratedUserId: UUID): List<Rating>
}
