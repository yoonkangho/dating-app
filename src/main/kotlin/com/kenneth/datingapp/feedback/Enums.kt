package com.kenneth.datingapp.feedback

/**
 * Reason why meeting did not happen.
 */
enum class NoShowReason {
    /** Submitter did not show up */
    I_DID_NOT_SHOW,
    
    /** Counterparty did not show up (triggers dispute) */
    THEY_DID_NOT_SHOW,
    
    /** Both parties agreed to cancel */
    MUTUAL_CANCEL,
    
    /** Other reason */
    OTHER
}

/**
 * Red flag categories that can be reported in feedback.
 * Stored as array in database, allowing multiple selections.
 */
enum class RedFlagCategory {
    HARASSMENT,
    MISREPRESENTATION,
    INAPPROPRIATE_BEHAVIOR,
    MADE_UNCOMFORTABLE,
    PHOTOS_NOT_ACCURATE,
    OFFENSIVE_LANGUAGE,
    OTHER
}

/**
 * Status of a dispute investigation.
 */
enum class DisputeStatus {
    /** Dispute opened, awaiting investigation */
    OPEN,
    
    /** Admin is investigating */
    INVESTIGATING,
    
    /** Dispute resolved */
    RESOLVED
}

/**
 * Resolution outcome of a dispute.
 */
enum class DisputeResolution {
    /** No action taken */
    NO_ACTION,
    
    /** Warning issued to reported user */
    WARNING_ISSUED,
    
    /** Reported user banned */
    USER_BANNED,
    
    /** Not enough evidence to determine */
    INSUFFICIENT_EVIDENCE
}
