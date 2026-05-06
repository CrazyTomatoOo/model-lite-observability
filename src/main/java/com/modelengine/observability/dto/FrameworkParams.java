package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO representing framework parameters.
 * Matches the FrameworkParams schema from the OpenAPI spec.
 *
 * Note: 'name' is String (not enum) for extensibility.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FrameworkParams {

    private String name;
    private String version;
    private String imageName;
    private Map<String, String> frameworkParam;
}
