# model-lite-observability Implementation Plan

> **Goal:** Implement ModelEngine observability service with REST APIs for health checks, model service discovery, topology queries, and metrics aggregation.

**Architecture:** Spring Boot 3.4.5 + Java 21 microservice with K8s Informer integration, Prometheus client, Caffeine caching, JWT security, and rate limiting.

**Tech Stack:** Spring Boot, fabric8 K8s client, Caffeine, Bucket4j, JJWT, Lombok

---

## Module Breakdown

### Layer 1: DTOs and Configuration (Foundation)
- ApiResponse, ErrorResponse DTOs
- ObservabilityProperties, CacheConfig, SecurityConfig
- Custom exceptions + GlobalExceptionHandler

### Layer 2: Clients and Informer (Infrastructure)
- PrometheusClient (query/queryRange/ping)
- K8sClient (pod/service queries)
- InformerManager + ModelInferenceInformer + PodInformer

### Layer 3: Cache and Services (Business Logic)
- CacheManager (Caffeine wrapper)
- HealthCheckService
- ModelServiceService
- TopologyService
- MetricsAggregator
- MetricsService

### Layer 4: Security and Controllers (API Layer)
- JWT authentication filter
- Security configuration
- HealthController
- ModelServiceController
- MetricsController

### Layer 5: Tests
- Unit tests for services
- Integration tests for controllers
- Mock tests for clients
