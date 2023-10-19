# Generating the CRs one by one
> Can you copy and paste this information in your server for deployment
* Create the next yaml files and these must be executed in the same order as shown below:

 1. profiles-crs.yaml
 1. pgconfig-cr.yaml
 1. pgbouncerconfig-cr.yaml
 1. cluster-cr.yaml

## 1.- Create each file with the content below:

`profiles-cr.yaml` Custom resources for instances size(memory and cpu):

```
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  name: size-s
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  cpu: "500m"
  memory: "512Mi"
---
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  name: size-s
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  cpu: "1"
  memory: "2Gi"
---
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  name: size-m
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  cpu: "2"
  memory: "4Gi"
---
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  name: size-l
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  cpu: "4"
  memory: "8Gi"
---
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  name: size-xl
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  cpu: "6"
  memory: "16Gi"
---
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  name: size-xxl
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  cpu: "8"
  memory: "32Gi"
```
`pgconfig-cr.yaml`  Custom resource for PosgreSQL configuration:
```
apiVersion: stackgres.io/v1
kind: SGPostgresConfig
metadata:
  name: postgresconf
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  postgresVersion: "11"
  postgresql.conf:
      shared_buffers: '256MB'
      random_page_cost: '1.5'
      password_encryption: 'scram-sha-256'
      wal_compression: 'on'
```

`pgbouncerconfig-cr.yaml` Custom resource for pgbouncer configuration:

```
apiVersion: stackgres.io/v1
kind: SGPoolingConfig
metadata:
  name: pgbouncerconf
  annotations:
    "helm.sh/hook": "pre-install"
spec:
  pgbouncerVersion: "1.12.0"
  pgbouncer.ini:
      pool_mode: transaction
      max_client_conn: '200'
      default_pool_size: '200'
```
`cluster-cr.yaml` Custom resource for StackGres cluster

``` yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: jaundiced-ladybug
spec:
  instances: 1
  postgresVersion: '11.6'
  sgInstanceProfile: 'size-s'
  configurations:
    sgPostgresConfig: 'postgresconf'
    sgPoolingConfig: 'pgbouncerconf' 
  pods:
    persistentVolume:
      size: '5Gi'
  postgresExporterVersion: '0.7.0'
  prometheusAutobind: true

```
### 2.- Once you have the files created, apply it to the k8s cluster:

```
kubectel apply -f _your_directory/profiles-cr.yaml
kubectel apply -f _your_directory/pgconfig-cr.yaml
kubectel apply -f _your_directory/pgbouncerconfig-cr.yaml
kubectel apply -f _your_directory/cluster-cr.yaml

```

> Note: This last file will create all the StackGres cluster resources


## Now, you can continue with the previous manual depends you cloud :

[azure verify the cluster ](https://gitlab.com/sancfc/sg/blob/master/azure.md#72-verify-the-cluster)

[aws verify the cluster](https://gitlab.com/sancfc/sg/blob/master/aws.md#72-verify-the-cluster)

[GCP verify the cluster](https://gitlab.com/sancfc/sg/blob/master/gcloud.md#72-verify-the-cluster)
