---
title: Server-Side Connection Pooling
weight: 6
url: features/connection-pooling
description: Integrated server-side connection pooling
---

Due to the Postgres process model, it is highly recommended to make use of connection pooling for most production scenarios.

StackGres ships with integrated server-side connection pooling out of the box.
[PgBouncer](https://www.pgbouncer.org/) is deployed as a sidecar container alongside the Postgres container.

Server-side pooling enables controlling the connections fan-in, that is, the incoming connections to Postgres, and making sure Postgres is not overwhelmed with traffic that may cause significant performance degradation.
StackGres also exports relevant connection pooling metrics to Prometheus, and specialized dashboards are shown in the Grafana integrated into the web console.

You can tune the low-level configuration or even entirely disable connection pooling via the [SGPoolingConfig CRD]({{% relref "06-crd-reference/04-sgpoolingconfig" %}}).

Have a look at the [Connection Pooling Configuration section]({{% relref "04-administration-guide/03-configuration/03-connection-pooling" %}}) to learn more about how to configure connection pooling.
