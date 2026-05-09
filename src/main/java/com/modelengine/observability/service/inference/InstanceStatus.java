package com.modelengine.observability.service.inference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of possible instance statuses.
 */
public enum InstanceStatus {
    PENDING("Pending"),
    RUNNING("Running"),
    FAILED("Failed"),
    UNKNOWN("Unknown"),
    SUCCEEDED("Succeeded"),
    TERMINATING("Terminating");

    private final String displayName;

    InstanceStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses a status string into an InstanceStatus enum value.
     * Case-insensitive matching.
     *
     * @param status the status string
     * @return the matching InstanceStatus, or UNKNOWN if not found
     */
    @JsonCreator
    public static InstanceStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return UNKNOWN;
        }
        for (InstanceStatus s : values()) {
            if (s.displayName.equalsIgnoreCase(status)) {
                return s;
            }
        }
        return UNKNOWN;
    }
}
