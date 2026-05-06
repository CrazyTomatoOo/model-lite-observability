package com.modelengine.observability.controller;

import com.modelengine.observability.dto.ApiResponse;
import com.modelengine.observability.dto.ModelServiceDTO;
import com.modelengine.observability.dto.PageDTO;
import com.modelengine.observability.dto.PaginationRequest;
import com.modelengine.observability.dto.PaginationUtils;
import com.modelengine.observability.exception.ErrorCode;
import com.modelengine.observability.exception.ObservabilityException;
import com.modelengine.observability.service.ModelServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/model-services")
@RequiredArgsConstructor
public class ModelServiceController {

    private final ModelServiceService modelServiceService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageDTO<ModelServiceDTO>>> listServices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "instanceName,desc") String sort,
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) String framework,
            @RequestParam(required = false) String status) {

        // Validate pagination params
        if (page < 1) {
            throw ObservabilityException.badRequest("page must be >= 1");
        }
        if (size < 1 || size > 100) {
            throw ObservabilityException.badRequest("size must be between 1 and 100");
        }

        PaginationUtils.Sort parsedSort = PaginationUtils.parseSort(sort);

        PaginationRequest request = PaginationRequest.builder()
                .page(page)
                .size(size)
                .sortField(parsedSort.field())
                .sortDirection(parsedSort.direction())
                .build();

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                request, namespace, framework, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
