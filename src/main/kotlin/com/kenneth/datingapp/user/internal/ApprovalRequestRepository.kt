package com.kenneth.datingapp.user.internal

import com.kenneth.datingapp.user.ApprovalRequest
import com.kenneth.datingapp.user.ApprovalRequestStatus
import com.kenneth.datingapp.user.ApprovalRequestType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ApprovalRequestRepository : JpaRepository<ApprovalRequest, UUID> {
    fun findByStatus(status: ApprovalRequestStatus): List<ApprovalRequest>
    fun findByUserIdAndRequestType(userId: UUID, requestType: ApprovalRequestType): List<ApprovalRequest>
    fun findByStatusAndRequestType(status: ApprovalRequestStatus, requestType: ApprovalRequestType): List<ApprovalRequest>
}
