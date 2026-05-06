package com.modelengine.observability.service;

import com.modelengine.observability.dto.ModelServiceDTO;
import com.modelengine.observability.dto.PageDTO;
import com.modelengine.observability.dto.PaginationRequest;
import com.modelengine.observability.dto.PodInfoDTO;
import com.modelengine.observability.informer.PodInformer;
import com.modelengine.observability.service.inference.InferenceInstance;
import com.modelengine.observability.service.inference.InferenceService;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodStatus;
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

    @Mock
    private PodInformer podInformer;

    private ModelServiceService modelServiceService;

    @BeforeEach
    void setUp() {
        modelServiceService = new ModelServiceService(inferenceService, podInformer);
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
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of());

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
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of());

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
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of());

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertNull(result.getData().get(0).getMetrics());
    }

    @Test
    void nestedCrdsFieldsAreNull() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 3, 2)
        ));
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of());

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
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of());

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
        when(podInformer.listBySelector(Map.of("app", "svc-c"))).thenReturn(List.of());
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of());
        when(podInformer.listBySelector(Map.of("app", "svc-b"))).thenReturn(List.of());

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
        when(podInformer.listBySelector(Map.of("app", "svc-c"))).thenReturn(List.of());
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of());
        when(podInformer.listBySelector(Map.of("app", "svc-b"))).thenReturn(List.of());

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
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of());

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
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of());

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
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of());

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
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of());

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
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of());

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
    void podsPopulatedFromPodInformer() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 1, 1)
        ));

        Pod pod = createPod("svc-a-pod-0", "node-1", "10.0.0.1", "Running", true, 0);
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of(pod));

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        List<PodInfoDTO> pods = result.getData().get(0).getPods();
        assertNotNull(pods);
        assertEquals(1, pods.size());
        assertEquals("svc-a-pod-0", pods.get(0).getName());
        assertEquals("node-1", pods.get(0).getNodeName());
        assertEquals("10.0.0.1", pods.get(0).getIp());
        assertEquals("Running", pods.get(0).getStatus());
        assertTrue(pods.get(0).getReady());
        assertEquals(0, pods.get(0).getRestartCount());
    }

    @Test
    void podsLimitedTo20() {
        when(inferenceService.listInstances()).thenReturn(List.of(
                createInstance("svc-a", "ns1", "Running", "VLLM", 1, 1)
        ));

        java.util.List<Pod> manyPods = new java.util.ArrayList<>();
        for (int i = 0; i < 25; i++) {
            manyPods.add(createPod("pod-" + i, "node-1", "10.0.0." + i, "Running", true, 0));
        }
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(manyPods);

        PageDTO<ModelServiceDTO> result = modelServiceService.listServices(
                defaultRequest(), null, null, null);

        assertEquals(20, result.getData().get(0).getPods().size());
    }

    @Test
    void noSelectorUsesAllPods() {
        InferenceInstance instance = InferenceInstance.builder()
                .instanceName("svc-a")
                .namespace("ns1")
                .status("Running")
                .framework("VLLM")
                .currentReplicas(1)
                .desiredReplicas(1)
                .build(); // no selector

        when(inferenceService.listInstances()).thenReturn(List.of(instance));

        Pod pod = createPod("svc-a-pod-0", "node-1", "10.0.0.1", "Running", true, 0);
        when(podInformer.listPods()).thenReturn(List.of(pod));

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
        for (int i = 0; i < 25; i++) {
            when(podInformer.listBySelector(Map.of("app", "svc-" + i))).thenReturn(List.of());
        }

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
        when(podInformer.listBySelector(Map.of("app", "svc-a"))).thenReturn(List.of());

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

    private Pod createPod(String name, String nodeName, String ip, String phase, boolean ready, int restartCount) {
        Pod pod = new Pod();
        ObjectMeta meta = new ObjectMeta();
        meta.setName(name);
        pod.setMetadata(meta);

        PodSpec spec = new PodSpec();
        spec.setNodeName(nodeName);
        pod.setSpec(spec);

        PodStatus status = new PodStatus();
        status.setPodIP(ip);
        status.setPhase(phase);
        if (ready) {
            PodCondition readyCondition = new PodCondition();
            readyCondition.setType("Ready");
            readyCondition.setStatus("True");
            status.setConditions(List.of(readyCondition));
        }
        ContainerStatus containerStatus = new ContainerStatus();
        containerStatus.setRestartCount(restartCount);
        status.setContainerStatuses(List.of(containerStatus));
        pod.setStatus(status);

        return pod;
    }
}
