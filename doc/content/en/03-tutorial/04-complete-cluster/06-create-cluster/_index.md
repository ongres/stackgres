---
title: Create the Cluster
weight: 6
url: tutorial/complete-cluster/create-cluster
---

In the section [Create a simple cluster]({{% relref "03-tutorial/03-simple-cluster" %}}) it was already presented how to
create a simple cluster. Here a more advanced cluster will be created, referencing all the configurations and
infrastructure already prepared.

For more information, review the [SGCluster]({{% relref "06-crd-reference/01-postgres-clusters" %}}) CRD specification.
Create the file `sgcluster-cluster1.yaml` with the following content:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: demo
  name: cluster
spec:
  postgresVersion: '12.3'
  instances: 3
  sgInstanceProfile: 'size-small'
  pods:
    persistentVolume:
      size: '10Gi'
  configurations:
    sgPostgresConfig: 'pgconfig1'
    sgPoolingConfig: 'poolconfig1'
    sgBackupConfig: 'backupconfig1'
  distributedLogs:
    sgDistributedLogs: 'distributedlogs'
  prometheusAutobind: true
```

and deploy to Kubernetes:

```plain
kubectl apply -f sgcluster-cluster1.yaml
```

You may watch pod and container creation:

```bash
kubectl -n demo get pods --watch
```

or from the Web Console:

![Cluster Creation](cluster-creation.png "Cluster Creation")

While the cluster is being created, you may notice a blip on the distributed logs server, where a container is
restarted. This is a normal process, and does only pause temporarily the collection of logs (no logs are lost, since
they are buffered on the source pods). This is caused by a re-configuration which requires a container restart.
