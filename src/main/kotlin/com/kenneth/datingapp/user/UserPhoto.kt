package com.kenneth.datingapp.user

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * User profile photo. Photos require admin approval before becoming active.
 * Users must maintain at least 3 active photos.
 */
@Entity
@Table(
    name = "user_photos",
    indexes = [
        Index(name = "idx_user_photos_profile_active", columnList = "profile_id, is_active")
    ]
)
class UserPhoto(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    val profile: Profile,

    @Column(nullable = false, length = 500)
    val url: String,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int,

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false,

    /**
     * false = pending approval or removed
     * true = approved and visible
     */
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null
) {
    /**
     * Activates the photo after approval.
     */
    fun activate() {
        isActive = true
        updatedAt = Instant.now()
    }

    /**
     * Deactivates the photo (for removal).
     */
    fun deactivate() {
        isActive = false
        isPrimary = false
        updatedAt = Instant.now()
    }

    /**
     * Sets this photo as the primary photo.
     */
    fun markAsPrimary() {
        require(isActive) { "Cannot mark inactive photo as primary" }
        isPrimary = true
        updatedAt = Instant.now()
    }

    /**
     * Unsets this photo as primary.
     */
    fun unmarkAsPrimary() {
        isPrimary = false
        updatedAt = Instant.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserPhoto) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
