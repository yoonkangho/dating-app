package com.kenneth.datingapp.user

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * User profile entity. Contains all profile information visible to other users.
 * 
 * Immutable fields (set once at sign-up, cannot be changed):
 * - displayName, birthDate, gender, heightCm, profession, education, incomeBracket, assetBracket
 * 
 * Mutable fields (can be changed via ApprovalRequest):
 * - bio
 * 
 * Reputation fields (set by system based on user behavior):
 * - hasUndoneOk, hasLetOkExpire
 */
@Entity
@Table(
    name = "profiles",
    indexes = [
        Index(name = "idx_profiles_user_id", columnList = "user_id", unique = true),
        Index(name = "idx_profiles_gender", columnList = "gender")
    ]
)
class Profile(
    @Id
    val id: UUID = UUID.randomUUID(),

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    // -- Immutable fields --
    
    @Column(name = "display_name", nullable = false, length = 100)
    val displayName: String,

    @Column(name = "birth_date", nullable = false)
    val birthDate: LocalDate,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    val gender: Gender,

    @Column(name = "height_cm")
    val heightCm: Int? = null,

    @Column(length = 100)
    val profession: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val education: Education? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "income_bracket", length = 20)
    val incomeBracket: IncomeBracket? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_bracket", length = 20)
    val assetBracket: AssetBracket? = null,

    // -- Mutable fields --
    
    @Column(columnDefinition = "TEXT")
    var bio: String? = null,

    // -- Reputation fields --
    
    @Column(name = "has_undone_ok", nullable = false)
    var hasUndoneOk: Boolean = false,

    @Column(name = "has_let_ok_expire", nullable = false)
    var hasLetOkExpire: Boolean = false,

    // -- Timestamps --
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null
) {
    /**
     * Updates bio. Should only be called after ApprovalRequest is approved.
     */
    fun updateBio(newBio: String?) {
        bio = newBio
        updatedAt = Instant.now()
    }

    /**
     * Marks that user has undone an Ok.
     */
    fun markUndoneOk() {
        if (!hasUndoneOk) {
            hasUndoneOk = true
            updatedAt = Instant.now()
        }
    }

    /**
     * Marks that user has let an Ok expire.
     */
    fun markLetOkExpire() {
        if (!hasLetOkExpire) {
            hasLetOkExpire = true
            updatedAt = Instant.now()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Profile) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
