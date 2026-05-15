package com.modelengine.observability.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import com.modelengine.observability.service.inference.InstanceStatus;
import com.modelengine.observability.service.inference.PodStatus;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD serialization tests for all spec-aligned DTOs.
 * Verifies Jackson serialization/deserialization matches OpenAPI spec field names.
 */
class DtoSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    // --- ModelMetaParams ---
    @Nested
    @DisplayName("ModelMetaParams serialization")
    class ModelMetaParamsTest {

        @Test
        @DisplayName("should serialize all fields")
        void serializeAllFields() throws Exception {
            ModelMetaParams params = ModelMetaParams.builder()
                    .modelType("llm")
                    .modelCategory("nlp")
                    .modelName("llama2-7b")
                    .version("V1")
                    .modelOwnerGroupId("group-001")
                    .build();

            String json = objectMapper.writeValueAsString(params);
            assertTrue(json.contains("\"modelType\""));
            assertTrue(json.contains("\"modelCategory\""));
            assertTrue(json.contains("\"modelName\""));
            assertTrue(json.contains("\"version\""));
            assertTrue(json.contains("\"modelOwnerGroupId\""));
            assertTrue(json.contains("llm"));
        }

        @Test
        @DisplayName("should deserialize from JSON")
        void deserializeFromJson() throws Exception {
            String json = """
                {"modelType":"llm","modelCategory":"nlp","modelName":"llama2-7b","version":"V1","modelOwnerGroupId":"group-001"}
                """;
            ModelMetaParams params = objectMapper.readValue(json, ModelMetaParams.class);
            assertEquals("llm", params.getModelType());
            assertEquals("nlp", params.getModelCategory());
            assertEquals("llama2-7b", params.getModelName());
            assertEquals("V1", params.getVersion());
            assertEquals("group-001", params.getModelOwnerGroupId());
        }

        @Test
        @DisplayName("should round-trip correctly")
        void roundTrip() throws Exception {
            ModelMetaParams original = ModelMetaParams.builder()
                    .modelType("llm").modelCategory("nlp").modelName("llama2-7b")
                    .version("V1").modelOwnerGroupId("group-001").build();
            String json = objectMapper.writeValueAsString(original);
            ModelMetaParams deserialized = objectMapper.readValue(json, ModelMetaParams.class);
            assertEquals(original, deserialized);
        }
    }

    // --- MetricParams ---
    @Nested
    @DisplayName("MetricParams serialization")
    class MetricParamsTest {

        @Test
        @DisplayName("should serialize all fields")
        void serializeAllFields() throws Exception {
            MetricParams params = MetricParams.builder()
                    .requests(1000).responses(995).exceptions(5)
                    .speed(150.5).totalInputTokens(50000).totalOutputTokens(25000)
                    .latency(85.3).build();

            String json = objectMapper.writeValueAsString(params);
            assertTrue(json.contains("\"requests\""));
            assertTrue(json.contains("\"responses\""));
            assertTrue(json.contains("\"exceptions\""));
            assertTrue(json.contains("\"speed\""));
            assertTrue(json.contains("\"totalInputTokens\""));
            assertTrue(json.contains("\"totalOutputTokens\""));
            assertTrue(json.contains("\"latency\""));
        }

        @Test
        @DisplayName("should round-trip correctly")
        void roundTrip() throws Exception {
            MetricParams original = MetricParams.builder()
                    .requests(1000).responses(995).exceptions(5)
                    .speed(150.5).totalInputTokens(50000).totalOutputTokens(25000)
                    .latency(85.3).build();
            String json = objectMapper.writeValueAsString(original);
            MetricParams deserialized = objectMapper.readValue(json, MetricParams.class);
            assertEquals(original, deserialized);
        }
    }

    // --- DeviceParams ---
    @Nested
    @DisplayName("DeviceParams serialization")
    class DeviceParamsTest {

        @Test
        @DisplayName("should serialize all fields including nested maps and lists")
        void serializeAllFields() throws Exception {
            DeviceParams params = DeviceParams.builder()
                    .chipName("Ascend910B").nums(2).memorySize("64G")
                    .isVirtual(false)
                    .nodeInfo(List.of("node-1", "node-2"))
                    .resourceName(List.of("npu"))
                    .volcanoLabels(Map.of("key1", "val1"))
                    .labels(Map.of("label1", "val1"))
                    .build();

            String json = objectMapper.writeValueAsString(params);
            assertTrue(json.contains("\"chipName\""));
            assertTrue(json.contains("\"nums\""));
            assertTrue(json.contains("\"memorySize\""));
            assertTrue(json.contains("\"vgpuCore\""));
            assertTrue(json.contains("\"vgpuMem\""));
            assertTrue(json.contains("\"isVirtual\""));
            assertTrue(json.contains("\"nodeInfo\""));
            assertTrue(json.contains("\"resourceName\""));
            assertTrue(json.contains("\"volcanoLabels\""));
            assertTrue(json.contains("\"labels\""));
        }

        @Test
        @DisplayName("should handle nullable vgpu fields")
        void nullableVgpuFields() throws Exception {
            DeviceParams params = DeviceParams.builder()
                    .vgpuCore(null).vgpuMem(null).build();

            String json = objectMapper.writeValueAsString(params);
            assertTrue(json.contains("\"vgpuCore\" : null"));
            assertTrue(json.contains("\"vgpuMem\" : null"));
        }

        @Test
        @DisplayName("should round-trip correctly")
        void roundTrip() throws Exception {
            DeviceParams original = DeviceParams.builder()
                    .chipName("Ascend910B").nums(2).memorySize("64G")
                    .isVirtual(false).nodeInfo(List.of("node-1"))
                    .resourceName(List.of("npu")).build();
            String json = objectMapper.writeValueAsString(original);
            DeviceParams deserialized = objectMapper.readValue(json, DeviceParams.class);
            assertEquals(original, deserialized);
        }
    }

    // --- FrameworkParams ---
    @Nested
    @DisplayName("FrameworkParams serialization")
    class FrameworkParamsTest {

        @Test
        @DisplayName("should serialize name as String not enum")
        void nameIsString() throws Exception {
            FrameworkParams params = FrameworkParams.builder()
                    .name("mindie-910b").version("1.0.0")
                    .imageName("registry/example/mindie:latest")
                    .frameworkParam(Map.of("key", "value"))
                    .build();

            String json = objectMapper.writeValueAsString(params);
            assertTrue(json.contains("\"name\" : \"mindie-910b\""));
            assertTrue(json.contains("\"frameworkParam\""));
        }

        @Test
        @DisplayName("should round-trip correctly")
        void roundTrip() throws Exception {
            FrameworkParams original = FrameworkParams.builder()
                    .name("mindie-910b").version("1.0.0")
                    .imageName("registry/example/mindie:latest")
                    .frameworkParam(Map.of("tensor-parallel-size", "2"))
                    .build();
            String json = objectMapper.writeValueAsString(original);
            FrameworkParams deserialized = objectMapper.readValue(json, FrameworkParams.class);
            assertEquals(original, deserialized);
        }
    }

    // --- StorageParams ---
    @Nested
    @DisplayName("StorageParams serialization")
    class StorageParamsTest {

        @Test
        @DisplayName("should serialize all fields")
        void serializeAllFields() throws Exception {
            StorageParams params = StorageParams.builder()
                    .useExistingPvc(false)
                    .existingPvcNames(List.of())
                    .storageClass("standard")
                    .storageCapacity("100Gi")
                    .build();

            String json = objectMapper.writeValueAsString(params);
            assertTrue(json.contains("\"useExistingPvc\""));
            assertTrue(json.contains("\"existingPvcNames\""));
            assertTrue(json.contains("\"storageClass\""));
            assertTrue(json.contains("\"storageCapacity\""));
        }

        @Test
        @DisplayName("should round-trip correctly")
        void roundTrip() throws Exception {
            StorageParams original = StorageParams.builder()
                    .useExistingPvc(true)
                    .existingPvcNames(List.of("pvc-1", "pvc-2"))
                    .storageClass("standard").storageCapacity("100Gi")
                    .build();
            String json = objectMapper.writeValueAsString(original);
            StorageParams deserialized = objectMapper.readValue(json, StorageParams.class);
            assertEquals(original, deserialized);
        }
    }

    // --- ResourceParams ---
    @Nested
    @DisplayName("ResourceParams serialization")
    class ResourceParamsTest {

        @Test
        @DisplayName("should serialize all fields")
        void serializeAllFields() throws Exception {
            ResourceParams params = ResourceParams.builder()
                    .cpu("16").memory("64").devShmSizeLimit("16Gi")
                    .build();

            String json = objectMapper.writeValueAsString(params);
            assertTrue(json.contains("\"cpu\" : \"16\""));
            assertTrue(json.contains("\"memory\" : \"64\""));
            assertTrue(json.contains("\"devShmSizeLimit\" : \"16Gi\""));
        }

        @Test
        @DisplayName("should round-trip correctly")
        void roundTrip() throws Exception {
            ResourceParams original = ResourceParams.builder()
                    .cpu("16").memory("64").devShmSizeLimit("16Gi").build();
            String json = objectMapper.writeValueAsString(original);
            ResourceParams deserialized = objectMapper.readValue(json, ResourceParams.class);
            assertEquals(original, deserialized);
        }
    }

    // --- DeployParams ---
    @Nested
    @DisplayName("DeployParams serialization")
    class DeployParamsTest {

        @Test
        @DisplayName("should serialize nodeNums field")
        void serializeNodeNums() throws Exception {
            DeployParams params = DeployParams.builder().nodeNums(1).build();

            String json = objectMapper.writeValueAsString(params);
            assertTrue(json.contains("\"nodeNums\" : 1"));
        }

        @Test
        @DisplayName("should round-trip correctly")
        void roundTrip() throws Exception {
            DeployParams original = DeployParams.builder().nodeNums(3).build();
            String json = objectMapper.writeValueAsString(original);
            DeployParams deserialized = objectMapper.readValue(json, DeployParams.class);
            assertEquals(original, deserialized);
        }
    }

    // --- ScheduleParams ---
    @Nested
    @DisplayName("ScheduleParams serialization")
    class ScheduleParamsTest {

        @Test
        @DisplayName("should serialize all fields including nested HpaRules")
        void serializeAllFields() throws Exception {
            HpaRule rule = HpaRule.builder()
                    .name("cron-rule-1").type("cron").enabled(true)
                    .timezone("Asia/Shanghai").start("09:00").end("18:00")
                    .desiredReplicas(4).build();

            ScheduleParams params = ScheduleParams.builder()
                    .priorityClass("high").enableSpreadPolicy(false)
                    .enablePreemption(false).hpaSwitch(false)
                    .minReplicaCount(1).maxReplicaCount(4)
                    .cooldownPeriod(600).hpaRules(List.of(rule))
                    .queueName("default").build();

            String json = objectMapper.writeValueAsString(params);
            assertTrue(json.contains("\"priorityClass\""));
            assertTrue(json.contains("\"enableSpreadPolicy\""));
            assertTrue(json.contains("\"enablePreemption\""));
            assertTrue(json.contains("\"hpaSwitch\""));
            assertTrue(json.contains("\"minReplicaCount\""));
            assertTrue(json.contains("\"maxReplicaCount\""));
            assertTrue(json.contains("\"cooldownPeriod\""));
            assertTrue(json.contains("\"hpaRules\""));
            assertTrue(json.contains("\"queueName\""));
            assertTrue(json.contains("\"name\" : \"cron-rule-1\""));
        }

        @Test
        @DisplayName("should round-trip correctly")
        void roundTrip() throws Exception {
            ScheduleParams original = ScheduleParams.builder()
                    .priorityClass("high").enableSpreadPolicy(true)
                    .hpaSwitch(true).hpaRules(List.of(
                            HpaRule.builder().name("r1").type("cron").enabled(true).build()
                    )).build();
            String json = objectMapper.writeValueAsString(original);
            ScheduleParams deserialized = objectMapper.readValue(json, ScheduleParams.class);
            assertEquals(original, deserialized);
        }
    }

    // --- HpaRule ---
    @Nested
    @DisplayName("HpaRule serialization")
    class HpaRuleTest {

        @Test
        @DisplayName("should serialize all fields")
        void serializeAllFields() throws Exception {
            HpaRule rule = HpaRule.builder()
                    .name("rule-1").type("metrics-api").enabled(false)
                    .targetValue(100.0).build();

            String json = objectMapper.writeValueAsString(rule);
            assertTrue(json.contains("\"name\" : \"rule-1\""));
            assertTrue(json.contains("\"type\" : \"metrics-api\""));
            assertTrue(json.contains("\"enabled\" : false"));
            assertTrue(json.contains("\"targetValue\" : 100.0"));
        }

        @Test
        @DisplayName("should round-trip correctly")
        void roundTrip() throws Exception {
            HpaRule original = HpaRule.builder()
                    .name("cron-rule").type("cron").enabled(true)
                    .timezone("UTC").start("00:00").end("23:59")
                    .desiredReplicas(2).targetValue(50.0).build();
            String json = objectMapper.writeValueAsString(original);
            HpaRule deserialized = objectMapper.readValue(json, HpaRule.class);
            assertEquals(original, deserialized);
        }
    }

    // --- DetailParams ---
    @Nested
    @DisplayName("DetailParams serialization")
    class DetailParamsTest {

        @Test
        @DisplayName("should serialize all fields")
        void serializeAllFields() throws Exception {
            DetailParams params = DetailParams.builder()
                    .group("Pod").name("llama2-7b-chat-xxx")
                    .status(PodStatus.HEALTHY).detail("Container ready")
                    .build();

            String json = objectMapper.writeValueAsString(params);
            assertTrue(json.contains("\"group\" : \"Pod\""));
            assertTrue(json.contains("\"name\" : \"llama2-7b-chat-xxx\""));
            assertTrue(json.contains("\"status\" : \"Healthy\""));
            assertTrue(json.contains("\"detail\" : \"Container ready\""));
        }

        @Test
        @DisplayName("should round-trip correctly")
        void roundTrip() throws Exception {
            DetailParams original = DetailParams.builder()
                    .group("Pod").name("test").status(PodStatus.ERROR).detail("OOMKilled").build();
            String json = objectMapper.writeValueAsString(original);
            DetailParams deserialized = objectMapper.readValue(json, DetailParams.class);
            assertEquals(original, deserialized);
        }
    }

    // --- PodInfoDTO ---
    @Nested
    @DisplayName("PodInfoDTO serialization")
    class PodInfoDtoTest {

        @Test
        @DisplayName("should serialize all 7 fields per spec")
        void serializeAllFields() throws Exception {
            PodInfoDTO pod = PodInfoDTO.builder()
                    .name("llama2-7b-chat-7d9f4b8c5-x2v9m")
                    .nodeName("ascend-node-01")
                    .ip("10.0.1.15")
                    .status(PodStatus.HEALTHY)
                    .ready(true)
                    .restartCount(0)
                    .metricsEndpoint("10.0.1.15:8080")
                    .build();

            String json = objectMapper.writeValueAsString(pod);
            assertTrue(json.contains("\"name\""));
            assertTrue(json.contains("\"nodeName\""));
            assertTrue(json.contains("\"ip\""));
            assertTrue(json.contains("\"status\""));
            assertTrue(json.contains("\"ready\""));
            assertTrue(json.contains("\"restartCount\""));
            assertTrue(json.contains("\"metricsEndpoint\""));
        }

        @Test
        @DisplayName("should round-trip correctly")
        void roundTrip() throws Exception {
            PodInfoDTO original = PodInfoDTO.builder()
                    .name("pod-1").nodeName("node-1").ip("10.0.0.1")
                    .status(PodStatus.HEALTHY).ready(true).restartCount(0)
                    .metricsEndpoint("10.0.0.1:8080").build();
            String json = objectMapper.writeValueAsString(original);
            PodInfoDTO deserialized = objectMapper.readValue(json, PodInfoDTO.class);
            assertEquals(original, deserialized);
        }
    }

    // --- ModelServiceDTO (full 20-field) ---
    @Nested
    @DisplayName("ModelServiceDTO serialization")
    class ModelServiceDtoTest {

        @Test
        @DisplayName("should serialize all 20 fields")
        void serializeAllFields() throws Exception {
            ModelServiceDTO dto = ModelServiceDTO.builder()
                    .instanceName("llama2-7b-chat")
                    .userResourceGroupId("group-001")
                    .userResourceGroupName("默认资源组")
                    .modelMeta(ModelMetaParams.builder().modelName("llama2-7b").build())
                    .metrics(MetricParams.builder().requests(1000).build())
                    .status(InstanceStatus.AVAILABLE)
                    .currentReplicas(3)
                    .desiredReplicas(3)
                    .address("http://llama2-7b-chat.default.svc.cluster.local:8080")
                    .deviceParams(DeviceParams.builder().chipName("Ascend910B").build())
                    .frameworkParams(FrameworkParams.builder().name("mindie-910b").build())
                    .env(Map.of("_LOG_LEVEL", "info"))
                    .storageParams(StorageParams.builder().storageClass("standard").build())
                    .resourceParams(ResourceParams.builder().cpu("16").build())
                    .deployParams(DeployParams.builder().nodeNums(1).build())
                    .scheduleParams(ScheduleParams.builder().priorityClass("high").build())
                    .additionalParams(Map.of())
                    .reason("")
                    .details(List.of(DetailParams.builder().group("Pod").build()))
                    .pods(List.of(PodInfoDTO.builder().name("pod-1").build()))
                    .build();

            String json = objectMapper.writeValueAsString(dto);
            assertTrue(json.contains("\"instanceName\""));
            assertTrue(json.contains("\"userResourceGroupId\""));
            assertTrue(json.contains("\"userResourceGroupName\""));
            assertTrue(json.contains("\"modelMeta\""));
            assertTrue(json.contains("\"metrics\""));
            assertTrue(json.contains("\"status\""));
            assertTrue(json.contains("\"currentReplicas\""));
            assertTrue(json.contains("\"desiredReplicas\""));
            assertTrue(json.contains("\"address\""));
            assertTrue(json.contains("\"deviceParams\""));
            assertTrue(json.contains("\"frameworkParams\""));
            assertTrue(json.contains("\"env\""));
            assertTrue(json.contains("\"storageParams\""));
            assertTrue(json.contains("\"resourceParams\""));
            assertTrue(json.contains("\"deployParams\""));
            assertTrue(json.contains("\"scheduleParams\""));
            assertTrue(json.contains("\"additionalParams\""));
            assertTrue(json.contains("\"reason\""));
            assertTrue(json.contains("\"details\""));
            assertTrue(json.contains("\"pods\""));
        }

        @Test
        @DisplayName("should handle null nested objects (CRD not present)")
        void nullNestedObjects() throws Exception {
            ModelServiceDTO dto = ModelServiceDTO.builder()
                    .instanceName("test")
                    .status(InstanceStatus.UNAVAILABLE)
                    .currentReplicas(0)
                    .desiredReplicas(0)
                    .build();

            String json = objectMapper.writeValueAsString(dto);
            // All nested objects should be null
            assertTrue(json.contains("\"modelMeta\" : null"));
            assertTrue(json.contains("\"metrics\" : null"));
            assertTrue(json.contains("\"deviceParams\" : null"));
            assertTrue(json.contains("\"frameworkParams\" : null"));
            assertTrue(json.contains("\"storageParams\" : null"));
            assertTrue(json.contains("\"resourceParams\" : null"));
            assertTrue(json.contains("\"deployParams\" : null"));
            assertTrue(json.contains("\"scheduleParams\" : null"));
        }

        @Test
        @DisplayName("should deserialize from JSON")
        void deserializeFromJson() throws Exception {
            String json = """
                {
                    "instanceName": "llama2-7b-chat",
                    "status": "AVAILABLE",
                    "currentReplicas": 3,
                    "desiredReplicas": 3,
                    "address": "http://example.com:8080",
                    "modelMeta": {"modelName": "llama2-7b"},
                    "pods": [{"name": "pod-1", "status": "HEALTHY"}]
                }
                """;
            ModelServiceDTO dto = objectMapper.readValue(json, ModelServiceDTO.class);
            assertEquals("llama2-7b-chat", dto.getInstanceName());
            assertEquals(InstanceStatus.AVAILABLE, dto.getStatus());
            assertEquals(3, dto.getCurrentReplicas());
            assertNotNull(dto.getModelMeta());
            assertEquals("llama2-7b", dto.getModelMeta().getModelName());
            assertNotNull(dto.getPods());
            assertEquals(1, dto.getPods().size());
            assertEquals("pod-1", dto.getPods().get(0).getName());
        }

        @Test
        @DisplayName("should round-trip correctly")
        void roundTrip() throws Exception {
            ModelServiceDTO original = ModelServiceDTO.builder()
                    .instanceName("test-service")
                    .status(InstanceStatus.AVAILABLE)
                    .currentReplicas(2)
                    .desiredReplicas(2)
                    .address("http://test:8080")
                    .env(Map.of("KEY", "VAL"))
                    .pods(List.of(PodInfoDTO.builder().name("p1").status(PodStatus.HEALTHY).ready(true).build()))
                    .build();
            String json = objectMapper.writeValueAsString(original);
            ModelServiceDTO deserialized = objectMapper.readValue(json, ModelServiceDTO.class);
            assertEquals(original.getInstanceName(), deserialized.getInstanceName());
            assertEquals(original.getStatus(), deserialized.getStatus());
            assertEquals(original.getCurrentReplicas(), deserialized.getCurrentReplicas());
            assertEquals(original.getDesiredReplicas(), deserialized.getDesiredReplicas());
            assertEquals(original.getEnv(), deserialized.getEnv());
            assertEquals(original.getPods().size(), deserialized.getPods().size());
        }

        @Test
        @DisplayName("builder should work with no args constructor")
        void builderWithNoArgs() {
            ModelServiceDTO dto = new ModelServiceDTO();
            assertNull(dto.getInstanceName());
            assertNull(dto.getModelMeta());
            assertNull(dto.getMetrics());
        }
    }
}
