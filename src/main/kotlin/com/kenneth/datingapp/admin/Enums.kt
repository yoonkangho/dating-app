package com.kenneth.datingapp.admin

/**
 * Admin user role with different permission levels.
 */
enum class AdminRole {
    /** Can approve/reject approval requests (sign-ups, photos, bio changes) */
    REVIEWER,
    
    /** REVIEWER + can resolve disputes and warn users */
    MODERATOR,
    
    /** All permissions + can ban/unban users, reactivate rejected users, manage admins, release waitlist */
    SUPER_ADMIN
}

/**
 * Type of admin action for audit logging.
 */
enum class AdminActionType {
    APPROVAL_REQUEST_APPROVED,
    APPROVAL_REQUEST_REJECTED,
    USER_RELEASED_FROM_WAITLIST,
    USER_WARNED,
    USER_BANNED,
    USER_UNBANNED,
    USER_REACTIVATED,
    DISPUTE_RESOLVED,
    DISPUTE_REOPENED
}

/**
 * Type of entity targeted by an admin action.
 */
enum class AdminTargetType {
    USER,
    APPROVAL_REQUEST,
    DISPUTE
}
