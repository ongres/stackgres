---
title: Pre-requisites
weight: 1
url: install/prerequisites
---

## Environment

As explained in the [Demo section](/demo/setenv/), for setting up the Operator and StackGres Cluster, you need to have an
environment on top of which it needs to request the necessary resources.

StackGres is able to run on any Kubernetes installation from 1.11 to 1.17 version, support for newer version, please follow up the open discussion at [#666](https://gitlab.com/ongresinc/stackgres/-/issues/666).

## Backups

<<<<<<< HEAD
All the configuration for this matter can be found at [Backup Configuration documentation](reference/backups/#configuration). By default, backups are scheduled daily (`config.backup.fullSchedule`) at `05:00 UTC` and with a retention policy (`config.backup.retention`) of 5 full-backups removed on rotation. You will have to find out the correct time window and retention policy that fit your needs.

In the next section, you'll be able to see how to done this [via Helm](install/helm/install/), with more explicit examples.
=======
### General configuration

By default backups are scheduled (`config.backup.fullSchedule`) at 05:00 UTC in a window
 (`config.backup.fullSchedule`) of 1 hour of duration and with a retention policy
 (`config.backup.retention`) of 5 for non-persistent full backups. You will have to find out the
 correct time window and retention policy that fit your needs.

There are more general fine tuning parameters that could affect backups in more aspects, you can get more info at [Backup Configuration](/03-production-installation/06-cluster-parameters/#backup-configuration):

StackGres recommend to configure all those aspects by creating a YAML values file for backup configuration, In the next section [Installation Via Helm](/03-production-installation/02-installation-via-helm/#install-operator) StackGres provides such example.
>>>>>>> 8f7f0789... Issues 682 341 686

### Storage

StackGres support Backups support the following storage options:
 
* AWS S3
* Google CLoud Storage
* Azure Blob Storage

<<<<<<< HEAD

> By default, examples are using [MinIO](https://min.io/) service as a S3 compatible service for 
> quick setups on local Kubernetes Cluster. Although, for production setups, StackGres Team recommends
> emphatically to pick a Storage as a Service for this purpose.

All the related configuration for the storage, is under backupconfig/storage section in your [Stackgres Cluster configuration file](https://gitlab.com/ongresinc/stackgres/-/blob/development/stackgres-k8s/install/helm/stackgres-cluster/values.yaml#L100-148).
=======
By default backups are stored in a [MinIO](https://min.io/) service as a separate component as a
 [StackGres cluster helm chart dependency](https://github.com/helm/charts/tree/master/stable/minio).
 MinIO is compatible with S3 service and is configured to stores the backups in a persistent volume
 with the default storage class of the kubernetes cluster. We recommend to disable the dependency
 and use a cloud provider. To disable MinIO dependency create a YAML values file for backup storage
 configuration  to include in the helm installation (`-f` or `--values` parameters) of the
 StackGres operator similar to the following:
>>>>>>> 8f7f0789... Issues 682 341 686

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

To extend the CRD for the backups, all the reference can be found at [CRD Reference Documentation](/reference/crd/#backups).

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

As early indicated in [Component of the Stack](/01-introduction/04-components-of-the-stack/#monitoring), StackGres at the moment supports Prometheus integration only. 

## Grafana Integration and Pre-requisites
As early indicated in [Component of the Stack](/01-introduction/04-components-of-the-stack/#monitoring) currently StackGres integrates only with prometheus. 

### Grafana integration Pre-requisites

#### All in one

You can install the Prometheus operator and Grafana together with StackGres operator by setting
 `prometheus-operator.create=true`, **this will install also a grafana instance and it will be
 embed with the StackGres UI automatically**

> See the [Installation Via Helm](/install/installation/helm/#install-operator) for usage examples

### Integrating Pre-existing Grafanas

If you already have a Grafana installation in your system you can embed it automatically in the
 StackGres UI by setting the property `grafana.autoEmbed=true`:

```
helm install --namespace prometheus prometheus-operator stable/prometheus-operator \
  --set grafana.autoEmbed=true
```

This method requires the installation process to be able to authenticate using administrative username and password to the Grafana's API (see [installation via helm]({{% relref "/03-production-installation/02-installation-via-helm" %}}) for more options related to automatic embedding of Grafana).

### Manual integration

Some manual steps are required in order to achieve such integration.

1. Create Grafana dashboard for Postgres exporter and copy/paste share URL:

    **Using the UI:** Click on Grafana > Create > Import > Grafana.com Dashboard 9628

    Check [the dashboard](https://grafana.com/grafana/dashboards/9628) for more details.

2. Copy/paste Grafana's dashboard URL for the Postgres exporter:

    **Using the UI:** Click on Grafana > Dashboard > Manage > Select Postgres exporter dashboard > Copy URL

    Or using the value returned by the next [script]().

3. Create and copy/paste Grafana API token:

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