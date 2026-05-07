package com.modelengine.observability.service;

import com.modelengine.observability.dto.ModelServiceDTO;
import com.modelengine.observability.dto.PageDTO;
import com.modelengine.observability.dto.PaginationRequest;
import com.modelengine.observability.dto.PodInfoDTO;
import com.modelengine.observability.service.inference.InferenceInstance;
import com.modelengine.observability.service.inference.InferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelServiceServiceTest {

    @Mock
    private InferenceService inferenceService;


    private ModelServiceService modelServiceService;

    @BeforeEach
    void setUp() {
        modelServiceService = new ModelServiceService(inferenceService);
    }

    private InferenceInstance createInstance(String name, String namespace, String status,
                                              String framework, int currentReplicas, int desiredReplicas) {
        return InferenceInstance.builder()
                .instanceName(name)
                .namespace(namespace)
                .status(status)
                .framework(framework)
                .currentReplicas(currentReplicas)
                .desiredReplicas(desiredReplicas)
                .selector(Map.of("app", name))
                .build();
    }

    private PaginationRequest defaultRequest() {
        return PaginationRequest.builder()
                .page(1)
                .size(20)
                .sortField("instanceName")
                .sortDirection("desc")
                .build();
    }

    // ───────── Response structure ─────────

    @Test
    void responseHasCorrectStructure() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 3, 2)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertNotNull(result.getData());
        assertEquals(1, result.getPage());
        assertEquals(20, result.getSize());
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPages());
    }

    @Test
    void eachItemHasInstanceName() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 2, 3)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertEquals(1, result.getData().size());
        assertEquals("svc-a", result.getData().get(0).getInstanceName());
        assertEquals("Running", result.getData().get(0).getStatus());
        assertEquals(2, result.getData().get(0).getCurrentReplicas());
        assertEquals(3, result.getData().get(0).getDesiredReplicas());
    }

    @Test
    void metricsFieldIsNull() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 3, 2)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertNull(result.getData().get(0).getMetrics());
    }

    @Test
    void nestedCrdsFieldsAreNull() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 3, 2)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        ModelServiceDTO dto = result.getData().get(0);
        assertNull(dto.getModelMeta());
        assertNull(dto.getDeviceParams());
        assertNull(dto.getFrameworkParams());
        assertNull(dto.getEnv());
        assertNull(dto.getStorageParams());
        assertNull(dto.getResourceParams());
        assertNull(dto.getDeployParams());
        assertNull(dto.getScheduleParams());
        assertNull(dto.getAdditionalParams());
        assertNull(dto.getReason());
        assertNull(dto.getDetails());
    }

    // ───────── Pagination defaults ─────────

    @Test
    void defaultPaginationParams() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 3, 2)
        ));

        PaginationRequest req = PaginationRequest.builder()
                .page(1)
                .size(20)
                .sortField("instanceName")
                .sortDirection("desc")
                .build();

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(req, null, null, null);

        assertEquals(1, result.getPage());
        assertEquals(20, result.getSize());
    }

    @Test
    void defaultSortIsInstanceNameDesc() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-c", "ns1", "Running", "VLLM", 1, 1),
                createInstance("svc-a", "ns1", "Running", "VLLM", 1, 1),
                createInstance("svc-b", "ns1", "Running", "VLLM", 1, 1)
        ));

        PaginationRequest req = PaginationRequest.builder()
                .page(1)
                .size(20)
                .sortField("instanceName")
                .sortDirection("desc")
                .build();

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(req, null, null, null);

        assertEquals(3, result.getData().size());
        assertEquals("svc-c", result.getData().get(0).getInstanceName());
        assertEquals("svc-b", result.getData().get(1).getInstanceName());
        assertEquals("svc-a", result.getData().get(2).getInstanceName());
    }

    @Test
    void sortAscending() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-c", "ns1", "Running", "VLLM", 1, 1),
                createInstance("svc-a", "ns1", "Running", "VLLM", 1, 1),
                createInstance("svc-b", "ns1", "Running", "VLLM", 1, 1)
        ));

        PaginationRequest req = PaginationRequest.builder()
                .page(1)
                .size(20)
                .sortField("instanceName")
                .sortDirection("asc")
                .build();

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(req, null, null, null);

        assertEquals("svc-a", result.getData().get(0).getInstanceName());
        assertEquals("svc-b", result.getData().get(1).getInstanceName());
        assertEquals("svc-c", result.getData().get(2).getInstanceName());
    }

    // ───────── Filtering ─────────

    @Test
    void filterByNamespace() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 1, 1),
                createInstance("svc-b", "ns2", "Running", "VLLM", 1, 1)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), "ns1", null, null);

        assertEquals(1, result.getData().size());
        assertEquals("svc-a", result.getData().get(0).getInstanceName());
    }

    @Test
    void filterByFramework() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 1, 1),
                createInstance("svc-b", "ns2", "Running", "SGLang", 1, 1)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, "VLLM", null);

        assertEquals(1, result.getData().size());
        assertEquals("svc-a", result.getData().get(0).getInstanceName());
    }

    @Test
    void filterByStatus() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 1, 1),
                createInstance("svc-b", "ns2", "Pending", "VLLM", 1, 1)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, "Running");

        assertEquals(1, result.getData().size());
        assertEquals("svc-a", result.getData().get(0).getInstanceName());
    }

    @Test
    void filterByNamespaceAndStatus() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 1, 1),
                createInstance("svc-b", "ns1", "Pending", "VLLM", 1, 1),
                createInstance("svc-c", "ns2", "Running", "VLLM", 1, 1)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), "ns1", null, "Running");

        assertEquals(1, result.getData().size());
        assertEquals("svc-a", result.getData().get(0).getInstanceName());
    }

    @Test
    void emptyFilterReturnsAll() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 1, 1)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), "", "", "");

        assertEquals(1, result.getData().size());
    }

    // ───────── Empty result ─────────

    @Test
    void emptyInstancesReturnsEmptyPage() {
        when(inferenceService.listInstances()).thenReturn(List.of());

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertTrue(result.getData().isEmpty());
        assertEquals(0L, result.getTotal());
        assertEquals(0, result.getPages());
    }

    // ───────── Pods ─────────

    @Test
    void podsGeneratedFromReplicas() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 3, 2)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        List<PodInfoDTO> pods = result.getData().get(0).getPods();
        assertNotNull(pods);
        assertEquals(3, pods.size());
        assertEquals("svc-a-0", pods.get(0).getName());
        assertEquals("node-1", pods.get(0).getNodeName());
        assertEquals("10.1.0.10", pods.get(0).getIp());
        assertEquals("Running", pods.get(0).getStatus());
        assertTrue(pods.get(0).getReady());
        assertEquals(0, pods.get(0).getRestartCount());
        assertEquals("svc-a-1", pods.get(1).getName());
        assertEquals("svc-a-2", pods.get(2).getName());
    }

    @Test
    void podsCountMatchesReplicas() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 25, 25)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertEquals(25, result.getData().get(0).getPods().size());
    }

    @Test
    void podsGeneratedWhenNoSelector() {
        InferenceInstance instance = InferenceInstance.builder()
                .instanceName("svc-a")
                .namespace("ns1")
                .status("Running")
                .framework("VLLM")
                .currentReplicas(1)
                .desiredReplicas(1)
                .build(); // no selector

        when(inferenceService.listInstances()).thenReturn(List.of(instance));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertEquals(1, result.getData().get(0).getPods().size());
    }

    // ───────── Pagination edge cases ─────────

    @Test
    void paginationSecondPage() {
        java.util.List<InferenceInstance> instances = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) {
            instances.add(createInstance("svc-" + i, "ns1", "Running", "VLLM", 1, 1));
        }
        when(inferenceService.listInstances()).thenReturn(instances);

        PaginationRequest req = PaginationRequest.builder()
                .page(2)
                .size(20)
                .sortField("instanceName")
                .sortDirection("asc")
                .build();

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(req, null, null, null);

        assertEquals(25L, result.getTotal());
        assertEquals(2, result.getPages());
        assertEquals(5, result.getData().size());
        assertEquals(2, result.getPage());
    }

    @Test
    void pageBeyondTotalReturnsEmpty() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 1, 1)
        ));

        PaginationRequest req = PaginationRequest.builder()
                .page(5)
                .size(20)
                .sortField("instanceName")
                .sortDirection("desc")
                .build();

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(req, null, null, null);

        assertTrue(result.getData().isEmpty());
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPages());
    }

    // ───────── Helper ─────────

}
