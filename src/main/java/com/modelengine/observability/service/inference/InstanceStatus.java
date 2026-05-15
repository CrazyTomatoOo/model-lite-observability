package com.modelengine.observability.service.inference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of inference service statuses.
 * Covers the full lifecycle: starting → available → stopping → stopped, etc.
 */
public enum InstanceStatus {
    PARTIAL_RUNNING,
    WAITING,
    AVAILABLE,
    UNAVAILABLE,
    STOPPED,
    STOPPING,
    DELETING,
    STARTING;

    @JsonValue
    public String getDisplayName() {
        return name();
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
            if (s.name().equalsIgnoreCase(status)) {
                return s;
            }
        }
        return UNAVAILABLE;
    }
}
