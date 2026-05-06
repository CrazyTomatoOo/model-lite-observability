package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * DTO representing health status response.
 * Matches the health check response schema from the OpenAPI spec.
 * Components are flat strings ("connected" / "disconnected"), not nested objects.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatusDTO {

    private String status;
    private String version;
    private Instant timestamp;
    private String clusterId;
    private String meVersion;
    private Map<String, String> components;
}
