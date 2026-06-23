---
title: Creating clusters
weight: 1
url: /cookbook/creating-clusters
description: Recipes for creating an SGCluster, including options that can only be set at creation time.
showToc: true
---

The recipes in this section cover the creation of an [SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}).
They start from a basic cluster and then layer on options that change how the cluster is
**bootstrapped**.

## Why creation is special

When the StackGres operator creates a cluster, the first Pod goes through a one-time
bootstrap: the persistent volume is initialized, Postgres is created (`initdb`, a restore
from a backup, or a replica clone), and any bootstrap SQL is executed. Several fields only
influence this bootstrap, so StackGres makes them **immutable**: the validating webhook
rejects any update that changes them on an existing cluster. To change them you must create
a new cluster.

The creation-only options covered here are:

{{% children style="li" depth="1" description="true" %}}

If you try to change one of these on a running cluster, `kubectl apply` fails with a
validation error from the StackGres webhook (for example, *"Cannot update SGCluster's
restore configuration"* or *"Cannot update patroni initial configuration"*). That is by
design, not a bug.
