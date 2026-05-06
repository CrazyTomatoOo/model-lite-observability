package com.modelengine.observability.service.inference;

import com.modelengine.observability.entity.ModelInference;
import com.modelengine.observability.informer.ModelInferenceInformer;
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
@ExtendWith(MockitoExtension.class)
class InformerInferenceServiceTest {

    @Mock
    private ModelInferenceInformer informer;

    private InformerInferenceService service;

    @BeforeEach
    void setUp() {
        service = new InformerInferenceService(informer);
    }

    private ModelInference createModelInference(String serviceId, String namespace, String status,
                                                 String framework, int replicas, int availableReplicas) {
        return ModelInference.builder()
                .serviceId(serviceId)
                .name(serviceId)
                .namespace(namespace)
                .status(status)
                .replicas(replicas)
                .availableReplicas(availableReplicas)
                .framework(framework)
                .selector(Map.of("app", serviceId))
                .labels(Map.of("env", "test"))
                .annotations(Map.of("note", "test"))
                .build();
    }

    @Test
    void listInstancesReturnsAllInstances() {
        when(informer.listModelInferences()).thenReturn(List.of(
                createModelInference("svc-a", "ns1", "Running", "VLLM", 3, 2),
                createModelInference("svc-b", "ns2", "Pending", "SGLang", 1, 0)
        ));

        List<InferenceInstance> result = service.listInstances();

        assertEquals(2, result.size());
        assertEquals("svc-a", result.get(0).getInstanceName());
        assertEquals("svc-b", result.get(1).getInstanceName());
    }

    @Test
    void listInstancesReturnsEmptyListWhenNoData() {
        when(informer.listModelInferences()).thenReturn(List.of());

        assertTrue(service.listInstances().isEmpty());
    }

    @Test
    void getInstanceReturnsInstanceWhenFound() {
        when(informer.getModelInference("svc-a"))
                .thenReturn(createModelInference("svc-a", "ns1", "Running", "VLLM", 3, 2));

        var result = service.getInstance("svc-a");

        assertTrue(result.isPresent());
        InferenceInstance instance = result.get();
        assertEquals("svc-a", instance.getInstanceName());
        assertEquals("ns1", instance.getNamespace());
        assertEquals("Running", instance.getStatus());
        assertEquals("VLLM", instance.getFramework());
        assertEquals(2, instance.getCurrentReplicas());
        assertEquals(3, instance.getDesiredReplicas());
    }

    @Test
    void getInstanceReturnsEmptyWhenNotFound() {
        when(informer.getModelInference("unknown")).thenReturn(null);

        assertTrue(service.getInstance("unknown").isEmpty());
    }

    @Test
    void listByNamespaceFiltersCorrectly() {
        when(informer.listByNamespace("ns1")).thenReturn(List.of(
                createModelInference("svc-a", "ns1", "Running", "VLLM", 3, 2)
        ));

        List<InferenceInstance> result = service.listByNamespace("ns1");

        assertEquals(1, result.size());
        assertEquals("svc-a", result.get(0).getInstanceName());
        assertEquals("ns1", result.get(0).getNamespace());
    }

    @Test
    void listByNamespaceReturnsEmptyWhenNoMatch() {
        when(informer.listByNamespace("nonexistent")).thenReturn(List.of());

        assertTrue(service.listByNamespace("nonexistent").isEmpty());
    }

    @Test
    void listByFrameworkFiltersCorrectly() {
        when(informer.listByFramework("VLLM")).thenReturn(List.of(
                createModelInference("svc-a", "ns1", "Running", "VLLM", 3, 2)
        ));

        List<InferenceInstance> result = service.listByFramework("VLLM");

        assertEquals(1, result.size());
        assertEquals("VLLM", result.get(0).getFramework());
    }

    @Test
    void listByFrameworkReturnsEmptyWhenNoMatch() {
        when(informer.listByFramework("Unknown")).thenReturn(List.of());

        assertTrue(service.listByFramework("Unknown").isEmpty());
    }

    @Test
    void listByStatusFiltersCorrectly() {
        when(informer.listByStatus("Running")).thenReturn(List.of(
                createModelInference("svc-a", "ns1", "Running", "VLLM", 3, 2)
        ));

        List<InferenceInstance> result = service.listByStatus("Running");

        assertEquals(1, result.size());
        assertEquals("Running", result.get(0).getStatus());
    }

    @Test
    void listByStatusReturnsEmptyWhenNoMatch() {
        when(informer.listByStatus("Failed")).thenReturn(List.of());

        assertTrue(service.listByStatus("Failed").isEmpty());
    }

    @Test
    void existsReturnsTrueWhenFound() {
        when(informer.contains("svc-a")).thenReturn(true);

        assertTrue(service.exists("svc-a"));
    }

    @Test
    void existsReturnsFalseWhenNotFound() {
        when(informer.contains("unknown")).thenReturn(false);

        assertFalse(service.exists("unknown"));
    }

    @Test
    void toInstanceMapsAllFieldsCorrectly() {
        when(informer.getModelInference("svc-a"))
                .thenReturn(createModelInference("svc-a", "ns1", "Running", "VLLM", 3, 2));

        var result = service.getInstance("svc-a").orElseThrow();

        assertEquals("svc-a", result.getInstanceName());
        assertEquals("ns1", result.getNamespace());
        assertEquals("Running", result.getStatus());
        assertEquals(2, result.getCurrentReplicas());
        assertEquals(3, result.getDesiredReplicas());
        assertEquals(Map.of("app", "svc-a"), result.getSelector());
        assertEquals("VLLM", result.getFramework());
        assertNull(result.getAddress());
        assertEquals(Map.of("env", "test"), result.getLabels());
        assertEquals(Map.of("note", "test"), result.getAnnotations());
    }
}
