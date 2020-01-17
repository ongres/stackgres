# StackGres Connection Pooling Config

The Profile CRD represent the PostgreSQL version and params to be configured.

___

**Kind:** StackGresConnectionPoolingConfig

**listKind:** StackGresConnectionPoolingConfigList

**plural:** sgconnectionpoolingconfigs

**singular:** sgconnectionpoolingconfig
___

### **Properties**


| Property | Type | Description |
|-----------|------|-------------|
| pgbouncer.ini | object  | pgbouncer.ini configuration  |


Example:

```
apiVersion: stackgres.io/v1alpha1
kind: StackGresConnectionPoolingConfig
metadata:
  name: pgbouncerconf
  labels:
    app: stackgres
    chart: stackgres-cluster-0.8-SNAPSHOT
    heritage: Tiller
    release: stackgres
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  pgbouncer.ini:
    default_pool_size: '200'
    max_client_conn: '200'
    pool_mode: 'transaction'
```