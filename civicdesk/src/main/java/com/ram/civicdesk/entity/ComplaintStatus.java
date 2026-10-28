package com.ram.civicdesk.entity;

import java.util.EnumSet;
import java.util.Set;

public enum ComplaintStatus {
    SUBMITTED,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    REOPENED;

    /** Statuses that still need action from an officer. */
    public static final Set<ComplaintStatus> OPEN = EnumSet.of(SUBMITTED, ASSIGNED, IN_PROGRESS, REOPENED);

    public boolean isOpen() {
        return OPEN.contains(this);
    }
}
