---
title: OpenTelemetry Collector
weight: 4
url: /administration/monitoring/opentelemetry-collector
description: How to configure the OpenTelemetry Collector for metrics collection.
showToc: true
---

StackGres uses an [OpenTelemetry Collector](https://opentelemetry.io/docs/collector/) to gather metrics from PostgreSQL clusters and expose them to monitoring systems like Prometheus.

## Overview

The OpenTelemetry Collector acts as a central hub for metrics:

```
┌─────────────────┐     ┌─────────────────────┐     ┌─────────────────┐
│  SGCluster      │────▶│  OpenTelemetry      │────▶│  Prometheus     │
│  (metrics)      │     │  Collector          │     │                 │
└─────────────────┘     └─────────────────────┘     └─────────────────┘
        │                         │
        │                         │
┌───────▼─────────┐               │
│  Envoy          │───────────────┘
│  (proxy metrics)│
└─────────────────┘
```

## Default Configuration

By default, StackGres deploys an OpenTelemetry Collector as part of the operator installation. The collector:

- Scrapes metrics from PostgreSQL exporters
- Scrapes Envoy proxy metrics
- Exposes metrics in Prometheus format
- Integrates with Prometheus Operator (if installed)

## Collector Configuration

### Via Helm Values

Configure the collector during StackGres operator installation:

```yaml
# values.yaml
collector:
  enabled: true
  config:
    receivers:
      prometheus:
        config:
          scrape_configs:
            - job_name: 'stackgres'
              scrape_interval: 30s
    exporters:
      prometheus:
        endpoint: "0.0.0.0:9090"
    service:
      pipelines:
        metrics:
          receivers: [prometheus]
          exporters: [prometheus]
```

### Via SGConfig

Configure the collector through the SGConfig CRD:

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-config
  namespace: stackgres
spec:
  collector:
    config:
      exporters:
        prometheus:
          endpoint: "0.0.0.0:9090"
      receivers:
        otlp:
          protocols:
            grpc:
              endpoint: "0.0.0.0:4317"
            http:
              endpoint: "0.0.0.0:4318"
```

## Receiver Configuration

### Prometheus Receiver

Configure how the collector scrapes metrics:

```yaml
spec:
  collector:
    receivers:
      prometheus:
        enabled: true
        # Additional Prometheus scrape configs
```

### OTLP Receiver

Enable OTLP protocol for receiving metrics:

```yaml
spec:
  collector:
    config:
      receivers:
        otlp:
          protocols:
            grpc:
              endpoint: "0.0.0.0:4317"
            http:
              endpoint: "0.0.0.0:4318"
```

## Exporter Configuration

### Prometheus Exporter

Configure the Prometheus endpoint:

```yaml
spec:
  collector:
    config:
      exporters:
        prometheus:
          endpoint: "0.0.0.0:9090"
          namespace: stackgres
          const_labels:
            environment: production
```

## Prometheus Operator Integration

If you have Prometheus Operator installed, StackGres can automatically create PodMonitor/ServiceMonitor resources.

### Enable Prometheus Operator Integration

```yaml
apiVersion: stackgres.io/v1
kind: SGConfig
metadata:
  name: stackgres-config
  namespace: stackgres
spec:
  collector:
    prometheusOperator:
      # Allow discovery of Prometheus instances in all namespaces
      allowDiscovery: true
      # Create monitors automatically
      # monitors:
      # - name: prometheus
```

### Prometheus Auto-Bind

Enable automatic binding to discovered Prometheus instances:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  configurations:
    observability:
      prometheusAutobind: true
```

This automatically creates the necessary ServiceMonitor resources.

## Cluster-Level Observability

### Configure Per-Cluster Observability

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: my-cluster
spec:
  configurations:
    observability:
      # Enable/disable metrics collection
      disableMetrics: false
      # Prometheus auto-discovery
      prometheusAutobind: true
      # Receiver name for collector scraper
      receiver: my-receiver
```

### Disable Metrics for Specific Clusters

For clusters where you don't need metrics:

```yaml
spec:
  configurations:
    observability:
      disableMetrics: true
```

## Collector Deployment

### Scaling the Collector

Configure multiple collector replicas:

```yaml
spec:
  collector:
    receivers:
      enabled: true
      deployments: 2  # Number of collector deployments
```

### Resource Configuration

Set resource limits for the collector:

```yaml
# Helm values
collector:
  resources:
    requests:
      cpu: 100m
      memory: 128Mi
    limits:
      cpu: 500m
      memory: 512Mi
```

## Custom Metrics Pipeline

### Adding Custom Processors

```yaml
spec:
  collector:
    config:
      processors:
        batch:
          timeout: 10s
          send_batch_size: 1000
        memory_limiter:
          check_interval: 1s
          limit_mib: 400
      service:
        pipelines:
          metrics:
            receivers: [prometheus, otlp]
            processors: [memory_limiter, batch]
            exporters: [prometheus]
```

## TLS Configuration

### Enable TLS for Collector

```yaml
spec:
  collector:
    config:
      receivers:
        otlp:
          protocols:
            grpc:
              endpoint: "0.0.0.0:4317"
              tls:
                cert_file: /etc/ssl/certs/collector.crt
                key_file: /etc/ssl/private/collector.key
```

## Monitoring the Collector

### Check Collector Status

```bash
# View collector pods
kubectl get pods -n stackgres -l app=stackgres-collector

# View collector logs
kubectl logs -n stackgres -l app=stackgres-collector

# Check metrics endpoint
kubectl port-forward -n stackgres svc/stackgres-collector 9090:9090
curl http://localhost:9090/metrics
```

### Collector Health Metrics

The collector exposes its own health metrics:

- `otelcol_receiver_received_metric_points`: Received metric points
- `otelcol_exporter_sent_metric_points`: Exported metric points
- `otelcol_processor_dropped_metric_points`: Dropped metric points

## Best Practices

1. **Enable Prometheus Operator integration**: Simplifies metrics discovery
2. **Use auto-bind**: Let StackGres automatically configure monitoring
3. **Set appropriate scrape intervals**: Balance freshness vs. load (30s default)
4. **Configure resource limits**: Prevent collector from consuming excessive resources
5. **Monitor the collector**: Use collector's own metrics to track health

## Related Documentation

- [Monitoring Overview]({{% relref "04-administration-guide/08-monitoring" %}})
- [PostgreSQL Exporter Metrics]({{% relref "04-administration-guide/08-monitoring/02-postgres_exporter-metrics" %}})
- [Envoy Metrics]({{% relref "04-administration-guide/08-monitoring/01-envoy-metrics" %}})
