package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing storage parameters.
 * Matches the StorageParams schema from the OpenAPI spec.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageParams {

    private Boolean useExistingPvc;
    private List<String> existingPvcNames;
    private String storageClass;
    private String storageCapacity;
}
