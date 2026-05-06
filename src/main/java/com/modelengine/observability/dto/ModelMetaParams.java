package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing model metadata.
 * Matches the ModelMetaParams schema from the OpenAPI spec.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelMetaParams {

    private String modelType;
    private String modelCategory;
    private String modelName;
    private String version;
    private String modelOwnerGroupId;
}
