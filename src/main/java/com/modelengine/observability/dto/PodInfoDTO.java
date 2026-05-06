package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String status;
    private Boolean ready;
    private Integer restartCount;
    private String metricsEndpoint;
}
