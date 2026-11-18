---
title: Observability
weight: 8
url: /features/observability
description: "Observability: Prometheus Grafana integration, Envoy proxy"
---

StackGres makes Postgres metrics available for enhanced observability and fully integrates with the Prometheus stack, including pre-defined, Postgres-specific dashboards and alerts.

StackGres utilizes an Envoy sidecar to transparently proxy all Postgres traffic.
The OnGres team has developed, in collaboration with the Envoy Community, the first [Postgres filter for Envoy](https://www.cncf.io/blog/2020/08/13/envoy-1-15-introduces-a-new-postgres-extension-with-monitoring-support/).
This Envoy Postgres filter provides enhanced observability by decoding the Postgres wire protocol and sending metrics to Prometheus.
This not only adds metrics that would be unavailable at the Postgres level, but also has zero impact on Postgres, since the metrics are gathered at the proxy level.
This process is fully transparent to Postgres.

Envoy will send the additional metrics, and as long as there is a Prometheus instance configured in Kubernetes, there's nothing more to do.

The StackGres Web Console includes built-in Grafana dashboards to visualize these metrics.

Have a look at the [Monitoring Guide]({{% relref "04-administration-guide/08-monitoring" %}}) to learn more about how to configure monitoring.
