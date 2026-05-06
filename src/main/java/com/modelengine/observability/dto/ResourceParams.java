package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing resource parameters (used by custom frameworks).
 * Matches the ResourceParams schema from the OpenAPI spec.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceParams {

    private String cpu;
    private String memory;
    private String devShmSizeLimit;
}
