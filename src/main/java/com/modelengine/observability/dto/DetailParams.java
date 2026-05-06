package com.modelengine.observability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String status;
    private String detail;
}
