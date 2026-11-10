package com.mentormatching.mentor.entity;

/** Local read copy of auth-service account status; auth remains the source of truth. */
public enum AccountStatus {
    PENDING_VERIFICATION,
    ACTIVE,
    LOCKED,
    DISABLED
}
