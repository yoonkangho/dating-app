package com.kenneth.datingapp.admin

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * Audit log of all admin actions.
 */
@Entity
@Table(
    name = "admin_actions",
    indexes = [
        Index(name = "idx_admin_actions_admin_id", columnList = "admin_id"),
        Index(name = "idx_admin_actions_target", columnList = "target_type, target_id"),
        Index(name = "idx_admin_actions_created_at", columnList = "created_at")
    ]
)
class AdminAction(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    val admin: AdminUser,

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    val actionType: AdminActionType,

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    val targetType: AdminTargetType,

    @Column(name = "target_id", nullable = false)
    val targetId: UUID,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val details: Map<String, Any>? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AdminAction) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
