# ModelEngine框架适配层 - 多框架指标调研汇总

> **调研目标**: 为ModelEngine可观测性模块的框架适配层设计提供参考  
> **调研框架**: MindIE、vLLM/vllm-ascend、SGLang  
> **生成时间**: 2025-01-23

---

## 1. 三框架指标对比总览

### 1.1 基础信息对比

| 特性 | MindIE | vLLM/vllm-ascend | SGLang |
|------|--------|------------------|--------|
| **定位** | 企业级推理服务 | 开源推理引擎 | 开源高性能推理引擎 |
| **Metrics端点** | `/metrics` | `/metrics` | `/metrics` |
| **指标前缀** | 无前缀 | `vllm:` | `sglang:` |
| **数据格式** | Prometheus | Prometheus | Prometheus |
| **启用方式** | 配置`MIES_SERVICE_MONITOR_MODE=1` | `--enable-metrics` | `--enable-metrics` |
| **NPU支持** | ✅ 原生NPU指标 | ⚠️ GPU指标映射 | ❌ GPU指标 |
| **多进程支持** | ⚠️ 需配置 | ✅ multiprocess | ✅ multiprocess |

### 1.2 核心指标映射表

| 统一概念 | MindIE | vLLM/vllm-ascend | SGLang |
|---------|--------|------------------|--------|
| **接收请求数** | `request_received_total` | `vllm:request_success_total` + `vllm:request_failure_total` | `sglang:num_requests_total` |
| **成功请求数** | `request_success_total` | `vllm:request_success_total` | - (需计算) |
| **失败请求数** | `request_failed_total` | - (需计算) | `sglang:num_aborted_requests_total` |
| **Prefill Tokens** | `prompt_tokens_total` | `vllm:prompt_tokens_total` | `sglang:prompt_tokens_total` |
| **Generation Tokens** | `generation_tokens_total` | `vllm:generation_tokens_total` | `sglang:generation_tokens_total` |
| **TTFT** | `time_to_first_token_seconds` | `vllm:time_to_first_token_seconds` | `sglang:time_to_first_token_seconds` |
| **TPOT** | `time_per_output_token_seconds` | `vllm:time_per_output_token_seconds` | `sglang:time_per_output_token_seconds` |
| **E2E延迟** | `e2e_request_latency_seconds` | `vllm:e2e_request_latency_seconds` | `sglang:e2e_request_latency_seconds` |
| **运行中请求** | `num_requests_running` | `vllm:num_requests_running` | `sglang:num_running_reqs` |
| **等待中请求** | `num_requests_waiting` | `vllm:num_requests_waiting` | `sglang:num_queue_reqs` |
| **NPU/GPU缓存使用率** | `npu_cache_usage_perc` | `vllm:gpu_cache_usage_perc` | `sglang:token_usage` |
| **前缀缓存命中率** | `npu_prefix_cache_hit_rate` | `vllm:gpu_prefix_cache_hit_rate` | `sglang:cache_hit_rate` |
| **Prefill吞吐量** | `avg_prompt_throughput_toks_per_s` | - | `sglang:realtime_tokens_total{mode="prefill_compute"}` |
| **Decode吞吐量** | `avg_generation_throughput_toks_per_s` | - | `sglang:gen_throughput` |

---

## 2. 各框架指标详情

### 2.1 MindIE（昇腾推理引擎）

**指标命名特点**: 无前缀，直接命名
**NPU特有指标**: 使用`npu_`前缀明确标识

#### 核心指标列表

```promql
# 请求计数
request_received_total{model_name="llama2-7b"}
request_success_total{model_name="llama2-7b"}
request_failed_total{model_name="llama2-7b"}

# 吞吐
total_token_throughput_toks_per_s{model_name="llama2-7b"}
avg_prompt_throughput_toks_per_s{model_name="llama2-7b"}
avg_generation_throughput_toks_per_s{model_name="llama2-7b"}

# 延迟
time_to_first_token_seconds_bucket{model_name="llama2-7b"}
time_per_output_token_seconds_bucket{model_name="llama2-7b"}
e2e_request_latency_seconds_bucket{model_name="llama2-7b"}

# 资源
npu_cache_usage_perc{model_name="llama2-7b"}
cpu_cache_usage_perc{model_name="llama2-7b"}
npu_prefix_cache_hit_rate{model_name="llama2-7b"}
```

#### 企业级特性
- 完整的请求生命周期追踪（接收→成功/失败）
- 区分NPU和CPU缓存
- 抢占计数（`num_preemptions_total`）
- 请求失败率百分比（`failed_request_perc`）

---

### 2.2 vLLM / vllm-ascend

**指标命名特点**: `vllm:`前缀，即使运行在NPU上仍使用`gpu_`命名
**现状**: vllm-ascend当前仍缺乏完整的内置metrics支持（GitHub Issue #1795）

#### 核心指标列表

