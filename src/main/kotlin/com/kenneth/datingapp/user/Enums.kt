package com.kenneth.datingapp.user

/**
 * User account status in the application lifecycle.
 */
enum class UserStatus {
    /** Identity verification in progress */
    VERIFICATION_PENDING,
    
    /** Verified, waiting to be released for admin review */
    WAITLISTED,
    
    /** Released from waitlist, pending admin approval */
    REVIEW_PENDING,
    
    /** Approved and active */
    ACTIVE,
    
    /** Rejected (shown as pending to user) */
    REJECTED,
    
    /** Banned by admin */
    BANNED
}

enum class Gender {
    MALE,
    FEMALE
}

enum class Education {
    HIGH_SCHOOL,
    BACHELORS,
    MASTERS,
    DOCTORATE
}

enum class IncomeBracket {
    UNDER_50K,
    `50K_100K`,
    `100K_150K`,
    `150K_200K`,
    OVER_200K
}

enum class AssetBracket {
    UNDER_100K,
    `100K_500K`,
    `500K_1M`,
    `1M_5M`,
    OVER_5M
}

enum class ApprovalRequestType {
    SIGN_UP,
    PHOTO_ADD,
    BIO_CHANGE
}

enum class ApprovalRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

enum class ApprovalTargetType {
    PROFILE,
    PHOTO
}
