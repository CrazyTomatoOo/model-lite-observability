package com.modelengine.observability.service;

import com.modelengine.observability.dto.ModelServiceDTO;
import com.modelengine.observability.dto.PageDTO;
import com.modelengine.observability.dto.PaginationRequest;
import com.modelengine.observability.dto.PodInfoDTO;
import com.modelengine.observability.service.inference.InferenceInstance;
import com.modelengine.observability.service.inference.InferenceService;
import com.modelengine.observability.service.inference.InstanceStatus;
import com.modelengine.observability.service.inference.PodStatus;
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

    private InferenceInstance createInstance(String name, String namespace, InstanceStatus status,
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
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 3, 2)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertNotNull(result.getRecords());
        assertEquals(1, result.getPage());
        assertEquals(20, result.getSize());
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPages());
    }

    @Test
    void eachItemHasInstanceName() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 2, 3)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertEquals(1, result.getRecords().size());
        assertEquals("svc-a", result.getRecords().get(0).getInstanceName());
        assertEquals(InstanceStatus.AVAILABLE, result.getRecords().get(0).getStatus());
        assertEquals(2, result.getRecords().get(0).getCurrentReplicas());
        assertEquals(3, result.getRecords().get(0).getDesiredReplicas());
    }

    @Test
    void metricsFieldIsNotNull() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 3, 2)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertNotNull(result.getRecords().get(0).getMetrics());
        assertEquals(1000, result.getRecords().get(0).getMetrics().getRequests());
    }
    void metricsFieldIsNull() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 3, 2)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertNull(result.getRecords().get(0).getMetrics());
    }

    @Test
    void nestedCrdsFieldsAreNotNull() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 3, 2)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        ModelServiceDTO dto = result.getRecords().get(0);
        assertNotNull(dto.getModelMeta());
        assertNotNull(dto.getDeviceParams());
        assertNotNull(dto.getFrameworkParams());
        assertNotNull(dto.getEnv());
        assertNotNull(dto.getStorageParams());
        assertNotNull(dto.getResourceParams());
        assertNotNull(dto.getDeployParams());
        assertNotNull(dto.getScheduleParams());
        assertNotNull(dto.getAdditionalParams());
        assertNotNull(dto.getReason());
        assertNotNull(dto.getDetails());
    }
    void nestedCrdsFieldsAreNull() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 3, 2)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        ModelServiceDTO dto = result.getRecords().get(0);
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
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 3, 2)
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
                createInstance("svc-c", "ns1", InstanceStatus.AVAILABLE, "MindIE", 1, 1),
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 1, 1),
                createInstance("svc-b", "ns1", InstanceStatus.AVAILABLE, "MindIE", 1, 1)
        ));

        PaginationRequest req = PaginationRequest.builder()
                .page(1)
                .size(20)
                .sortField("instanceName")
                .sortDirection("desc")
                .build();

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(req, null, null, null);

        assertEquals(3, result.getRecords().size());
        assertEquals("svc-c", result.getRecords().get(0).getInstanceName());
        assertEquals("svc-b", result.getRecords().get(1).getInstanceName());
        assertEquals("svc-a", result.getRecords().get(2).getInstanceName());
    }

    @Test
    void sortAscending() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-c", "ns1", InstanceStatus.AVAILABLE, "MindIE", 1, 1),
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 1, 1),
                createInstance("svc-b", "ns1", InstanceStatus.AVAILABLE, "MindIE", 1, 1)
        ));

        PaginationRequest req = PaginationRequest.builder()
                .page(1)
                .size(20)
                .sortField("instanceName")
                .sortDirection("asc")
                .build();

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(req, null, null, null);

        assertEquals("svc-a", result.getRecords().get(0).getInstanceName());
        assertEquals("svc-b", result.getRecords().get(1).getInstanceName());
        assertEquals("svc-c", result.getRecords().get(2).getInstanceName());
    }

    // ───────── Filtering ─────────

    @Test
    void filterByNamespace() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 1, 1),
                createInstance("svc-b", "ns2", InstanceStatus.AVAILABLE, "MindIE", 1, 1)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), "ns1", null, null);

        assertEquals(1, result.getRecords().size());
        assertEquals("svc-a", result.getRecords().get(0).getInstanceName());
    }

    @Test
    void filterByFramework() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 1, 1),
                createInstance("svc-b", "ns2", InstanceStatus.AVAILABLE, "OtherFramework", 1, 1)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, "MindIE", null);

        assertEquals(1, result.getRecords().size());
        assertEquals("svc-a", result.getRecords().get(0).getInstanceName());
    }

    @Test
    void filterByStatus() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 1, 1),
                createInstance("svc-b", "ns2", InstanceStatus.WAITING, "MindIE", 1, 1)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, "AVAILABLE");

        assertEquals(1, result.getRecords().size());
        assertEquals("svc-a", result.getRecords().get(0).getInstanceName());
    }

    @Test
    void filterByNamespaceAndStatus() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 1, 1),
                createInstance("svc-b", "ns1", InstanceStatus.WAITING, "MindIE", 1, 1),
                createInstance("svc-c", "ns2", InstanceStatus.AVAILABLE, "MindIE", 1, 1)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), "ns1", null, "AVAILABLE");

        assertEquals(1, result.getRecords().size());
        assertEquals("svc-a", result.getRecords().get(0).getInstanceName());
    }

    @Test
    void emptyFilterReturnsAll() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 1, 1)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), "", "", "");

        assertEquals(1, result.getRecords().size());
    }

    // ───────── Empty result ─────────

    @Test
    void emptyInstancesReturnsEmptyPage() {
        when(inferenceService.listInstances()).thenReturn(List.of());

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertTrue(result.getRecords().isEmpty());
        assertEquals(0L, result.getTotal());
        assertEquals(0, result.getPages());
    }

    // ───────── Pods ─────────

    @Test
    void podsGeneratedFromReplicas() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 3, 2)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        List<PodInfoDTO> pods = result.getRecords().get(0).getPods();
        assertNotNull(pods);
        assertEquals(3, pods.size());
        assertEquals("svc-a-0", pods.get(0).getName());
        assertEquals("node-1", pods.get(0).getNodeName());
        assertEquals("10.1.0.10", pods.get(0).getIp());
        assertEquals(PodStatus.HEALTHY, pods.get(0).getStatus());
        assertTrue(pods.get(0).getReady());
        assertEquals(0, pods.get(0).getRestartCount());
        assertEquals("svc-a-1", pods.get(1).getName());
        assertEquals("svc-a-2", pods.get(2).getName());
    }

    @Test
    void podsCountMatchesReplicas() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 25, 25)
        ));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertEquals(25, result.getRecords().get(0).getPods().size());
    }

    @Test
    void podsGeneratedWhenNoSelector() {
        InferenceInstance instance = InferenceInstance.builder()
                .instanceName("svc-a")
                .namespace("ns1")
                .status(InstanceStatus.AVAILABLE)
                .framework("MindIE")
                .currentReplicas(1)
                .desiredReplicas(1)
                .build(); // no selector

        when(inferenceService.listInstances()).thenReturn(List.of(instance));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertEquals(1, result.getRecords().get(0).getPods().size());
    }

    // ───────── Pagination edge cases ─────────

    @Test
    void paginationSecondPage() {
        java.util.List<InferenceInstance> instances = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) {
            instances.add(createInstance("svc-" + i, "ns1", InstanceStatus.AVAILABLE, "MindIE", 1, 1));
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
        assertEquals(5, result.getRecords().size());
        assertEquals(2, result.getPage());
    }

    @Test
    void pageBeyondTotalReturnsEmpty() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", InstanceStatus.AVAILABLE, "MindIE", 1, 1)
        ));

        PaginationRequest req = PaginationRequest.builder()
                .page(5)
                .size(20)
                .sortField("instanceName")
                .sortDirection("desc")
                .build();

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(req, null, null, null);

        assertTrue(result.getRecords().isEmpty());
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPages());
    }
}
