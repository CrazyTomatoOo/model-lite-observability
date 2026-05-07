package main

import (
	"fmt"
	"math/rand"
	"net/http"
	"os"
	"strings"

	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

var (
	port     = envDefault("EXPORTER_PORT", "9091")
	podNames = strings.Split(envDefault("POD_NAMES", "model-inference-7b-001,model-inference-7b-002"), ",")

	// Baselines for random-walk simulation
	baselines = map[string]map[string]float64{
		"ttft_ms":               {"model-inference-7b-001": 120.0, "model-inference-7b-002": 150.0},
		"tpot_ms":               {"model-inference-7b-001": 22.0, "model-inference-7b-002": 28.0},
		"prompt_throughput":     {"model-inference-7b-001": 1100.0, "model-inference-7b-002": 900.0},
		"decode_throughput":     {"model-inference-7b-001": 750.0, "model-inference-7b-002": 600.0},
		"qps":                   {"model-inference-7b-001": 130.0, "model-inference-7b-002": 100.0},
		"concurrent_connections": {"model-inference-7b-001": 30.0, "model-inference-7b-002": 20.0},
		"request_success_rate":   {"model-inference-7b-001": 99.5, "model-inference-7b-002": 98.5},
	}
)

func envDefault(key, def string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return def
}

func randomWalk(baseline, amplitude float64) float64 {
	return baseline * (1.0 + (rand.Float64()*2-1)*amplitude)
}

// ── Metrics ─────────────────────────────────────────────────────

var (
	ttftMs = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "ttft_ms", Help: "Time To First Token (ms)",
	}, []string{"pod"})

	tpotMs = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "tpot_ms", Help: "Time Per Output Token (ms)",
	}, []string{"pod"})

	promptThroughput = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "prompt_throughput", Help: "Prompt throughput (tokens/s)",
	}, []string{"pod"})

	decodeThroughput = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "decode_throughput", Help: "Decode throughput (tokens/s)",
	}, []string{"pod"})

	qpsMetric = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "qps", Help: "Queries Per Second",
	}, []string{"pod"})

	concurrentConns = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "concurrent_connections", Help: "Concurrent connections",
	}, []string{"pod"})

	requestSuccessRate = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "request_success_rate", Help: "Request success rate (%)",
	}, []string{"pod"})

	// Counters — name without _total; prometheus client auto-appends it
	generationTokensTotal = prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "generation_tokens_total", Help: "Total generation tokens",
	}, []string{"pod"})

	apiCallsTotal = prometheus.NewCounterVec(prometheus.CounterOpts{
		Name: "api_calls_total", Help: "Total API calls",
	}, []string{"pod"})

	// Gauges used as cumulative counters (observability service queries without _total)
	apiCallsFailed = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "api_calls_failed", Help: "Failed API calls",
	}, []string{"pod"})

	apiCallsSuccess = prometheus.NewGaugeVec(prometheus.GaugeOpts{
		Name: "api_calls_success", Help: "Successful API calls",
	}, []string{"pod"})
)

func init() {
	prometheus.MustRegister(
		ttftMs, tpotMs, promptThroughput, decodeThroughput,
		qpsMetric, concurrentConns, requestSuccessRate,
		generationTokensTotal, apiCallsTotal,
		apiCallsFailed, apiCallsSuccess,
	)
}

// ── Update loop ──────────────────────────────────────────────────

func updateMetrics() {
	for _, pod := range podNames {
		// Gauges
		ttftMs.WithLabelValues(pod).Set(max(0, randomWalk(baselines["ttft_ms"][pod], 0.20)))
		tpotMs.WithLabelValues(pod).Set(max(0, randomWalk(baselines["tpot_ms"][pod], 0.10)))
		promptThroughput.WithLabelValues(pod).Set(max(0, randomWalk(baselines["prompt_throughput"][pod], 0.15)))
		decodeThroughput.WithLabelValues(pod).Set(max(0, randomWalk(baselines["decode_throughput"][pod], 0.12)))
		qpsMetric.WithLabelValues(pod).Set(max(0, randomWalk(baselines["qps"][pod], 0.20)))
		concurrentConns.WithLabelValues(pod).Set(max(0, float64(int(randomWalk(baselines["concurrent_connections"][pod], 0.25)))))
		success := min(100.0, max(0.0, randomWalk(baselines["request_success_rate"][pod], 0.01)))
		requestSuccessRate.WithLabelValues(pod).Set(success)

		// Counters
		generationTokensTotal.WithLabelValues(pod).Add(float64(80 + rand.Intn(120)))
		apiCallsTotal.WithLabelValues(pod).Add(float64(10 + rand.Intn(20)))

		failInc := float64(rand.Intn(3))
		callInc := float64(10 + rand.Intn(20))
		apiCallsFailed.WithLabelValues(pod).Add(failInc)
		apiCallsSuccess.WithLabelValues(pod).Add(callInc - failInc)
	}
}

// ── Handler that updates metrics on each scrape ──────────────────

func metricsHandler() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		updateMetrics()
		promhttp.HandlerFor(prometheus.DefaultGatherer, promhttp.HandlerOpts{}).ServeHTTP(w, r)
	})
}

func main() {
	fmt.Printf("LLM Inference Metrics Exporter on :%s\n", port)
	fmt.Printf("Simulated pods: %v\n", podNames)
	http.Handle("/metrics", metricsHandler())
	if err := http.ListenAndServe(":"+port, nil); err != nil {
		fmt.Fprintf(os.Stderr, "Server error: %v\n", err)
		os.Exit(1)
	}
}
