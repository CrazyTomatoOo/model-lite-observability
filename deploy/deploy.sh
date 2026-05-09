#!/bin/bash
set -euo pipefail

# ------------------------------------------------------------------
# deploy.sh - Build and deploy model-lite-observability locally
# Usage: ./deploy.sh [--cluster <kind|minikube|k3d>] [--namespace <ns>] [--skip-build]
# ------------------------------------------------------------------

CLUSTER="kind"
NAMESPACE="model-engine"
SKIP_BUILD=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --cluster)
      if [[ -z "${2:-}" ]]; then
        echo "ERROR: --cluster requires an argument (kind, minikube, or k3d)" >&2
        exit 1
      fi
      CLUSTER="$2"
      shift 2
      ;;
    --namespace)
      if [[ -z "${2:-}" ]]; then
        echo "ERROR: --namespace requires an argument" >&2
        exit 1
      fi
      NAMESPACE="$2"
      shift 2
      ;;
    --skip-build)
      SKIP_BUILD=true
      shift
      ;;
    -*)
      echo "ERROR: Unknown option: $1" >&2
      echo "Usage: $0 [--cluster <kind|minikube|k3d>] [--namespace <ns>] [--skip-build]" >&2
      exit 1
      ;;
    *)
      echo "ERROR: Unexpected argument: $1" >&2
      exit 1
      ;;
  esac
done

# Validate cluster type
case "$CLUSTER" in
  kind|minikube|k3d) ;;
  *)
    echo "ERROR: Invalid cluster type '$CLUSTER'. Must be one of: kind, minikube, k3d" >&2
    exit 1
    ;;
esac

# ── Banner ───────────────────────────────────────────────────────
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║        model-lite-observability - Local Deploy              ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  Cluster   : $CLUSTER"
echo "║  Namespace : $NAMESPACE"
echo "║  Skip build: $SKIP_BUILD"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""


SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# ── Step 1: Docker build ─────────────────────────────────────────
if [ "$SKIP_BUILD" = false ]; then
  echo ">>> Building Docker image: model-lite-observability:latest ..."
  echo ">>> Building Java application locally (mvn clean package -DskipTests) ..."
  mvn clean package -DskipTests -q -f "${SCRIPT_DIR}/../pom.xml"

  docker build -t model-lite-observability:latest ..

fi

# ── Step 2: Load images into cluster ────────────────────────────
echo ">>> Loading images into $CLUSTER cluster ..."
case "$CLUSTER" in
  kind)
    kind load docker-image model-lite-observability:latest
    ;;
  minikube)
    minikube image load model-lite-observability:latest
    ;;
  k3d)
    ;;
esac
echo ""
# ── Step 3: Apply Kubernetes manifests ───────────────────────────

echo ">>> Applying CRD manifests ..."
kubectl apply -f "${SCRIPT_DIR}/../k8s/crds/"

echo ">>> Applying namespace manifest ..."
kubectl apply -f "${SCRIPT_DIR}/../k8s/namespace.yaml"
echo ">>> Applying RBAC manifests ..."
kubectl apply -f "${SCRIPT_DIR}/../k8s/observability/rbac.yaml"


echo ">>> Applying observability manifests ..."
kubectl apply -f "${SCRIPT_DIR}/../k8s/observability/"
echo ""
echo ">>> Waiting for model-lite-observability rollout ..."
kubectl rollout status deployment/model-lite-observability -n "$NAMESPACE" --timeout=120s
echo ""

# ── Step 5: Success message ──────────────────────────────────────
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  Deployment complete!                                       ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  App:                                                        ║"
echo "║    kubectl port-forward svc/model-lite-observability \\"
echo "║      8080:8080 -n $NAMESPACE                                 "
echo "║    curl http://localhost:8080/model/observability/v1/health  "
echo "╚══════════════════════════════════════════════════════════════╝"
