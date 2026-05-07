package main

import (
	"fmt"
	"math/rand"
	"net/http"
	"os"
	"strings"
	"time"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

var (
	port      = envDefault("EXPORTER_PORT", "9091")
	modelList = strings.Split(envDefault("MODEL_NAMES", "llama3-70b,qwen2-7b"), ",")

	seed = time.Now().UnixNano()
	rng  = rand.New(rand.NewSource(seed))
)

func envDefault(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

func jitter(baseline, pct float64) float64 {
	return baseline * (1.0 + (rng.Float64()*2-1)*pct)
}

// ── MindIE Service Governance Metrics ─────────────────────────────
// Exact names from MindIE 2.3.0 /metrics endpoint.
// Label: model_name (MindIE convention)

var (
	// ── Counters ───────────────────────────────────────────────
	requestReceivedTotal = prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "request_received_total",
		Help: "Number of requests received so far.",
	}, []string{"model_name"})

	requestSuccessTotal = prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "request_success_total",
		Help: "Number of requests proceed successfully so far.",
	}, []string{"model_name"})

	requestFailedTotal = prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "request_failed_total",
		Help: "Number of requests failed so far.",
	}, []string{"model_name"})

	numPreemptionsTotal = prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "num_preemptions_total",
		Help: "Cumulative number of preemption from the engine.",
	}, []string{"model_name"})

	promptTokensTotal = prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "prompt_tokens_total",
		Help: "Number of prefill tokens processed.",
	}, []string{"model_name"})

	generationTokensTotal = prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "generation_tokens_total",
		Help: "Number of generation tokens processed.",
	}, []string{"model_name"})

	// ── Gauges ────────────────────────────────────────────────
	numRequestsRunning = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "num_requests_running",
		Help: "Number of requests currently running on NPU.",
	}, []string{"model_name"})

	numRequestsWaiting = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "num_requests_waiting",
		Help: "Number of requests waiting to be processed.",
	}, []string{"model_name"})

	numRequestsSwapped = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "num_requests_swapped",
		Help: "Number of requests swapped to CPU.",
	}, []string{"model_name"})

	avgPromptThroughput = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "avg_prompt_throughput_toks_per_s",
		Help: "Average prefill throughput in tokens/s.",
	}, []string{"model_name"})

	avgGenThroughput = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "avg_generation_throughput_toks_per_s",
		Help: "Average generation throughput in tokens/s.",
	}, []string{"model_name"})

	failedRequestPerc = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "failed_request_perc",
		Help: "Requests failure rate. 1 means 100 percent usage.",
	}, []string{"model_name"})

	npuCacheUsagePerc = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "npu_cache_usage_perc",
		Help: "NPU KV-cache usage. 1 means 100 percent usage.",
	}, []string{"model_name"})

	cpuCacheUsagePerc = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "cpu_cache_usage_perc",
		Help: "CPU KV-cache usage. 1 means 100 percent usage.",
	}, []string{"model_name"})

	npuPrefixCacheHitRate = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "npu_prefix_cache_hit_rate",
		Help: "NPU prefix cache block hit rate.",
	}, []string{"model_name"})

	// ── Histograms ────────────────────────────────────────────
	ttftSeconds = prometheus.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "time_to_first_token_seconds",
		Help:    "Histogram of time to first token in seconds.",
		Buckets: []float64{0.001, 0.005, 0.01, 0.02, 0.04, 0.06, 0.08, 0.1, 0.25, 0.5, 0.75, 1, 2.5, 5, 7.5, 10},
	}, []string{"model_name"})

	tpotSeconds = prometheus.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "time_per_output_token_seconds",
		Help:    "Histogram of time per output token in seconds.",
		Buckets: []float64{0.01, 0.025, 0.05, 0.075, 0.1, 0.15, 0.2, 0.3, 0.4, 0.5, 0.75, 1, 2.5},
	}, []string{"model_name"})

	e2eLatencySeconds = prometheus.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "e2e_request_latency_seconds",
		Help:    "Histogram of end to end request latency in seconds.",
		Buckets: []float64{1, 2.5, 5, 10, 15, 20, 30, 40, 50, 60},
	}, []string{"model_name"})

	requestPromptTokens = prometheus.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "request_prompt_tokens",
		Help:    "Number of prefill tokens processed.",
		Buckets: []float64{10, 50, 100, 200, 500, 1000, 2000, 5000, 10000},
	}, []string{"model_name"})

	requestGenTokens = prometheus.NewHistogramVec(prometheus.HistogramOpts{
		Name:    "request_generation_tokens",
		Help:    "Number of generation tokens processed.",
		Buckets: []float64{10, 50, 100, 200, 500, 1000, 2000, 5000, 10000},
	}, []string{"model_name"})
)

