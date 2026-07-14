package com.kenneth.datingapp.support

import com.kenneth.datingapp.admin.AdminUser
import com.kenneth.datingapp.admin.AdminRole
import com.kenneth.datingapp.feedback.*
import com.kenneth.datingapp.matching.*
import com.kenneth.datingapp.user.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

object TestFixtures {
    private val counter = AtomicLong(0)

    /** Fixed instant for deterministic time-sensitive test setup. */
    val FIXED_NOW: Instant = Instant.parse("2026-01-15T10:00:00Z")

    /** Deterministic UUID derived from a name — stable across runs. */
    fun uuid(name: String): UUID = UUID.nameUUIDFromBytes(name.toByteArray())

    fun createUser(
        id: UUID = UUID.randomUUID(),
        email: String = "user${counter.incrementAndGet()}@example.com",
        phone: String? = null,
        passwordHash: String = "hashed_password",
        status: UserStatus = UserStatus.VERIFICATION_PENDING
    ) = User(
        id = id,
        email = email,
        phone = phone,
        passwordHash = passwordHash,
        status = status
    )

    fun createProfile(
        id: UUID = UUID.randomUUID(),
        user: User,
        displayName: String = "Test User",
        birthDate: LocalDate = LocalDate.of(1995, 6, 15),
        gender: Gender = Gender.FEMALE,
        heightCm: Int? = 165,
        profession: String? = "Engineer",
        education: Education? = Education.BACHELORS,
        incomeBracket: IncomeBracket? = null,
        assetBracket: AssetBracket? = null,
        bio: String? = "Hello world"
    ) = Profile(
        id = id,
        user = user,
        displayName = displayName,
        birthDate = birthDate,
        gender = gender,
        heightCm = heightCm,
        profession = profession,
        education = education,
        incomeBracket = incomeBracket,
        assetBracket = assetBracket,
        bio = bio
    )

    fun createUserPhoto(
        id: UUID = UUID.randomUUID(),
        profile: Profile,
        url: String = "https://photos.example.com/${UUID.randomUUID()}.jpg",
        displayOrder: Int = 1,
        isPrimary: Boolean = false,
        isActive: Boolean = false
    ) = UserPhoto(
        id = id,
        profile = profile,
        url = url,
        displayOrder = displayOrder,
        isPrimary = isPrimary,
        isActive = isActive
    )

    fun createApprovalRequest(
        id: UUID = UUID.randomUUID(),
        user: User,
        requestType: ApprovalRequestType = ApprovalRequestType.SIGN_UP,
        status: ApprovalRequestStatus = ApprovalRequestStatus.PENDING,
        targetType: ApprovalTargetType? = null,
        targetId: UUID? = null,
        payload: Map<String, Any> = mapOf("display_name" to "Test User")
    ) = ApprovalRequest(
        id = id,
        user = user,
        requestType = requestType,
        status = status,
        targetType = targetType,
        targetId = targetId,
        payload = payload
    )

    fun createAdminUser(
        id: UUID = UUID.randomUUID(),
        email: String = "admin${counter.incrementAndGet()}@example.com",
        name: String = "Admin User",
        passwordHash: String = "hashed_password",
        role: AdminRole = AdminRole.REVIEWER
    ) = AdminUser(
        id = id,
        email = email,
        name = name,
        passwordHash = passwordHash,
        role = role
    )

    fun createOk(
        id: UUID = UUID.randomUUID(),
        sender: User,
        recipient: User,
        status: OkStatus = OkStatus.PENDING,
        sentAt: Instant = FIXED_NOW,
        expiresAt: Instant = sentAt.plus(Duration.ofDays(7))
    ) = Ok(
        id = id,
        sender = sender,
        recipient = recipient,
        status = status,
        sentAt = sentAt,
        expiresAt = expiresAt
    )

    fun createMeeting(
        id: UUID = UUID.randomUUID(),
        ok: Ok,
        woman: User,
        man: User,
        status: MeetingStatus = MeetingStatus.PENDING_PROPOSAL
    ) = Meeting(
        id = id,
        ok = ok,
        woman = woman,
        man = man,
        status = status
    )

    fun createFeedback(
        id: UUID = UUID.randomUUID(),
        meeting: Meeting,
        submittedBy: User,
        aboutUser: User,
        meetingHappened: Boolean = true,
        noShowReason: NoShowReason? = null,
        wantsContactExchange: Boolean = false
    ) = Feedback(
        id = id,
        meeting = meeting,
        submittedBy = submittedBy,
        aboutUser = aboutUser,
        meetingHappened = meetingHappened,
        noShowReason = noShowReason,
        wantsContactExchange = wantsContactExchange
    )

    fun createRating(
        id: UUID = UUID.randomUUID(),
        feedback: Feedback,
        ratedUser: User,
        ratedBy: User,
        looks: Int = 4,
        attitude: Int = 4
    ) = Rating(
        id = id,
        feedback = feedback,
        ratedUser = ratedUser,
        ratedBy = ratedBy,
        looks = looks,
        attitude = attitude
    )

    fun createDispute(
        id: UUID = UUID.randomUUID(),
        meeting: Meeting,
        feedback: Feedback,
        reportedBy: User,
        reportedUser: User,
        status: DisputeStatus = DisputeStatus.OPEN
    ) = Dispute(
        id = id,
        meeting = meeting,
        feedback = feedback,
        reportedBy = reportedBy,
        reportedUser = reportedUser,
        status = status
    )

    fun createMeetingProposal(
        id: UUID = UUID.randomUUID(),
        meeting: Meeting,
        proposedDate: Instant = FIXED_NOW.plus(Duration.ofDays(7)),
        proposedLocation: String = "Coffee Shop"
    ) = MeetingProposal(
        id = id,
        meeting = meeting,
        proposedDate = proposedDate,
        proposedLocation = proposedLocation
    )

    fun createMeetingEvent(
        id: UUID = UUID.randomUUID(),
        meeting: Meeting,
        eventType: MeetingEventType = MeetingEventType.PROPOSAL_SUBMITTED,
        actor: User? = null,
        details: Map<String, Any>? = null
    ) = MeetingEvent(
        id = id,
        meeting = meeting,
        eventType = eventType,
        actor = actor,
        details = details
    )
}
