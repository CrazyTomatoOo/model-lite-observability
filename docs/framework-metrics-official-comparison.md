# ModelEngine框架适配层 - 官方文档指标对比补充

> **基于官方文档的指标对比**
> **文档来源**:
> - vLLM: https://docs.vllm.ai/en/latest/design/metrics/
> - SGLang: http://docs.sglang.io/references/production_metrics.html
> **更新时间**: 2025-01-23

---

## 1. vLLM官方指标清单 (v1引擎)

根据vLLM官方设计文档，v1引擎暴露的完整指标集如下：

### 1.1 核心v1指标 (带`vllm:`前缀)

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `vllm:num_requests_running` | Gauge | 当前正在执行的请求数 |
| `vllm:num_requests_waiting` | Gauge | 当前等待的请求数 |
| `vllm:num_requests_swapped` | Gauge | 当前交换到CPU的请求数 |
| `vllm:kv_cache_usage_perc` | Gauge | KV缓存使用率 (0-1) |
| `vllm:prefix_cache_queries` | Counter | 前缀缓存查询次数 |
| `vllm:prefix_cache_hits` | Counter | 前缀缓存命中次数 |
| `vllm:prompt_tokens_total` | Counter | 处理的prompt tokens总数 |
| `vllm:generation_tokens_total` | Counter | 生成的tokens总数 |
| `vllm:request_success_total` | Counter | 成功完成的请求数（按finish_reason分类） |
| `vllm:request_prompt_tokens` | Histogram | 输入prompt token数分布 |
| `vllm:request_generation_tokens` | Histogram | 生成token数分布 |
| `vllm:time_to_first_token_seconds` | Histogram | TTFT（首token时间） |
| `vllm:inter_token_latency_seconds` | Histogram | Token间延迟（TPOT） |
| `vllm:e2e_request_latency_seconds` | Histogram | 端到端请求延迟 |
| `vllm:request_prefill_time_seconds` | Histogram | Prefill阶段时间 |
| `vllm:request_decode_time_seconds` | Histogram | Decode阶段时间 |

### 1.2 KV Cache生命周期指标 (采样)

通过`--kv-cache-metrics-sample`启用：

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `vllm:kv_block_lifetime_seconds` | Histogram | 采样块的生命周期（分配→驱逐） |
| `vllm:kv_block_idle_before_evict_seconds` | Histogram | 最后访问→驱逐的空闲时间 |
| `vllm:kv_block_reuse_gap_seconds` | Histogram | 块重用的间隔时间 |

### 1.3 Grafana Dashboard核心指标

vLLM官方Grafana Dashboard包含的关键指标：

```promql
# 延迟指标
vllm:e2e_request_latency_seconds_bucket
vllm:inter_token_latency_seconds  # TPOT
vllm:time_to_first_token_seconds  # TTFT

# 吞吐指标
vllm:prompt_tokens
vllm:generation_tokens
vllm:gen_throughput

# 请求状态
vllm:num_requests_running
vllm:num_requests_swapped
vllm:num_requests_waiting

# 缓存
vllm:kv_cache_usage_perc
vllm:prefix_cache_hit_rate

# 请求分布
vllm:request_prompt_tokens
vllm:request_generation_tokens
vllm:request_success

# 阶段时间
vllm:request_queue_time_seconds
vllm:request_prefill_time_seconds
vllm:request_decode_time_seconds
vllm:request_max_num_generation_tokens
```

### 1.4 重要特性

**多进程模式**: 
- 当`--api-server-count > 1`时启用multiprocess模式
- 使用`prometheus_client.multiprocess`
- 某些Python/进程指标在multiprocess模式下不可用

**HTTP中间件指标**:
```promql
http_requests_total{handler="/v1/completions",method="POST",status="2xx"}
http_request_size_bytes_count{handler="/v1/completions"}
http_response_size_bytes_count{handler="/v1/completions"}
http_request_duration_seconds_count{handler="/v1/completions",method="POST"}
```

**缓存配置信息**:
```promql
vllm:cache_config_info{block_size="16",cache_dtype="auto",gpu_memory_utilization="0.9",...}
```

---

## 2. SGLang官方指标清单

根据SGLang官方文档，`--enable-metrics`启用的完整指标：

### 2.1 Token相关指标

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `sglang:prompt_tokens_total` | Counter | Prefill tokens总数 |
| `sglang:generation_tokens_total` | Counter | Generation tokens总数 |
| `sglang:num_used_tokens` | Gauge | 当前使用的tokens数 |
| `sglang:token_usage` | Gauge | Token使用率 (0.0-1.0) |

### 2.2 延迟Histogram指标

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `sglang:time_to_first_token_seconds` | Histogram | TTFT |
| `sglang:time_per_output_token_seconds` | Histogram | TPOT（每输出token时间） |
| `sglang:e2e_request_latency_seconds` | Histogram | 端到端延迟 |
| `sglang:func_latency_seconds` | Histogram | 函数执行延迟 |
| `sglang:inter_token_latency_seconds` | Histogram | Token间延迟 |

