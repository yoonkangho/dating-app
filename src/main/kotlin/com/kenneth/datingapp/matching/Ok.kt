package com.kenneth.datingapp.matching

import com.kenneth.datingapp.user.User
import jakarta.persistence.*
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Ok entity representing an expression of interest from a woman to a man.
 * 
 * Business rules:
 * - Woman can have max 3 active oks (PENDING or ACCEPTED with non-completed meeting)
 * - Ok expires after 7 days if man doesn't respond
 * - Either party can undo before meeting is scheduled (incurs penalty)
 */
@Entity
@Table(
    name = "oks",
    indexes = [
        Index(name = "idx_oks_sender_status", columnList = "sender_id, status"),
        Index(name = "idx_oks_recipient_status", columnList = "recipient_id, status"),
        Index(name = "idx_oks_expires_at", columnList = "expires_at")
    ]
)
class Ok(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    val recipient: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OkStatus = OkStatus.PENDING,

    @Column(name = "sent_at", nullable = false)
    val sentAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant = Instant.now().plus(EXPIRATION_DURATION),

    @Column(name = "responded_at")
    var respondedAt: Instant? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "undone_by")
    var undoneBy: User? = null,

    @Column(name = "undone_at")
    var undoneAt: Instant? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at")
    var updatedAt: Instant? = null
) {
    companion object {
        val EXPIRATION_DURATION: Duration = Duration.ofDays(7)
    }

    /**
     * Checks if this ok has expired.
     */
    fun isExpired(now: Instant): Boolean =
        status == OkStatus.PENDING && now.isAfter(expiresAt)

    /**
     * Accepts the ok (man responds positively).
     */
    fun accept(now: Instant) {
        require(status == OkStatus.PENDING) { "Can only accept PENDING ok, current: $status" }
        require(!isExpired(now)) { "Cannot accept expired ok" }
        status = OkStatus.ACCEPTED
        respondedAt = now
        updatedAt = now
    }

    /**
     * Declines the ok (man responds negatively).
     */
    fun decline(now: Instant) {
        require(status == OkStatus.PENDING) { "Can only decline PENDING ok, current: $status" }
        status = OkStatus.DECLINED
        respondedAt = now
        updatedAt = now
    }

    /**
     * Marks ok as expired (called by scheduled job).
     */
    fun markExpired(now: Instant) {
        require(status == OkStatus.PENDING) { "Can only expire PENDING ok, current: $status" }
        require(isExpired(now)) { "Ok has not expired yet" }
        status = OkStatus.EXPIRED
        updatedAt = now
    }

    /**
     * Undoes the ok (either party withdraws before meeting is scheduled).
     */
    fun undo(by: User, now: Instant) {
        require(status == OkStatus.PENDING || status == OkStatus.ACCEPTED) {
            "Can only undo PENDING or ACCEPTED ok, current: $status"
        }
        require(by.id == sender.id || by.id == recipient.id) {
            "Only sender or recipient can undo"
        }
        status = OkStatus.UNDONE
        undoneBy = by
        undoneAt = now
        updatedAt = now
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ok) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
