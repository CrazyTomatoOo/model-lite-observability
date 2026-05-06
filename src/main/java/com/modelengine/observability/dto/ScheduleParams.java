package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing scheduling parameters.
 * Matches the ScheduleParams schema from the OpenAPI spec.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleParams {

    private String priorityClass;
    private Boolean enableSpreadPolicy;
    private Boolean enablePreemption;
    private Boolean hpaSwitch;
    private Integer minReplicaCount;
    private Integer maxReplicaCount;
    private Integer cooldownPeriod;
    private List<HpaRule> hpaRules;
    private String queueName;
}
