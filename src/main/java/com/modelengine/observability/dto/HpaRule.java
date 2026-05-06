package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single HPA scaling rule.
 * Matches the HpaRule schema from the OpenAPI spec.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HpaRule {

    private String name;
    private String type;
    private Boolean enabled;
    private String timezone;
    private String start;
    private String end;
    private Integer desiredReplicas;
    private Double targetValue;
}
