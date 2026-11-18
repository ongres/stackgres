---
title: Distributed Logs
weight: 7
url: /features/distributed-logs
description: Distributed logs for Postgres and Patroni
---

For those of you who are tired of typing `kubectl logs` for each and one of the many pods of your cluster, to then `grep` and `awk` the Postgres logs to get the information you are looking for, there's a better solution with StackGres.

StackGres supports centralized, distributed logs for Postgres and Patroni.
A distributed log cluster can be created and configured quickly via YAML-based CRDs or via the Web Console.

Both Postgres and Patroni container logs will be captured via a [FluentBit](https://fluentbit.io/) sidecar, which will forward them to the distributed log server.
It contains in turn a [Fluentd](https://www.fluentd.org/) collector that forwards the logs to a dedicated Postgres database.
To support high log volume ingestion, this log-dedicated database is enhanced via the TimescaleDB extension, on which StackGres also relies to perform log retention policies.

The aggregated logs can then be queried via SQL from the centralized location or visualized via the Web Console, which includes search and filter capabilities.
The logs are enhanced with rich metadata, which helps for any Postgres troubleshooting.

Have a look at the [Distributed Logs Guide]({{% relref "04-administration-guide/12-distributed-logs" %}}) to learn more about how to configure distributed logs.