```promql
# 请求状态（Gauge）
vllm:num_requests_running{model_name="meta-llama/Llama-3.1-8B-Instruct"}
vllm:num_requests_waiting{model_name="meta-llama/Llama-3.1-8B-Instruct"}
vllm:num_requests_swapped{model_name="meta-llama/Llama-3.1-8B-Instruct"}

# Token计数（Counter）
vllm:prompt_tokens_total{model_name="meta-llama/Llama-3.1-8B-Instruct"}
vllm:generation_tokens_total{model_name="meta-llama/Llama-3.1-8B-Instruct"}

# 缓存（Gauge）
vllm:gpu_cache_usage_perc{model_name="meta-llama/Llama-3.1-8B-Instruct"}
vllm:cpu_cache_usage_perc{model_name="meta-llama/Llama-3.1-8B-Instruct"}
vllm:gpu_prefix_cache_hit_rate{model_name="meta-llama/Llama-3.1-8B-Instruct"}

# 延迟（Histogram）
vllm:time_to_first_token_seconds_bucket{model_name="meta-llama/Llama-3.1-8B-Instruct"}
vllm:time_per_output_token_seconds_bucket{model_name="meta-llama/Llama-3.1-8B-Instruct"}
vllm:e2e_request_latency_seconds_bucket{model_name="meta-llama/Llama-3.1-8B-Instruct"}
```

#### 标签设计
- `model_name`: 模型名称
- `finished_reason`: 完成原因（stop/length/abort）

#### vllm-ascend注意事项
- 当前metrics功能仍在开发中（Issue #1795）
- NPU指标使用`gpu_`前缀（命名映射问题）
- 内存指标细分功能请求中（Issue #2662）

---

### 2.3 SGLang

**指标命名特点**: `sglang:`前缀，最丰富的指标集
**优势**: 原生支持Disaggregation、MoE、HiCache等高级特性

#### 核心指标列表

```promql
# 请求状态（Gauge）
sglang:num_running_reqs{model_name="meta-llama/Llama-3.1-8B-Instruct"}
sglang:num_queue_reqs{model_name="meta-llama/Llama-3.1-8B-Instruct"}

# Token（Counter）
sglang:prompt_tokens_total{model_name="...",engine_type="unified"}
sglang:generation_tokens_total{model_name="...",engine_type="unified"}
sglang:cached_tokens_total{model_name="...",cache_source="device"}

# 延迟（Histogram）
sglang:time_to_first_token_seconds_bucket{model_name="..."}
sglang:time_per_output_token_seconds_bucket{model_name="..."}
sglang:e2e_request_latency_seconds_bucket{model_name="..."}
sglang:inter_token_latency_seconds_bucket{model_name="..."}

# 资源（Gauge）
sglang:token_usage{model_name="..."}
sglang:cache_hit_rate{model_name="..."}
sglang:gen_throughput{model_name="..."}
```

#### SGLang特有指标

```promql
# Disaggregation (P/D分离)
sglang:num_prefill_prealloc_queue_reqs
sglang:num_decode_transfer_queue_reqs
sglang:kv_transfer_speed_gb_s
sglang:kv_transfer_latency_ms
sglang:kv_transfer_total_mb

# MoE负载均衡
eplb_balancedness{model_name="...",rank="0"}

# HiCache分层缓存
sglang:hicache_host_used_tokens
sglang:hicache_host_total_tokens

# 推测解码
sglang:spec_accept_length
sglang:spec_accept_rate

# 流式/异步
sglang:num_retracted_reqs
sglang:num_retracted_requests_total
sglang:num_paused_reqs
```

#### 标签设计（最丰富）
- `model_name`: 模型名称
- `engine_type`: unified/prefill/decode
- `tp_rank`: Tensor并行rank
- `pp_rank`: Pipeline并行rank
- `dp_rank`: Data并行rank
- `moe_ep_rank`: MoE Expert并行rank
- `cache_source`: device/host/storage_memory
- `forward_mode`: decode/extend

---

## 3. 框架适配层设计建议

### 3.1 统一指标抽象

建议定义统一的指标接口，屏蔽底层框架差异：

```java
public interface FrameworkMetricsAdapter {
    // 核心指标
    List<Metric> getRequestMetrics(String modelName);
    List<Metric> getTokenMetrics(String modelName);
    List<Metric> getLatencyMetrics(String modelName);
    List<Metric> getResourceMetrics(String modelName);
    
    // 原始查询
    String queryRaw(String promql);
    
    // 框架检测
    FrameworkType detectFramework();
}

public enum FrameworkType {
    MINDIE,      // 昇腾MindIE
    VLLM,        // 标准vLLM
    VLLM_ASCEND, // 昇腾版vLLM
    SGLANG       // SGLang
}
```

### 3.2 指标名称映射表

