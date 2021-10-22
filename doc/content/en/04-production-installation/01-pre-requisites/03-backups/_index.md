---
title: Backups
weight: 3
url: install/prerequisites/backups
description: Details about how to setup and configure the backups. 
showToc: true
---

All the configuration for this matter can be found at [Backup Configuration documentation]({{% relref "06-crd-reference/06-sgbackup/#configuration" %}}). By default, backups are scheduled daily (`config.backup.fullSchedule`) at `05:00 UTC` and with a retention policy (`config.backup.retention`) of 5 full-backups removed on rotation. You will have to find out the correct time window and retention policy that fit your needs.

In the next section, you'll be able to see how to done this [via Helm]({{% relref "04-production-installation/02-installation-via-helm" %}}), with more explicit examples.

## Storage

StackGres support Backups with the following storage options:

* AWS S3
* Google CLoud Storage
* Azure Blob Storage
* S3-Compatible Storages:
  * DigitalOcean Spaces
  * Self-hosted MinIO


> Examples are using [MinIO](https://min.io/) service as a S3 compatible service for
> quick setups on local Kubernetes Cluster. Although, for production setups, StackGres Team recommends
> emphatically to pick a Storage as a Service for this purpose.

All the related configuration for the storage, is under `configurations.backupconfig.storage` section in your [Stackgres Cluster configuration file](https://gitlab.com/ongresinc/stackgres/-/blob/main/stackgres-k8s/install/helm/stackgres-cluster/values.yaml#L100-148).

```yaml
configurations:
  backupconfig:
    # fill the preferred storage method with
    # specific credentials and configurations
    storage:
      s3: {}
      s3Compatible: {}
      gcs: {}
      azureBlob: {}
```

To extend the CRD for the backups, all the reference can be found at [CRD Reference Documentation]({{% relref "06-crd-reference/06-sgbackup" %}}).

## Restore

StackGres can perform a database restoration from a StackGres backup by just setting the UID of
 the backup CR that represents the backup that we want to restore. Like this:

``` yaml
cluster:
  initialData:
    restore:
      fromBackup: #the backup UID to restore
```

