package com.modelengine.observability.service;

import com.modelengine.observability.dto.ModelServiceDTO;
import com.modelengine.observability.dto.PageDTO;
import com.modelengine.observability.dto.PaginationRequest;
import com.modelengine.observability.dto.PaginationUtils;
import com.modelengine.observability.dto.PodInfoDTO;
import com.modelengine.observability.informer.PodInformer;
import com.modelengine.observability.service.inference.InferenceInstance;
import com.modelengine.observability.service.inference.InferenceService;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
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
    private final PodInformer podInformer;

    private static final int MAX_PODS_PER_SERVICE = 20;

    public PageDTO<ModelServiceDTO> listServices(PaginationRequest request,
                                                  String namespace,
                                                  String framework,
                                                  String status) {
        log.debug("Listing services: page={}, size={}, sort={}.{}, namespace={}, framework={}, status={}",
                request.getPage(), request.getSize(),
                request.getSortField(), request.getSortDirection(),
                namespace, framework, status);

        // 1. Get all instances
        List<InferenceInstance> instances = inferenceService.listInstances();

        // 2. Apply filters
        List<InferenceInstance> filtered = instances.stream()
                .filter(i -> isBlank(namespace) || Objects.equals(i.getNamespace(), namespace))
                .filter(i -> isBlank(framework) || Objects.equals(i.getFramework(), framework))
                .filter(i -> isBlank(status) || Objects.equals(i.getStatus(), status))
                .collect(Collectors.toList());

        // 3. Map to DTOs with pods
        List<ModelServiceDTO> dtos = filtered.stream()
                .map(this::toModelServiceDTO)
                .collect(Collectors.toList());

        // 4. Sort and paginate
        Comparator<ModelServiceDTO> comparator = buildComparator(
                request.getSortField(), request.getSortDirection());
        return PaginationUtils.paginate(dtos, request, comparator);
    }

    private ModelServiceDTO toModelServiceDTO(InferenceInstance instance) {
        List<PodInfoDTO> pods = resolvePods(instance);

        return ModelServiceDTO.builder()
                .instanceName(instance.getInstanceName())
                .status(instance.getStatus())
                .currentReplicas(instance.getCurrentReplicas())
                .desiredReplicas(instance.getDesiredReplicas())
                .address(instance.getAddress())
                .pods(pods)
                .build();
    }

    private List<PodInfoDTO> resolvePods(InferenceInstance instance) {
        Map<String, String> selector = instance.getSelector();
        List<Pod> pods;
        if (selector != null && !selector.isEmpty()) {
            pods = podInformer.listBySelector(selector);
        } else {
            pods = podInformer.listPods();
        }

        return pods.stream()
                .limit(MAX_PODS_PER_SERVICE)
                .map(this::toPodInfoDTO)
                .collect(Collectors.toList());
    }

    private PodInfoDTO toPodInfoDTO(Pod pod) {
        String name = pod.getMetadata() != null ? pod.getMetadata().getName() : null;
        String nodeName = pod.getSpec() != null ? pod.getSpec().getNodeName() : null;
        String ip = pod.getStatus() != null ? pod.getStatus().getPodIP() : null;
        String status = pod.getStatus() != null ? pod.getStatus().getPhase() : null;

        boolean ready = false;
        if (pod.getStatus() != null && pod.getStatus().getConditions() != null) {
            ready = pod.getStatus().getConditions().stream()
                    .anyMatch(c -> "Ready".equals(c.getType()) && "True".equals(c.getStatus()));
        }

        int restartCount = 0;
        if (pod.getStatus() != null && pod.getStatus().getContainerStatuses() != null) {
            restartCount = pod.getStatus().getContainerStatuses().stream()
                    .mapToInt(ContainerStatus::getRestartCount)
                    .sum();
        }

        return PodInfoDTO.builder()
                .name(name)
                .nodeName(nodeName)
                .ip(ip)
                .status(status)
                .ready(ready)
                .restartCount(restartCount)
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