func init() {
	prometheus.MustRegister(
		requestReceivedTotal, requestSuccessTotal, requestFailedTotal,
		numPreemptionsTotal, promptTokensTotal, generationTokensTotal,
		numRequestsRunning, numRequestsWaiting, numRequestsSwapped,
		avgPromptThroughput, avgGenThroughput, failedRequestPerc,
		npuCacheUsagePerc, cpuCacheUsagePerc, npuPrefixCacheHitRate,
		ttftSeconds, tpotSeconds, e2eLatencySeconds,
		requestPromptTokens, requestGenTokens,
	)
}

// ── Model profiles ───────────────────────────────────────────────
// Each model has a personality: load level, speed, etc.
type profile struct {
	requestsPerTick    int     // new requests per scrape interval
	failRate           float64 // fraction of failed requests
	runningReqs        float64 // baseline running requests
	waitingReqs        float64
	preemptionsPerTick int
	promptToksPerReq   float64
	genToksPerReq      float64
	promptThroughput   float64
	genThroughput      float64
	cacheUsage         float64
	prefixHitRate      float64
	ttftMs             float64
	tpotMs             float64
	e2eMs              float64
}

var modelProfiles = map[string]profile{
	"llama3-70b": {
		requestsPerTick: 10, failRate: 0.0, runningReqs: 128, waitingReqs: 32,
		preemptionsPerTick: 2, promptToksPerReq: 3000, genToksPerReq: 500,
		promptThroughput: 1200, genThroughput: 800, cacheUsage: 0.85, prefixHitRate: 0.5,
		ttftMs: 500, tpotMs: 30, e2eMs: 15000,
	},
	"qwen2-7b": {
		requestsPerTick: 8, failRate: 0.01, runningReqs: 64, waitingReqs: 16,
		preemptionsPerTick: 1, promptToksPerReq: 1500, genToksPerReq: 300,
		promptThroughput: 900, genThroughput: 600, cacheUsage: 0.60, prefixHitRate: 0.35,
		ttftMs: 250, tpotMs: 18, e2eMs: 8000,
	},
}

// ── Update ───────────────────────────────────────────────────────

func updateMetrics() {
	for _, model := range modelList {
		p := modelProfiles[model]

		// Counters
		n := p.requestsPerTick
		requestReceivedTotal.WithLabelValues(model).Add(float64(n))
		succ := int(float64(n) * (1.0 - p.failRate))
		fail := n - succ
		requestSuccessTotal.WithLabelValues(model).Add(float64(succ))
		requestFailedTotal.WithLabelValues(model).Add(float64(fail))
		numPreemptionsTotal.WithLabelValues(model).Add(float64(p.preemptionsPerTick))
		promptTokensTotal.WithLabelValues(model).Add(float64(n) * p.promptToksPerReq * (0.9 + rng.Float64()*0.2))
		generationTokensTotal.WithLabelValues(model).Add(float64(succ) * p.genToksPerReq * (0.9 + rng.Float64()*0.2))

		// Gauges
		numRequestsRunning.WithLabelValues(model).Set(jitter(p.runningReqs, 0.15))
		numRequestsWaiting.WithLabelValues(model).Set(jitter(p.waitingReqs, 0.2))
		numRequestsSwapped.WithLabelValues(model).Set(0)
		avgPromptThroughput.WithLabelValues(model).Set(jitter(p.promptThroughput, 0.1))
		avgGenThroughput.WithLabelValues(model).Set(jitter(p.genThroughput, 0.1))
		failedRequestPerc.WithLabelValues(model).Set(p.failRate)
		npuCacheUsagePerc.WithLabelValues(model).Set(jitter(p.cacheUsage, 0.05))
		cpuCacheUsagePerc.WithLabelValues(model).Set(0)
		npuPrefixCacheHitRate.WithLabelValues(model).Set(jitter(p.prefixHitRate, 0.1))

		// Histograms — observe several synthetic samples per tick
		for i := 0; i < succ; i++ {
			ttftSeconds.WithLabelValues(model).Observe(jitter(p.ttftMs/1000, 0.5))
			e2eLatencySeconds.WithLabelValues(model).Observe(jitter(p.e2eMs/1000, 0.4))
			requestPromptTokens.WithLabelValues(model).Observe(jitter(p.promptToksPerReq, 0.3))
			requestGenTokens.WithLabelValues(model).Observe(jitter(p.genToksPerReq, 0.5))
		}
		for i := 0; i < n*int(p.genToksPerReq); i++ {
			tpotSeconds.WithLabelValues(model).Observe(jitter(p.tpotMs/1000, 0.4))
		}
	}
}

// ── Handler ──────────────────────────────────────────────────────

func metricsHandler() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		updateMetrics()
		promhttp.HandlerFor(prometheus.DefaultGatherer, promhttp.HandlerOpts{}).ServeHTTP(w, r)
	})
}

func main() {
	fmt.Printf("MindIE Metrics Exporter on :%s\n", port)
	fmt.Printf("Models: %v\n", modelList)
	http.Handle("/metrics", metricsHandler())
	if err := http.ListenAndServe(":"+port, nil); err != nil {
		fmt.Fprintf(os.Stderr, "Fatal: %v\n", err)
		os.Exit(1)
	}
}
