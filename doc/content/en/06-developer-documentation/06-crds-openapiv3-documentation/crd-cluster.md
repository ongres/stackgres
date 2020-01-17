# StackGresCluster

The cluster CRD represent the main params to create a new StackGres cluster.

___

**Kind:** StackGresCluster

**listKind:** StackGresClusterList

**plural:** sgclusters

**singular:** sgcluster
___

### **Properties**


| Property | Type | Description |
|-----------|------|-------------|
| instances | integer  | Number of instances to be created  |
| pgVersion | string  | PostgreSQL version for the new cluster  |
| pgConfig | string  | PostgreSQL configuration to apply  |
| connectionPoolingConfig | string  | Pooling configuration to apply  |
| resourceProfile | string  | Resource profile size to apply  |
| volumeSize | string  | Storage volume size  |
| storageClass | string  | Storage class name to be used for the cluster  |
| sidecars | array  | List of sidecars to include in the cluster  |
| [nonProduction](#non-production-options)  | array  | Additional parameter for non production environments  |

#### Non Production options
| Property | Type | Description |
|-----------|------|-------------|
| disableClusterPodAntiAffinity | boolean | Disable the pod Anti-Affinity rule |

Example:

```
apiVersion: stackgres.io/v1alpha1
kind: StackGresCluster
metadata:
  name: stackgres
  labels:
    app: stackgres
    chart: stackgres-cluster-0.8-SNAPSHOT
    heritage: Tiller
    release: stackgres
spec:
  instances: 3
  pgVersion: '11.6'
  pgConfig: 'postgresconf'
  connectionPoolingConfig: 'pgbouncerconf'
  resourceProfile: 'size-xs'
  backupConfig: 'backupconf'
  volumeSize: '5Gi'
  prometheusAutobind: true
  sidecars:
  - connection-pooling
  - postgres-util
  - prometheus-postgres-exporter
  nonProduction:
    disableClusterPodAntiAffinity: true
```