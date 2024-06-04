---
title: Synchronous Replication Mode
weight: 2
url: /administration/replication/modes/sync
description: This section describes the involved steps and concepts under the Stackgres sync option.
---

The Stackgres `replication.mode` *sync* option instructs Stackgres to create one or more cluster members as synchronous replicas. As indicated in the [CRD reference](https://stackgres.io/doc/latest/reference/crd/sgcluster/#sgclusterspecreplication) the cluster will not block transactions in the Leader in the event of replica lost, since Patroni will switch off the synchronous replication.

# Setting up a Cluster with Synchronous replica

Move forward to the next item, creating a synchronous replication cluster:

```sh
$ cat << EOF | kubectl apply -f -
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: failover
  name: sync-cluster
spec:
  postgres:
	version: '16.1'
  instances: 3
  sgInstanceProfile: 'size-s'
  pods:
	persistentVolume:
  	size: '10Gi'
  configurations:
	sgPostgresConfig: 'pgconfig1'
	sgPoolingConfig: 'poolconfig1'
  prometheusAutobind: true
  nonProductionOptions:
	disableClusterPodAntiAffinity: true
  replication:
	mode: sync
	role: ha-read
	syncInstances: 1
EOF
```

Stackgres creates one synchronous replica and one asynchronous accordingly.

```sh
$ kubectl -n failover exec -it sync-cluster-0 -c patroni -- bash - patronictl list
+ Cluster: sync-cluster (7369946595341132525) -----+-----------+----+-----------+
| Member     	| Host         	| Role     	| State 	| TL | Lag in MB |
+----------------+------------------+--------------+-----------+----+-----------+
| sync-cluster-0 | 10.244.0.21:7433 | Leader   	| running   |  1 |       	|
| sync-cluster-1 | 10.244.0.23:7433 | Sync Standby | streaming |  1 |     	0 |
| sync-cluster-2 | 10.244.0.25:7433 | Replica  	| streaming |  1 |     	0 |
+----------------+------------------+--------------+-----------+----+-----------+
```