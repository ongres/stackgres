---
title: Advanced Replication Modes
weight: 12
url: /features/advanced-replication-modes
description: Make use of advanced replication modes, including async, sync and group replication.
---

Replicas are copies of the database that use the replication mechanism provided by Postgres. Those instances are updated with the latest data changes happening in the primary and allow implementing high availability and serve read-only traffic.

StackGres supports different advanced replication modes for the Postgres instances, including async, sync, and group replication. The replicas can be configured to be initialized from other replicas or using a recent backup in order to avoid loading the primary or any other instances.

You can configure the replication in the [SGCluster CRD replication section]({{% relref "06-crd-reference/01-sgcluster#sgclusterspecreplication" %}}).

Cascading replication and standby clusters on separate Kubernetes clusters for disaster recovery are also supported.

You can configure the standby cluster in the [SGCluster CRD replicateFrom section]({{% relref "06-crd-reference/01-sgcluster#sgclusterspecreplicatefrom" %}}).

