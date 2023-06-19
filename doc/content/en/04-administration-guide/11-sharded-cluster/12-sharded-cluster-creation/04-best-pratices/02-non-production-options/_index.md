---
title: Sharded Cluster Non-Production Options
weight: 2
url: /administration/sharded-cluster/creation/nonproduction
description: Important notes for non-production options in the production environment.
---

Please refer to the [Cluster Non-Production Options]({{% relref "04-administration-guide/02-cluster-creation/04-best-pratices/02-non-production-options" %}}) page since those apply also the the Sharded Cluster.

The only difference is that, after setting those options you will have to restart the Postgres cluster Pods by simply deleting them (or using a [restart SGDbOps]({{% relref "06-crd-reference/08-sgdbops#restart" %}})) for each cluster that belongs to the sharded cluster:

```bash
kubectl get sgcluster -l app=StackGresShardedCluster,stackgres.io/shardedcluster-name=simple -o name \
  | cut -d / -f 2 \
  | while read -r CLUSTER_NAME
    do
      kubectl delete pod -l app=StackGresCluster,stackgres.io/cluster-name=$CLUSTER_NAME
    done
```
