# StackGresPostgresConfig

The Profile CRD represent the PostgreSQL version and params to be configured.

___

**Kind:** StackGresPostgresConfig

**listKind:** StackGresPostgresConfigList

**plural:** sgpgconfigs

**singular:** sgpgconfig
___

### **Properties**


| Property | Type | Description |
|-----------|------|-------------|
| pgVersion | string  | PostgreSQL configuration version  |
| postgresql.conf | object  | List of PostgreSQL configuration parameters with their values  |


Example:

```
apiVersion: stackgres.io/v1alpha1
kind: StackGresPostgresConfig
metadata:
  name: postgresconf
  labels:
    app: stackgres
    chart: stackgres-cluster-0.8
    heritage: Tiller
    release: stackgres
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  pgVersion: "11"
  postgresql.conf:
    password_encryption: 'scram-sha-256'
    random_page_cost: '1.5'
    shared_buffers: '256MB'
    wal_compression: 'on'
```