package com.modelengine.observability.service;

import com.modelengine.observability.dto.ModelServiceDTO;
import com.modelengine.observability.dto.PageDTO;
import com.modelengine.observability.dto.PaginationRequest;
import com.modelengine.observability.dto.PaginationUtils;
import com.modelengine.observability.dto.PodInfoDTO;
import com.modelengine.observability.service.inference.InferenceInstance;
import com.modelengine.observability.service.inference.InferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class ModelServiceService {

    private final InferenceService inferenceService;

    public PageDTO<ModelServiceDTO> listServices(PaginationRequest request,
                                                  String namespace,
                                                  String framework,
                                                  String status) {
        log.debug("Listing services: page={}, size={}, namespace={}, framework={}, status={}",
                request.getPage(), request.getSize(), namespace, framework, status);

        List<InferenceInstance> instances = inferenceService.listInstances();

        List<InferenceInstance> filtered = instances.stream()
                .filter(i -> isBlank(namespace) || Objects.equals(i.getNamespace(), namespace))
                .filter(i -> isBlank(framework) || Objects.equals(i.getFramework(), framework))
                .filter(i -> isBlank(status) || Objects.equals(i.getStatus(), status))
                .collect(Collectors.toList());

        List<ModelServiceDTO> dtos = filtered.stream()
                .map(this::toModelServiceDTO)
                .collect(Collectors.toList());

        Comparator<ModelServiceDTO> comparator = buildComparator(
                request.getSortField(), request.getSortDirection());
        return PaginationUtils.paginate(dtos, request, comparator);
    }

    private ModelServiceDTO toModelServiceDTO(InferenceInstance instance) {
        int n = instance.getCurrentReplicas() != null ? instance.getCurrentReplicas() : 1;
        List<PodInfoDTO> pods = java.util.stream.IntStream.range(0, n)
                .mapToObj(i -> PodInfoDTO.builder()
                        .name(instance.getInstanceName() + "-" + i)
                        .nodeName("node-" + (i + 1))
                        .ip("10.1.0." + (10 + i))
                        .status("Running")
                        .ready(true)
                        .restartCount(0)
                        .build())
                .collect(Collectors.toList());

        return ModelServiceDTO.builder()
                .instanceName(instance.getInstanceName())
                .status(instance.getStatus())
                .currentReplicas(instance.getCurrentReplicas())
                .desiredReplicas(instance.getDesiredReplicas())
                .address(instance.getAddress())
                .pods(pods)
                .build();
    }

    private Comparator<ModelServiceDTO> buildComparator(String field, String direction) {
        Comparator<ModelServiceDTO> comparator = switch (field != null ? field : "instanceName") {
            case "instanceName" -> Comparator.comparing(
                    ModelServiceDTO::getInstanceName, Comparator.nullsLast(String::compareTo));
            case "status" -> Comparator.comparing(
                    ModelServiceDTO::getStatus, Comparator.nullsLast(String::compareTo));
            default -> Comparator.comparing(
                    ModelServiceDTO::getInstanceName, Comparator.nullsLast(String::compareTo));
        };
        return "asc".equalsIgnoreCase(direction) ? comparator : comparator.reversed();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
