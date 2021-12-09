---
title: Init cluster from backup
weight: 2
url: tutorial/using-initialdata/data-from-backup
description: "Details about use initialData section to create a cluster from a backup"
---

The `initialData` section allows you to create a new cluster and initilized the `PGDATA` from an existing backup. First you need to identify the backup you want to restore and get the backup's `UID`:

```bash
kubectl get sgbackups --namespace $NAMESPACE $CLUSTER_BACKUP_NAME -o jsonpath="{.metadata.uid}"
```

This command will print the UID:

```
0a3bb287-6b3f-4309-87bf-8d7c4c9e1beb
```

Create the file `sgcluster-from-backup.yaml` that contains the UID from the previous step:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  name: stackgres
spec:
  initialData:
    restore:
      fromBackup: 0a3bb287-6b3f-4309-87bf-8d7c4c9e1beb
```

and deploy it to Kubernetes:

```bash
kubectl apply -f sgcluster-from-backup.yaml
```

Check the complete explanation about restoring a backup in the [Restore a backup Runbook.]({{% relref "09-runbooks/03-restore-backup" %}})