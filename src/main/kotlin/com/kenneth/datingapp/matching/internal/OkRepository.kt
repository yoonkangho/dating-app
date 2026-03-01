package com.kenneth.datingapp.matching.internal

import com.kenneth.datingapp.matching.Ok
import com.kenneth.datingapp.matching.OkStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface OkRepository : JpaRepository<Ok, UUID> {
    fun findBySenderIdAndRecipientId(senderId: UUID, recipientId: UUID): Ok?
    fun findBySenderIdAndStatus(senderId: UUID, status: OkStatus): List<Ok>
    fun findByRecipientIdAndStatus(recipientId: UUID, status: OkStatus): List<Ok>
    fun findByStatusAndExpiresAtBefore(status: OkStatus, cutoff: Instant): List<Ok>
    fun existsBySenderIdAndRecipientId(senderId: UUID, recipientId: UUID): Boolean
}
