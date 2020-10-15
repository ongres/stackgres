---
title: Pre-requisites
weight: 1
url: install/prerequisites
---

## Backups

### General configuration

By default backups are scheduled (`config.backup.fullSchedule`) at 05:00 UTC in a window
 (`config.backup.fullSchedule`) of 1 hour of duration and with a retention policy
 (`config.backup.retention`) of 5 for non-persistent full backups. You will have to find out the
 correct time window and retention policy that fit your needs.

There are more general fine tuning parameters that could affect backups in more aspects, you can get more info at [Backup Configuration](/03-production-installation/06-cluster-parameters/#backup-configuration):

StackGres recommend to configure all those aspects by creating a YAML values file for backup configuration, In the next section [Installation Via Helm](/03-production-installation/02-installation-via-helm/#install-operator) StackGres provides such example.

### Storage

Backups support the following storage options:
 
* AWS S3
* Google CLoud Storage
* Azure Blob Storage

By default backups are stored in a [MinIO](https://min.io/) service as a separate component as a
 [StackGres cluster helm chart dependency](https://github.com/helm/charts/tree/master/stable/minio).
 MinIO is compatible with S3 service and is configured to stores the backups in a persistent volume
 with the default storage class of the kubernetes cluster. We recommend to disable the dependency
 and use a cloud provider. To disable MinIO dependency create a YAML values file for backup storage
 configuration  to include in the helm installation (`-f` or `--values` parameters) of the
 StackGres operator similar to the following:

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
minio:
  create: false
```

### Restore

StackGres can perform a database restoration from a StackGres backup by just setting the UID of 
 the backup CR that represents the backup that we want to restore. Like this:

``` yaml
cluster:
  initialData:
    restore:
      fromBackup: #the backup UID to restore
```

## Monitoring

As early indicated in [Component of the Stack](/01-introduction/04-components-of-the-stack/#monitoring) currently StackGres integrates only with prometheus. 

## Grafana integration Pre-requisites

### Automatic integration

If you already have a grafana installation in your system you can embed it automatically in the
 StackGres UI by setting the property `grafana.autoEmbed=true`:

```
helm install --namespace prometheus prometheus-operator stable/prometheus-operator \
  --set grafana.autoEmbed=true
```

This method requires the installation process to be able to access grafana API as grafana
 administrator by username and password (see [installation via helm]({{% relref "/03-production-installation/02-installation-via-helm" %}})
 for more options related to automatic embedding of grafana).

### Manual integration

Some manual steps are required in order to achieve such integration.

1. Create grafana dashboard for postgres exporter and copy/paste share URL:

    **Using the UI:** Click on Grafana > Create > Import > Grafana.com Dashboard 9628

    Check [the dashboard](https://grafana.com/grafana/dashboards/9628) for more details.

2. Copy/paste grafana dashboard URL for postgres exporter:

    **Using the UI:** Click on Grafana > Dashboard > Manage > Select Postgres exporter dashboard > Copy URL

    Or using the value returned by the next [script]().

3. Create and copy/paste grafana API token:

    **Using the UI:** Grafana > Configuration > API Keys > Add API key (for viewer) > Copy key value

## Non production options

We recommend to disable all non production options in a production environment. To do so create a
 YAML values file to include in the helm installation (`-f` or `--values` parameters) of the
 StackGres operator similar to the following:

``` yaml
nonProductionOptions: {}
```

The use of MinIO in production is not considered a bad practice but we recommend to install MinIO
 separately to in order to be able to change version independently from the StackGres cluster helm
 chart.