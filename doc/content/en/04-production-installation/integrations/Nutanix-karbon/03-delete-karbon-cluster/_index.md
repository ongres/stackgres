---
title: Integrations - Nutanix Karbon
weight: 3
pre: "<b>3. </b>"
url: install/integrations/nutanix-karbon/cluster-delete
---

# StackGres Cluster Delete

Delete a SG cluster within all Postgres servers inside is not a common task, but if need to do here is the step

```sh
cat <<EOF | kubectl delete -f -
apiVersion: stackgres.io/v1beta1
kind: SGCluster
metadata:
  namespace: karbon
  name: karbon-db
spec:
  postgresVersion: '12.3'
  instances: 3
  sgInstanceProfile: 'size-s'
  pods:
    persistentVolume:
      size: '20Gi'
  configurations:
    sgPostgresConfig: 'pgconfig'
    sgPoolingConfig: 'poolconfig'
    sgBackupConfig: 'backupconfig'
  distributedLogs:
    sgDistributedLogs: 'distributedlogs'
  initialData:
    scripts:
    - name: create-admin-user
      scriptFrom:
        secretKeyRef:
          name: admin-user-password
          key: admin-create-user-sql
    - name: create-database
      script: |
        create database admin owner admin;
  prometheusAutobind: true
  nonProductionOptions:
    disableClusterPodAntiAffinity: true
EOF
```

> You should use the same YAML used to create the cluster, that is a good reason to save all this into yaml files.
