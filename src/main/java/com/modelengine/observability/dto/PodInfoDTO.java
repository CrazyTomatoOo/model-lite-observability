package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.modelengine.observability.service.inference.PodStatus;

/**
 * DTO representing Pod information.
 * Matches the PodInfo schema from the OpenAPI spec.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PodInfoDTO {

    private String name;
    private String nodeName;
    private String ip;
    private PodStatus status;
    private Boolean ready;
    private Integer restartCount;
    private String metricsEndpoint;
}
