package com.modelengine.observability.service.inference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of possible pod statuses.
 */
public enum PodStatus {
    INSUFFICIENT_RESOURCE("InsufficientResource"),
    UNSCHEDULABLE("Unschedulable"),
    IMAGE_PULL_FAILURE("ImagePullFailure"),
    ERROR("Error"),
    TERMINATING("Terminating"),
    STARTING("Starting"),
    HEALTHY("Healthy"),
    MOUNT_FAILURE("MountFailure");

    private final String displayName;

    PodStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parses a status string into a PodStatus enum value.
     * Case-insensitive matching.
     *
     * @param status the status string
     * @return the matching PodStatus, or ERROR if not found
     */
    @JsonCreator
    public static PodStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return ERROR;
        }
        for (PodStatus s : values()) {
            if (s.displayName.equalsIgnoreCase(status)) {
                return s;
            }
        }
        return ERROR;
    }
}
