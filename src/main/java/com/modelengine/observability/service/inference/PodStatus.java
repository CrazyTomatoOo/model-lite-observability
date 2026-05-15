package com.modelengine.observability.service.inference;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of possible pod statuses.
 */
public enum PodStatus {
    INSUFFICIENT_RESOURCE,
    UNSCHEDULABLE,
    IMAGE_PULL_FAILURE,
    ERROR,
    TERMINATING,
    STARTING,
    HEALTHY,
    MOUNT_FAILURE;

    @JsonValue
    public String getDisplayName() {
        return name();
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
            if (s.name().equalsIgnoreCase(status)) {
                return s;
            }
        }
        return ERROR;
    }
}
