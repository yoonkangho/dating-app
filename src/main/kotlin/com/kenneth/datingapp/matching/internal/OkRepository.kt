package com.kenneth.datingapp.matching.internal

import com.kenneth.datingapp.matching.Ok
import com.kenneth.datingapp.matching.OkStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface OkRepository : JpaRepository<Ok, UUID> {
    fun findBySenderIdAndRecipientId(senderId: UUID, recipientId: UUID): Ok?
    fun findBySenderIdAndStatus(senderId: UUID, status: OkStatus): List<Ok>
    fun findByRecipientIdAndStatus(recipientId: UUID, status: OkStatus): List<Ok>
    fun findByRecipientIdAndStatusIn(recipientId: UUID, statuses: List<OkStatus>): List<Ok>
    fun findByStatusAndExpiresAtBefore(status: OkStatus, cutoff: Instant): List<Ok>
    fun existsBySenderIdAndRecipientId(senderId: UUID, recipientId: UUID): Boolean

    @Query("SELECT COUNT(o) FROM Ok o WHERE o.sender.id = :senderId AND o.status IN :statuses")
    fun countBySenderIdAndStatusIn(@Param("senderId") senderId: UUID, @Param("statuses") statuses: List<OkStatus>): Long

    @Query("SELECT o FROM Ok o WHERE o.status = 'PENDING' AND o.expiresAt < :now")
    fun findExpiredPendingOks(@Param("now") now: Instant): List<Ok>

    @Query("""
        SELECT o FROM Ok o
        JOIN FETCH o.sender
        WHERE o.recipient.id = :recipientId
        AND o.status = 'PENDING'
        ORDER BY o.sentAt ASC
    """)
    fun findPendingOksWithSender(@Param("recipientId") recipientId: UUID): List<Ok>
}
