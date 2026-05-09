package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import com.modelengine.observability.service.inference.InstanceStatus;

/**
 * DTO representing model service information.
 * Matches the ModelService schema from the OpenAPI spec.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelServiceDTO {

    private String instanceName;
    private String userResourceGroupId;
    private String userResourceGroupName;
    private ModelMetaParams modelMeta;
    private MetricParams metrics;
    private InstanceStatus status;
    private Integer currentReplicas;
    private Integer desiredReplicas;
    private String address;
    private DeviceParams deviceParams;
    private FrameworkParams frameworkParams;
    private Map<String, String> env;
    private StorageParams storageParams;
    private ResourceParams resourceParams;
    private DeployParams deployParams;
    private ScheduleParams scheduleParams;
    private Map<String, String> additionalParams;
    private String reason;
    private List<DetailParams> details;
    private List<PodInfoDTO> pods;
}
