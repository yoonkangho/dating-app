package com.kenneth.datingapp.support

import com.kenneth.datingapp.admin.AdminUser
import com.kenneth.datingapp.admin.AdminRole
import com.kenneth.datingapp.user.*
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

object TestFixtures {
    private val counter = AtomicLong(0)

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
}
