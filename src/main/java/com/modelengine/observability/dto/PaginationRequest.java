package com.modelengine.observability.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Pagination request parameters.
 * <p>
 * Page starts at 1, size is 1-100, sort direction is asc or desc.
 * Default sort: instanceName,desc
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationRequest {

    @Min(1)
    @Builder.Default
    private int page = 1;

    @Min(1)
    @Max(100)
    @Builder.Default
    private int size = 20;

    private String sortField;

    @Pattern(regexp = "asc|desc", message = "sortDirection must be 'asc' or 'desc'")
    @Builder.Default
    private String sortDirection = "desc";
}
