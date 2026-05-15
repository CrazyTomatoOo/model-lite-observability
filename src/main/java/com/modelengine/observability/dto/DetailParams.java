package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.modelengine.observability.service.inference.PodStatus;
/**
 * DTO representing detailed status information.
 * Matches the DetailParams schema from the OpenAPI spec.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetailParams {

    private String group;
    private String name;
    private PodStatus status;
    private String detail;
}
