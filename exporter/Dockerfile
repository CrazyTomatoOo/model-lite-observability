# Stage 1: Build the Go binary
FROM golang:1.24-alpine AS builder

WORKDIR /build
COPY go.mod go.sum ./
RUN go mod download
COPY main.go .
RUN CGO_ENABLED=0 go build -o exporter .

# Stage 2: Minimal runtime
FROM busybox:1.36

WORKDIR /app
COPY --from=builder /build/exporter .

EXPOSE 9091
CMD ["./exporter"]
