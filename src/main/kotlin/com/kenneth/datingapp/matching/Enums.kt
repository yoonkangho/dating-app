package com.kenneth.datingapp.matching

/**
 * Status of an Ok (expression of interest).
 */
enum class OkStatus {
    /** Woman sent ok, waiting for man's response */
    PENDING,
    
    /** Man accepted the ok, mutual match achieved */
    ACCEPTED,
    
    /** Man declined the ok */
    DECLINED,
    
    /** Ok expired after 7 days without response */
    EXPIRED,
    
    /** Ok was undone by either party before meeting was scheduled */
    UNDONE
}

/**
 * Status of a meeting arrangement.
 */
enum class MeetingStatus {
    /** Waiting for woman to propose dates */
    PENDING_PROPOSAL,
    
    /** Woman proposed, waiting for man to finalize */
    PENDING_FINALIZATION,
    
    /** Meeting date and location confirmed */
    SCHEDULED,
    
    /** Meeting completed (time passed) */
    COMPLETED,
    
    /** Meeting cancelled */
    CANCELLED
}

/**
 * Reason for meeting cancellation. Used for analytics.
 */
enum class CancellationReason {
    /** Woman did not propose dates within 2 days */
    PROPOSAL_DEADLINE_MISSED,
    
    /** Man did not finalize within 2 days of proposal */
    FINALIZATION_DEADLINE_MISSED,
    
    /** User explicitly cancelled */
    USER_CANCELLED,
    
    /** Woman undid ok before meeting was scheduled */
    UNDO_BY_WOMAN,
    
    /** Man undid ok before meeting was scheduled */
    UNDO_BY_MAN
}

/**
 * Type of meeting event for audit log.
 */
enum class MeetingEventType {
    /** Woman submitted date proposals */
    PROPOSAL_SUBMITTED,
    
    /** Man finalized the meeting */
    MEETING_FINALIZED,
    
    /** One party requested a reschedule (immediately goes to PENDING_PROPOSAL) */
    RESCHEDULE_REQUESTED,
    
    /** Meeting was cancelled */
    MEETING_CANCELLED,
    
    /** Meeting completed successfully */
    MEETING_COMPLETED
}
