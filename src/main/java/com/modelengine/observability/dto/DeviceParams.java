package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO representing device parameters.
 * Matches the DeviceParams schema from the OpenAPI spec.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceParams {

    private String chipName;
    private Integer nums;
    private String memorySize;
    private Integer vgpuCore;
    private String vgpuMem;
    private Boolean isVirtual;
    private List<String> nodeInfo;
    private List<String> resourceName;
    private Map<String, String> volcanoLabels;
    private Map<String, String> labels;
}
