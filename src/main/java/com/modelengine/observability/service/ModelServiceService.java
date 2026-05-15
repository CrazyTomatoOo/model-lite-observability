package com.modelengine.observability.service;

import com.modelengine.observability.dto.ModelServiceDTO;
import com.modelengine.observability.dto.ModelMetaParams;
import com.modelengine.observability.dto.MetricParams;
import com.modelengine.observability.dto.DeviceParams;
import com.modelengine.observability.dto.FrameworkParams;
import com.modelengine.observability.dto.StorageParams;
import com.modelengine.observability.dto.ResourceParams;
import com.modelengine.observability.dto.DeployParams;
import com.modelengine.observability.dto.ScheduleParams;
import com.modelengine.observability.dto.DetailParams;
import com.modelengine.observability.dto.HpaRule;
import com.modelengine.observability.dto.PageDTO;
import com.modelengine.observability.dto.PaginationRequest;
import com.modelengine.observability.dto.PaginationUtils;
import com.modelengine.observability.dto.PodInfoDTO;
import com.modelengine.observability.service.inference.InferenceInstance;
import com.modelengine.observability.service.inference.InferenceService;
import com.modelengine.observability.service.inference.InstanceStatus;
import com.modelengine.observability.service.inference.PodStatus;
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
                .filter(i -> isBlank(status) || Objects.equals(i.getStatus(), InstanceStatus.fromString(status)))
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
        String name = instance.getInstanceName();

        List<PodInfoDTO> pods = java.util.stream.IntStream.range(0, n)
                .mapToObj(i -> {
                    PodStatus podStatus = mapPodStatus(instance, i);
                    boolean ready = podStatus == PodStatus.HEALTHY;
                    int restarts = podStatus == PodStatus.ERROR || podStatus == PodStatus.IMAGE_PULL_FAILURE ? 1 + i : 0;
                    return PodInfoDTO.builder()
                            .name(name + "-" + i)
                            .nodeName("node-" + (i + 1))
                            .ip("10.1.0." + (10 + i))
                            .status(podStatus)
                            .ready(ready)
                            .restartCount(restarts)
                            .metricsEndpoint("http://" + name + "-" + i + ":9091/metrics")
                            .build();
                })
                .collect(Collectors.toList());

        List<DetailParams> details = java.util.stream.IntStream.range(0, n)
                .mapToObj(i -> {
                    PodStatus podStatus = mapPodStatus(instance, i);
                    String detailText;
                    switch (podStatus) {
                        case HEALTHY:
                            detailText = "Pod " + name + "-" + i + " is running on node-" + (i + 1);
                            break;
                        case STARTING:
                            detailText = "Pod " + name + "-" + i + " is starting on node-" + (i + 1);
                            break;
                        case ERROR:
                            detailText = "Pod " + name + "-" + i + " has error on node-" + (i + 1);
                            break;
                        case TERMINATING:
                            detailText = "Pod " + name + "-" + i + " is terminating on node-" + (i + 1);
                            break;
                        case UNSCHEDULABLE:
                            detailText = "Pod " + name + "-" + i + " is unschedulable";
                            break;
                        case INSUFFICIENT_RESOURCE:
                            detailText = "Pod " + name + "-" + i + " has insufficient resources";
                            break;
                        case IMAGE_PULL_FAILURE:
                            detailText = "Pod " + name + "-" + i + " failed to pull image";
                            break;
                        case MOUNT_FAILURE:
                            detailText = "Pod " + name + "-" + i + " failed to mount volume";
                            break;
                        default:
                            detailText = "Pod " + name + "-" + i + " status unknown";
                    }
                    return DetailParams.builder()
                            .group("Pod")
                            .name(name + "-" + i)
                            .status(podStatus)
                            .detail(detailText)
                            .build();
                })
                .collect(Collectors.toList());

        // Build fully-populated default DTO first (no nulls)
        ModelServiceDTO.ModelServiceDTOBuilder builder = ModelServiceDTO.builder()
                .instanceName(name)
                .userResourceGroupId("rg-" + instance.getNamespace())
                .userResourceGroupName(instance.getNamespace() + " Group")
                .modelMeta(buildModelMeta(name, "1.0", "default"))
                .metrics(MetricParams.builder()
                        .requests(1000)
                        .responses(995)
                        .exceptions(5)
                        .speed(40.0)
                        .totalInputTokens(500000)
                        .totalOutputTokens(150000)
                        .latency(0.3)
                        .build())
                .status(instance.getStatus())
                .currentReplicas(instance.getCurrentReplicas())
                .desiredReplicas(instance.getDesiredReplicas())
                .address(instance.getAddress())
                .deviceParams(DeviceParams.builder()
                        .chipName("Ascend 910B")
                        .nums(2)
                        .memorySize("64Gi")
                        .vgpuCore(0)
                        .vgpuMem("0")
                        .isVirtual(false)
                        .nodeInfo(List.of("node-1", "node-2"))
                        .resourceName(List.of("npu.huawei.com/Ascend910B"))
                        .volcanoLabels(Map.of("volcano.sh/queue-name", "default-queue"))
                        .labels(Map.of("node-type", "gpu"))
                        .build())
                .frameworkParams(FrameworkParams.builder()
                        .name("MindIE")
                        .version("2.3.0")
                        .imageName("swr.cn-north-4.myhuaweicloud.com/mindie:2.3.0")
                        .frameworkParam(Map.of("max_batch_size", "16", "max_seq_len", "8192"))
                        .build())
                .env(Map.of("MODEL_PATH", "/models/" + name,
                        "TENSOR_PARALLEL_SIZE", "1",
                        "MAX_BATCH_SIZE", "16"))
                .storageParams(StorageParams.builder()
                        .useExistingPvc(false)
                        .existingPvcNames(List.of())
                        .storageClass("csi-disk")
                        .storageCapacity("200Gi")
                        .build())
                .resourceParams(ResourceParams.builder()
                        .cpu("8")
                        .memory("64Gi")
                        .devShmSizeLimit("16Gi")
                        .build())
                .deployParams(DeployParams.builder()
                        .nodeNums(n)
                        .build())
                .scheduleParams(ScheduleParams.builder()
                        .priorityClass("default-priority")
                        .enableSpreadPolicy(true)
                        .enablePreemption(false)
                        .hpaSwitch(false)
                        .minReplicaCount(1)
                        .maxReplicaCount(n)
                        .cooldownPeriod(300)
                        .hpaRules(List.of())
                        .queueName("default-queue")
                        .build())
                .additionalParams(Map.of("model_format", "safetensors",
                        "quantization", "fp16",
                        "max_context_length", "8192"))
                .reason(instance.getStatus() == InstanceStatus.AVAILABLE ? "" : "Instance is in " + instance.getStatus().getDisplayName() + " state")
                .details(details)
                .pods(pods);

        switch (name) {
            case "llama3-70b":
                return builder
                        .userResourceGroupId("rg-llama-001")
                        .userResourceGroupName("LLaMA Production")
                        .modelMeta(buildModelMeta("llama3-70b", "3.1", "meta-ai"))
                        .metrics(MetricParams.builder()
                                .requests(8750)
                                .responses(8700)
                                .exceptions(50)
                                .speed(45.2)
                                .totalInputTokens(26250000)
                                .totalOutputTokens(4375000)
                                .latency(0.5)
                                .build())
                        .deviceParams(DeviceParams.builder()
                                .chipName("Ascend 910B")
                                .nums(8)
                                .memorySize("64Gi")
                                .vgpuCore(1)
                                .vgpuMem("16Gi")
                                .isVirtual(false)
                                .nodeInfo(List.of("node-1", "node-2", "node-3", "node-4"))
                                .resourceName(List.of("npu.huawei.com/Ascend910B"))
                                .volcanoLabels(Map.of("volcano.sh/queue-name", "default-queue"))
                                .labels(Map.of("node-type", "gpu"))
                                .build())
                        .frameworkParams(FrameworkParams.builder()
                                .name("MindIE")
                                .version("2.3.0")
                                .imageName("swr.cn-north-4.myhuaweicloud.com/mindie:2.3.0")
                                .frameworkParam(Map.of("max_batch_size", "32", "max_seq_len", "32768"))
                                .build())
                        .env(Map.of("MODEL_PATH", "/models/llama3-70b",
                                "TENSOR_PARALLEL_SIZE", "4",
                                "MAX_BATCH_SIZE", "32"))
                        .storageParams(StorageParams.builder()
                                .useExistingPvc(true)
                                .existingPvcNames(List.of("pvc-llama3-70b-models"))
                                .storageClass("csi-disk")
                                .storageCapacity("500Gi")
                                .build())
                        .resourceParams(ResourceParams.builder()
                                .cpu("32")
                                .memory("256Gi")
                                .devShmSizeLimit("64Gi")
                                .build())
                        .deployParams(DeployParams.builder()
                                .nodeNums(4)
                                .build())
                        .scheduleParams(ScheduleParams.builder()
                                .priorityClass("high-priority")
                                .enableSpreadPolicy(true)
                                .enablePreemption(false)
                                .hpaSwitch(true)
                                .minReplicaCount(2)
                                .maxReplicaCount(8)
                                .cooldownPeriod(300)
                                .hpaRules(List.of(
                                        HpaRule.builder()
                                                .name("cpu-rule")
                                                .type("cpu")
                                                .enabled(true)
                                                .timezone("Asia/Shanghai")
                                                .start("08:00")
                                                .end("22:00")
                                                .desiredReplicas(4)
                                                .targetValue(75.0)
                                                .build(),
                                        HpaRule.builder()
                                                .name("qps-rule")
                                                .type("qps")
                                                .enabled(true)
                                                .timezone("Asia/Shanghai")
                                                .start("00:00")
                                                .end("23:59")
                                                .desiredReplicas(6)
                                                .targetValue(100.0)
                                                .build()))
                                .queueName("default-queue")
                                .build())
                        .additionalParams(Map.of("model_format", "safetensors",
                                "quantization", "fp16",
                                "max_context_length", "32768"))
                        .reason("")
                        .details(List.of(
                                DetailParams.builder()
                                        .group("Pod")
                                        .name("llama3-70b-0")
                                        .status(PodStatus.HEALTHY)
                                        .detail("Pod llama3-70b-0 is running on node-1")
                                        .build(),
                                DetailParams.builder()
                                        .group("Pod")
                                        .name("llama3-70b-1")
                                        .status(PodStatus.HEALTHY)
                                        .detail("Pod llama3-70b-1 is running on node-2")
                                        .build(),
                                DetailParams.builder()
                                        .group("Pod")
                                        .name("llama3-70b-2")
                                        .status(PodStatus.HEALTHY)
                                        .detail("Pod llama3-70b-2 is running on node-3")
                                        .build(),
                                DetailParams.builder()
                                        .group("Pod")
                                        .name("llama3-70b-3")
                                        .status(PodStatus.HEALTHY)
                                        .detail("Pod llama3-70b-3 is running on node-4")
                                        .build()))
                        .pods(pods)
                        .build();
            case "qwen2-7b":
                return builder
                        .userResourceGroupId("rg-qwen-002")
                        .userResourceGroupName("Qwen Research")
                        .modelMeta(buildModelMeta("qwen2-7b", "2.0", "alibaba-cloud"))
                        .metrics(MetricParams.builder()
                                .requests(6200)
                                .responses(6100)
                                .exceptions(100)
                                .speed(38.5)
                                .totalInputTokens(9300000)
                                .totalOutputTokens(1860000)
                                .latency(0.25)
                                .build())
                        .deviceParams(DeviceParams.builder()
                                .chipName("Ascend 910B")
                                .nums(4)
                                .memorySize("64Gi")
                                .vgpuCore(1)
                                .vgpuMem("16Gi")
                                .isVirtual(false)
                                .nodeInfo(List.of("node-5", "node-6"))
                                .resourceName(List.of("npu.huawei.com/Ascend910B"))
                                .volcanoLabels(Map.of("volcano.sh/queue-name", "default-queue"))
                                .labels(Map.of("node-type", "gpu"))
                                .build())
                        .frameworkParams(FrameworkParams.builder()
                                .name("MindIE")
                                .version("2.3.0")
                                .imageName("swr.cn-north-4.myhuaweicloud.com/mindie:2.3.0")
                                .frameworkParam(Map.of("max_batch_size", "16", "max_seq_len", "16384"))
                                .build())
                        .env(Map.of("MODEL_PATH", "/models/qwen2-7b",
                                "TENSOR_PARALLEL_SIZE", "2",
                                "MAX_BATCH_SIZE", "16"))
                        .storageParams(StorageParams.builder()
                                .useExistingPvc(true)
                                .existingPvcNames(List.of("pvc-qwen2-7b-models"))
                                .storageClass("csi-disk")
                                .storageCapacity("300Gi")
                                .build())
                        .resourceParams(ResourceParams.builder()
                                .cpu("16")
                                .memory("128Gi")
                                .devShmSizeLimit("32Gi")
                                .build())
                        .deployParams(DeployParams.builder()
                                .nodeNums(2)
                                .build())
                        .scheduleParams(ScheduleParams.builder()
                                .priorityClass("medium-priority")
                                .enableSpreadPolicy(true)
                                .enablePreemption(false)
                                .hpaSwitch(true)
                                .minReplicaCount(1)
                                .maxReplicaCount(4)
                                .cooldownPeriod(300)
                                .hpaRules(List.of(
                                        HpaRule.builder()
                                                .name("cpu-rule")
                                                .type("cpu")
                                                .enabled(true)
                                                .timezone("Asia/Shanghai")
                                                .start("08:00")
                                                .end("22:00")
                                                .desiredReplicas(2)
                                                .targetValue(70.0)
                                                .build()))
                                .queueName("default-queue")
                                .build())
                        .additionalParams(Map.of("model_format", "safetensors",
                                "quantization", "int8",
                                "max_context_length", "16384"))
                        .reason("")
                        .details(List.of(
                                DetailParams.builder()
                                        .group("Pod")
                                        .name("qwen2-7b-0")
                                        .status(PodStatus.HEALTHY)
                                        .detail("Pod qwen2-7b-0 is running on node-1")
                                        .build(),
                                DetailParams.builder()
                                        .group("Pod")
                                        .name("qwen2-7b-1")
                                        .status(PodStatus.HEALTHY)
                                        .detail("Pod qwen2-7b-1 is running on node-2")
                                        .build()))
                        .pods(pods)
                        .build();
            default:
                return builder.build();
        }
    }

    /**
     * Maps an inference instance to the appropriate PodStatus for its mock pods.
     * Ensures all PodStatus enum values (including underscore ones) appear in mock data.
     *
     * @param instance the inference instance
     * @param podIndex the index of the pod within this instance
     * @return the PodStatus for this pod
     */
    private PodStatus mapPodStatus(InferenceInstance instance, int podIndex) {
        InstanceStatus status = instance.getStatus();
        String name = instance.getInstanceName();

        // Distribute missing underscore statuses across specific instances
        switch (name) {
            case "chatglm3-6b": return PodStatus.INSUFFICIENT_RESOURCE;
            case "bloom-7b1": return PodStatus.IMAGE_PULL_FAILURE;
            case "bert-base-chinese": return PodStatus.MOUNT_FAILURE;
            case "mixtral-8x7b": return PodStatus.UNSCHEDULABLE;
            case "tinyllama-1.1b": return PodStatus.UNSCHEDULABLE;
            default:
                return switch (status) {
                    case AVAILABLE -> PodStatus.HEALTHY;
                    case PARTIAL_RUNNING -> (podIndex == 0) ? PodStatus.ERROR : PodStatus.HEALTHY;
                    case WAITING, STARTING -> PodStatus.STARTING;
                    case UNAVAILABLE -> PodStatus.ERROR;
                    case STOPPED, STOPPING, DELETING -> PodStatus.TERMINATING;
                };
        }
    }

    private Comparator<ModelServiceDTO> buildComparator(String field, String direction) {
        Comparator<ModelServiceDTO> comparator = switch (field != null ? field : "instanceName") {
            case "instanceName" -> Comparator.comparing(
                    ModelServiceDTO::getInstanceName, Comparator.nullsLast(String::compareTo));
            case "status" -> Comparator.comparing(
                    ModelServiceDTO::getStatus, Comparator.nullsLast(Comparator.comparing(InstanceStatus::getDisplayName)));
            default -> Comparator.comparing(
                    ModelServiceDTO::getInstanceName, Comparator.nullsLast(String::compareTo));
        };
        return "asc".equalsIgnoreCase(direction) ? comparator : comparator.reversed();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private ModelMetaParams buildModelMeta(String instanceName, String version, String ownerGroupId) {
        String modelCategory;
        String modelType;
        switch (instanceName) {
            case "llama3-70b":
                modelCategory = "TextGeneration";
                modelType = "Llama3-70B";
                break;
            case "llama3-8b":
                modelCategory = "TextGeneration";
                modelType = "Llama3-8B";
                break;
            case "qwen2-7b":
                modelCategory = "TextGeneration";
                modelType = "Qwen2-7B";
                break;
            case "qwen2-72b":
                modelCategory = "TextGeneration";
                modelType = "Qwen2-72B";
                break;
            case "qwen2-110b":
                modelCategory = "TextGeneration";
                modelType = "Qwen2-110B";
                break;
            case "baichuan2-13b":
                modelCategory = "TextGeneration";
                modelType = "Baichuan2-13B";
                break;
            case "chatglm3-6b":
                modelCategory = "TextGeneration";
                modelType = "ChatGLM3-6B";
                break;
            case "Yi-34b":
                modelCategory = "TextGeneration";
                modelType = "Yi-34B";
                break;
            case "mistral-7b":
                modelCategory = "TextGeneration";
                modelType = "Mistral-7B";
                break;
            case "mixtral-8x7b":
                modelCategory = "TextGeneration";
                modelType = "Mixtral-8x7B";
                break;
            case "phi-3-mini":
                modelCategory = "TextGeneration";
                modelType = "Phi-3-Mini";
                break;
            case "gemma-2b":
                modelCategory = "TextGeneration";
                modelType = "Gemma-2B";
                break;
            case "qwen2-1.5b":
                modelCategory = "TextGeneration";
                modelType = "Qwen2-1.5B";
                break;
            case "tinyllama-1.1b":
                modelCategory = "TextGeneration";
                modelType = "TinyLlama-1.1B";
                break;
            case "gpt-j-6b":
                modelCategory = "TextGeneration";
                modelType = "GPT-J-6B";
                break;
            case "falcon-40b":
                modelCategory = "TextGeneration";
                modelType = "Falcon-40B";
                break;
            case "codellama-34b":
                modelCategory = "TextGeneration";
                modelType = "CodeLlama-34B";
                break;
            case "bloom-7b1":
                modelCategory = "TextGeneration";
                modelType = "BLOOM-7B1";
                break;
            case "starcoder2-15b":
                modelCategory = "TextGeneration";
                modelType = "StarCoder2-15B";
                break;
            case "whisper-large-v3":
                modelCategory = "AudioToText";
                modelType = "Whisper-Large-V3";
                break;
            case "clip-vit-large":
                modelCategory = "ImageTextToText";
                modelType = "CLIP-ViT-Large";
                break;
            case "bert-base-chinese":
                modelCategory = "TextEmbedding";
                modelType = "BERT-Base-Chinese";
                break;
            case "roberta-large":
                modelCategory = "TextEmbedding";
                modelType = "RoBERTa-Large";
                break;
            case "m3e-base":
                modelCategory = "TextEmbedding";
                modelType = "M3E-Base";
                break;
            case "bge-large-zh":
                modelCategory = "TextEmbedding";
                modelType = "BGE-Large-ZH";
                break;
            case "jina-embeddings-v2":
                modelCategory = "TextEmbedding";
                modelType = "Jina-Embeddings-V2";
                break;
            default:
                modelCategory = "TextGeneration";
                modelType = instanceName;
                break;
        }
        return ModelMetaParams.builder()
                .modelType(modelType)
                .modelCategory(modelCategory)
                .modelName(instanceName)
                .version(version)
                .modelOwnerGroupId(ownerGroupId)
                .build();
    }
}