| 统一指标名 | MindIE | vLLM | SGLang | 指标类型 |
|-----------|--------|------|--------|---------|
| `request_received_total` | `request_received_total` | `vllm:request_success_total` + failure | `sglang:num_requests_total` | Counter |
| `request_success_total` | `request_success_total` | `vllm:request_success_total` | - | Counter |
| `request_failed_total` | `request_failed_total` | - | `sglang:num_aborted_requests_total` | Counter |
| `prompt_tokens_total` | `prompt_tokens_total` | `vllm:prompt_tokens_total` | `sglang:prompt_tokens_total` | Counter |
| `generation_tokens_total` | `generation_tokens_total` | `vllm:generation_tokens_total` | `sglang:generation_tokens_total` | Counter |
| `ttft_seconds` | `time_to_first_token_seconds` | `vllm:time_to_first_token_seconds` | `sglang:time_to_first_token_seconds` | Histogram |
| `tpot_seconds` | `time_per_output_token_seconds` | `vllm:time_per_output_token_seconds` | `sglang:time_per_output_token_seconds` | Histogram |
| `e2e_latency_seconds` | `e2e_request_latency_seconds` | `vllm:e2e_request_latency_seconds` | `sglang:e2e_request_latency_seconds` | Histogram |
| `requests_running` | `num_requests_running` | `vllm:num_requests_running` | `sglang:num_running_reqs` | Gauge |
| `requests_waiting` | `num_requests_waiting` | `vllm:num_requests_waiting` | `sglang:num_queue_reqs` | Gauge |
| `cache_usage_percent` | `npu_cache_usage_perc` | `vllm:gpu_cache_usage_perc` | `sglang:token_usage` | Gauge |
| `prefix_cache_hit_rate` | `npu_prefix_cache_hit_rate` | `vllm:gpu_prefix_cache_hit_rate` | `sglang:cache_hit_rate` | Gauge |

### 3.3 标签标准化

统一输出标签（映射关系）：

| 统一标签 | MindIE | vLLM | SGLang |
|---------|--------|------|--------|
| `model_name` | `model_name` | `model_name` | `model_name` |
| `framework` | "mindie" | "vllm" | "sglang" |
| `instance` | - | - | `instance` (节点IP) |
| `tp_rank` | - | - | `tp_rank` |
| `pp_rank` | - | - | `pp_rank` |

### 3.4 框架特定指标处理

对于各框架特有指标，建议保留原始指标名并添加`framework_specific=true`标签：

```promql
# SGLang特有
sglang:kv_transfer_speed_gb_s{framework_specific="true"}
sglang:hicache_host_used_tokens{framework_specific="true"}

# MindIE特有
num_preemptions_total{framework_specific="true"}

# vLLM特有
vllm:num_requests_swapped{framework_specific="true"}
```

### 3.5 Histogram Bucket统一

建议使用SGLang的bucket边界作为标准（最细粒度）：

```
TTFT buckets: [0.001, 0.005, 0.01, 0.02, 0.04, 0.06, 0.08, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0, 15.0, 20.0, 25.0, 30.0, +Inf]
TPOT buckets: [0.002, 0.005, 0.01, 0.02, 0.03, 0.04, 0.05, 0.06, 0.08, 0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0, 5.0, 6.0, 8.0, +Inf]
```

---

## 4. 关键发现总结

### 4.1 指标完备度排序

1. **SGLang** ⭐⭐⭐⭐⭐
   - 最完整的指标集
   - 支持Disaggregation、MoE、HiCache等高级特性
   - 丰富的标签体系

2. **MindIE** ⭐⭐⭐⭐
   - 企业级完整指标
   - 原生NPU支持
   - 简洁的标签设计

3. **vLLM/vllm-ascend** ⭐⭐⭐
   - 基础指标完整
   - vllm-ascend仍在开发metrics功能
   - GPU/NPU命名映射问题

### 4.2 适配层实现优先级

**P0 - 必须支持**（11个核心指标）
- 请求计数、Token计数（Prefill/Generation）
- 延迟指标（TTFT、TPOT、E2E）
- 资源指标（缓存使用率、前缀缓存命中率）

**P1 - 推荐支持**
- 队列深度、运行中请求数
- 请求成功率/失败率
- Prefill/Decode吞吐量

**P2 - 可选支持**
- 框架特有指标（SGLang的Disaggregation、MindIE的抢占等）
- 多并行rank标签

### 4.3 风险提示

1. **vllm-ascend metrics功能不完善**：当前GitHub Issue #1795仍在跟踪，生产环境使用前需验证
2. **命名映射问题**：vllm-ascend使用`gpu_`前缀表示NPU指标，需在适配层进行语义映射
3. **标签差异大**：SGLang的标签最丰富，MindIE最简洁，适配层需处理标签缺失情况
4. **Histogram bucket差异**：三个框架的bucket边界不同，计算P99等分位数时需注意

---

## 5. 参考资源

### 官方文档
- [MindIE 服务监控指标接口](https://www.hiascend.com/document/detail/zh/mindie/100/mindieservice/servicedev/mindie_service0103.html)
- [vLLM 生产指标文档](https://vllm.hyper.ai/docs/0.8.x/serving/production-metrics/)
- [SGLang Production Metrics](https://docs.sglang.io/references/production_metrics.html)

### GitHub仓库
- [vllm-ascend](https://github.com/vllm-project/vllm-ascend)
- [SGLang源码 - metrics_collector.py](https://github.com/sgl-project/sglang/blob/main/python/sglang/srt/observability/metrics_collector.py)

---

**调研完成** - 可用于框架适配层详细设计
