---
title: High Availability
weight: 4
url: /features/high-availability
description: High availability and automated failover
---

StackGres integrates the most renowned and production-tested high availability software for Postgres: [Patroni](https://github.com/zalando/patroni).

StackGres fully integrates Patroni, and for you there's nothing else to do.
If any pod, any node, anything fails, the cluster will perform a failover and re-heal automatically in a matter of seconds, without human intervention.

StackGres exposes one read-write (primary) and one read-only (replicas) connection for the applications via Kubernetes services, that will automatically be updated after any disruptive event happens.

Have a look at the [High Availability Guide]({{% relref "04-administration-guide/09-high-availability" %}}) to learn more about how Patroni is integrated into StackGres, how to perform manual switchovers, or how to test failovers.