### 2.3 请求状态Gauge指标

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `sglang:num_running_reqs` | Gauge | 运行中的请求数 |
| `sglang:num_queue_reqs` | Gauge | 等待队列中的请求数 |
| `sglang:gen_throughput` | Gauge | 生成吞吐量 (token/s) |
| `sglang:cache_hit_rate` | Gauge | 缓存命中率 (0.0-1.0) |

### 2.4 MFU相关指标（需`--enable-mfu-metrics`）

| 指标名称 | 类型 | 说明 |
|---------|------|------|
| `sglang:estimated_flops_per_gpu_total` | Counter | 估计的浮点运算数 |
| `sglang:estimated_read_bytes_per_gpu_total` | Counter | 估计的内存读取字节数 |
| `sglang:estimated_write_bytes_per_gpu_total` | Counter | 估计的内存写入字节数 |

**PromQL示例**:
```promql
# 平均TFLOPS/GPU
rate(sglang:estimated_flops_per_gpu_total[1m]) / 1e12

# 估计内存带宽 GB/s
(rate(sglang:estimated_read_bytes_per_gpu_total[1m]) + 
 rate(sglang:estimated_write_bytes_per_gpu_total[1m])) / 1e9
```

---

## 3. 三框架核心指标官方对比

### 3.1 指标命名差异对照表

| 统一概念 | MindIE | vLLM (官方) | SGLang (官方) |
|---------|--------|-------------|---------------|
| **运行中请求** | `num_requests_running` | `vllm:num_requests_running` | `sglang:num_running_reqs` |
| **等待请求** | `num_requests_waiting` | `vllm:num_requests_waiting` | `sglang:num_queue_reqs` |
| **交换请求** | - | `vllm:num_requests_swapped` | - |
| **缓存使用率** | `npu_cache_usage_perc` | `vllm:kv_cache_usage_perc` | `sglang:token_usage` |
| **前缀缓存命中** | `npu_prefix_cache_hit_rate` | `vllm:prefix_cache_hits/queries` | `sglang:cache_hit_rate` |
| **Prompt Tokens** | `prompt_tokens_total` | `vllm:prompt_tokens_total` | `sglang:prompt_tokens_total` |
| **Generation Tokens** | `generation_tokens_total` | `vllm:generation_tokens_total` | `sglang:generation_tokens_total` |
| **TTFT** | `time_to_first_token_seconds` | `vllm:time_to_first_token_seconds` | `sglang:time_to_first_token_seconds` |
| **TPOT** | `time_per_output_token_seconds` | `vllm:inter_token_latency_seconds` | `sglang:time_per_output_token_seconds` |
| **E2E延迟** | `e2e_request_latency_seconds` | `vllm:e2e_request_latency_seconds` | `sglang:e2e_request_latency_seconds` |
| **请求成功** | `request_success_total` | `vllm:request_success_total` | - |
| **Prefill时间** | - | `vllm:request_prefill_time_seconds` | - |
| **Decode时间** | - | `vllm:request_decode_time_seconds` | - |
| **生成吞吐** | `avg_generation_throughput_toks_per_s` | - | `sglang:gen_throughput` |

### 3.2 Histogram Buckets对比

**vLLM TTFT Buckets** (官方):
```
0.001, 0.005, 0.01, 0.02, 0.04, 0.06, 0.08, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0, 15.0, 20.0, +Inf
```

**SGLang TTFT Buckets** (官方):
```
0.001, 0.005, 0.01, 0.02, 0.04, 0.06, 0.08, 0.1, 0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0, 15.0, 20.0, 25.0, 30.0, +Inf
```

**差异**: SGLang的buckets延伸到30s，更适合长prompt场景

**SGLang TPOT Buckets** (官方):
```
0.005, 0.01, 0.015, 0.02, 0.025, 0.03, 0.04, 0.05, 0.075, 0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.75, 1.0, 2.5, +Inf
```

### 3.3 标签(Labels)对比

| 标签 | MindIE | vLLM | SGLang |
|------|--------|------|--------|
| **model_name** | ✅ | ✅ | ✅ |
| **finished_reason** | ❌ | ✅ (stop/length/abort) | ❌ |
| **engine_type** | ❌ | ❌ | ✅ (unified/prefill/decode) |

---

## 4. 适配层设计要点更新

### 4.1 必须支持的11个核心指标（基于官方文档）

```java
// 基于vLLM和SGLang官方文档的统一抽象
public enum CoreMetrics {
    // 请求计数
    REQUEST_RUNNING("num_requests_running", "运行中请求数"),
    REQUEST_WAITING("num_requests_waiting", "等待中请求数"),
    
    // Token计数
    PROMPT_TOKENS_TOTAL("prompt_tokens_total", "Prefill tokens总数"),
    GENERATION_TOKENS_TOTAL("generation_tokens_total", "Generation tokens总数"),
    
    // 延迟Histogram
    TTFT("time_to_first_token_seconds", "首Token时间"),
    TPOT("time_per_output_token_seconds", "Token间延迟"),
    E2E_LATENCY("e2e_request_latency_seconds", "端到端延迟"),
    
    // 资源
    CACHE_USAGE("cache_usage_percent", "缓存使用率"),
    CACHE_HIT_RATE("cache_hit_rate", "缓存命中率"),
    
    // 吞吐
    GEN_THROUGHPUT("gen_throughput", "生成吞吐量(token/s)")
}
```

