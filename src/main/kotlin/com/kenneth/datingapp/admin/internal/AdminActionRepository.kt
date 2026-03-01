package com.kenneth.datingapp.admin.internal

import com.kenneth.datingapp.admin.AdminAction
import com.kenneth.datingapp.admin.AdminActionType
import com.kenneth.datingapp.admin.AdminTargetType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AdminActionRepository : JpaRepository<AdminAction, UUID> {
    fun findByTargetTypeAndTargetId(targetType: AdminTargetType, targetId: UUID): List<AdminAction>
    fun findByAdminId(adminId: UUID): List<AdminAction>
    fun findByActionType(actionType: AdminActionType): List<AdminAction>
}
