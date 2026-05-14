package com.modelengine.observability.service.inference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of inference service statuses.
 * Covers the full lifecycle: starting → available → stopping → stopped, etc.
 */
public enum InstanceStatus {
    PARTIAL_RUNNING("PartialRunning"),
    WAITING("Waiting"),
    AVAILABLE("Available"),
    UNAVAILABLE("Unavailable"),
    STOPPED("Stopped"),
    STOPPING("Stopping"),
    DELETING("Deleting"),
    STARTING("Starting");

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
     * @return the matching InstanceStatus, or UNAVAILABLE if not found
     */
    @JsonCreator
    public static InstanceStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return UNAVAILABLE;
        }
        for (InstanceStatus s : values()) {
            if (s.displayName.equalsIgnoreCase(status)) {
                return s;
            }
        }
        return UNAVAILABLE;
    }
}