### 4.2 框架特定指标映射

```yaml
# 每个框架特有的指标，适配层应保留原始名称
mindie_specific:
  - request_received_total    # MindIE有接收请求计数
  - request_failed_total      # MindIE有失败请求计数
  - num_preemptions_total     # MindIE有抢占计数

vllm_specific:
  - vllm:request_success_total       # 带finish_reason标签
  - vllm:request_prefill_time_seconds
  - vllm:request_decode_time_seconds
  - vllm:kv_block_lifetime_seconds   # KV cache生命周期

sglang_specific:
  - sglang:num_used_tokens
  - sglang:func_latency_seconds      # 函数级延迟
  - sglang:inter_token_latency_seconds
  - sglang:estimated_flops_per_gpu_total  # MFU相关
```

### 4.3 Histogram Bucket统一建议

由于三个框架的buckets不同，建议适配层提供标准化bucket：

```python
# 建议的标准化buckets（综合三个框架）
TTFT_BUCKETS = [
    0.001, 0.005, 0.01, 0.02, 0.04, 0.06, 0.08, 0.1, 
    0.25, 0.5, 0.75, 1.0, 2.5, 5.0, 7.5, 10.0, 
    15.0, 20.0, 25.0, 30.0, float('inf')
]

TPOT_BUCKETS = [
    0.005, 0.01, 0.015, 0.02, 0.025, 0.03, 0.04, 0.05, 
    0.075, 0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.75, 1.0, 
    2.5, float('inf')
]
```

---

## 5. 官方文档关键发现

### 5.1 vLLM关键发现

1. **V1引擎重构**: 文档明确指出v1引擎进行了metrics重构，很多指标从engine core移到frontend
2. **采样指标**: KV cache生命周期指标通过`--kv-cache-metrics-sample`采样，开销很小
3. **多进程限制**: multiprocess模式下某些Python/进程指标不可用
4. **缓存配置Info**: 通过Gauge暴露静态配置信息
5. **HTTP中间件**: 使用`prometheus_fastapi_instrumentator`提供HTTP级指标

### 5.2 SGLang关键发现

1. **MFU指标**: 独特的`estimated_flops_per_gpu_total`等MFU相关指标
2. **函数级延迟**: `func_latency_seconds`提供函数级别的性能分析
3. **监控Dashboard**: 官方提供Grafana dashboard配置
4. **并行配置**: 支持`tp_rank`, `pp_rank`等并行相关标签

---

## 6. 适配层实现建议

### 6.1 指标发现机制

```python
class MetricsDiscovery:
    """自动发现推理框架类型和可用指标"""
    
    def discover_framework(self, endpoint: str) -> FrameworkType:
        """通过查询/metrics端点自动识别框架"""
        response = requests.get(f"{endpoint}/metrics")
        content = response.text
        
        if "vllm:" in content:
            return FrameworkType.VLLM
        elif "sglang:" in content:
            return FrameworkType.SGLANG
        elif "request_received_total" in content:
            return FrameworkType.MINDIE
        else:
            return FrameworkType.UNKNOWN
    
    def get_available_metrics(self, endpoint: str) -> List[str]:
        """获取端点暴露的所有指标名"""
        # 解析/metrics输出，提取所有指标名
```

### 6.2 PromQL生成器

```python
class PromQLGenerator:
    """根据框架类型生成对应的PromQL"""
    
    def ttft_p99(self, framework: FrameworkType, model_name: str) -> str:
        """生成P99 TTFT查询"""
        if framework == FrameworkType.VLLM:
            return f'histogram_quantile(0.99, rate(vllm:time_to_first_token_seconds_bucket{{model_name="{model_name}"}}[5m]))'
        elif framework == FrameworkType.SGLANG:
            return f'histogram_quantile(0.99, rate(sglang:time_to_first_token_seconds_bucket{{model_name="{model_name}"}}[5m]))'
        elif framework == FrameworkType.MINDIE:
            return f'histogram_quantile(0.99, rate(time_to_first_token_seconds_bucket{{model_name="{model_name}"}}[5m]))'
```

---

## 7. 参考链接

- **vLLM Metrics Design**: https://docs.vllm.ai/en/latest/design/metrics/
- **vLLM Production Metrics**: https://docs.vllm.ai/en/latest/usage/metrics.html
- **SGLang Production Metrics**: http://docs.sglang.io/references/production_metrics.html
- **vLLM Grafana Dashboard**: https://github.com/vllm-project/vllm/blob/main/examples/online_serving/prometheus_grafana/
- **SGLang Grafana Dashboard**: https://github.com/sgl-project/sglang/blob/main/examples/monitoring/

---

**文档状态**: ✅ 基于官方最新文档
**建议**: 将此文档与`framework-metrics-research-summary.md`结合使用
