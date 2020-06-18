# Release 0.9-beta3

## FIXES

* Backup configuration is not shown on edit cluster form
* Edit form of a cluster is not filled when no distributed logs exists
* Internal error when deleting a distributed log CR
* Wrong notes in operator helm chart

# Release 0.9-beta2

## NOTES

* StackGres pods now run with non-root user (or arbitrary user in OpenShift)
* StackGres run without anyuid in OpenShift
* UI and REST API now run in its own pod
* UI authentication now uses JWT instead of basic authentication
* Many more UI improvements

## FIXES

* Using fixed version 8.13.4 for prometheus-operator dependency
* Using fixed version 5.0.26 for MinIO dependency

# Release 0.9-beta1

## NOTES

* Logs can now be collected and analyzed in the UI or REST API
* OpenShift 3.11 support added, tested on MiniShift.
* Custom annotations and labels can now be specified for StackGres cluster pods.
* Patroni 1.6.5
* WAL-G 0.2.15
* Many UI improvements

## FIXES

* When deleting a `SGBackup` CR, the backup in the object storage is not deleted
* Automatic backups are removed when the cluster is removed
* Manual backups are apparently not working
* Connection does not work when SG cluster pod is "Ready"
* Creation of cluster for pg 11 with default configuration fail

## KNOWN ISSUES

* Kubernetes 1.18 is not supported yet, see #439 
* Kubernetes 1.11 requires PodShareProcessNamespace feature gate to be enabled (not tested in any kubernetes environment except for MiniShift 3.11)

# Release 0.9-alpha1

## NOTES

* New Shiny UI
* Improved REST API
* Version 1beta1 of StackGres CRDs
* Improved grafana integration
* Updated PostgreSQL to 12.2 and 11.7, Patroni 1.6.4 and Envoy 1.13.0
* Support for helm 3.x

## FIXES

* Upgrade of helm chart release give "resource already exists" error.
* Error on POST /stackgres/connpoolconfig if kind and apiVersion are not specified
* Error on create backup conf in namespace that is not the operator one
* /status/isPermanent is not updated when /spec/isPermanent is changed
* Backup job to store on S3 never adquire the lock

# Release 0.8

## NOTES

* Avoid modify backup configuration that could make backups unusable
* Improved UI for backups
* Allow install in kubernetes up to version 1.17 in helm chart
* Added backup configuration in backup CR
* Removed restore configuration CRD

## FIXES

* Added missing cleanups to init job in operator helm chart
* Default pgVersion of helm, now match the latest one
* Allow non breaking changes in prometheus CRDs
* Allow to create a cluster in the operator namespace
* Not scaling down when primary is not the last pod
* Cluster pods endpoint returns only pods belonging to cluster

## KNOWN ISSUES

* Backup /status/isPermanent is not updated when /spec/isPermanent is changed
* Restore does not validate backup version so a cluster could hang when restoring a backup of
 another version of PostgreSQL
* Backup configuration stored in backups could be different from used one
* Restoration fails if the new cluster is on a different namespace that the backup storage
* Google Cloud Storage can not be used as backup storage due to a bug

# Release 0.8-alpha3

* First documentation complete draft
* Create ServiceMonitor to export envoy metrics to prometheus
* Allow to set latest or just major version for postgresql
* If no sidecars are specified all will be enabled

## FIXES

* Fixed backup and restore config in cluster CRD open API v3 schema
* Postgres exporter can not connect to postgresql
* Removed version check in CRD since already part of validation
* Endpoint /stackgres/pods/{namespace}/{name} returns 404 on existing clusters
* Added prometheusAutobind to cluster CR

# Release 0.8-alpha2

## NOTES

* Addded backup CR
* Annotate StackGres CRs with operator version
* Support for backup restoration
* Validation of CR structure

## FIXES

* Prevent reconciliation cycle to fail when not able to send an event
* Fixed search of prometheus service monitor orphans
* Correct response POJO when returning error for admission webhooks

# Release 0.8-alpha1

## NOTES

* Added automatic backups
* Added defaults CRs
* Non production options (to run many instances of a cluster in a single node)
* Developer options (log level and debug mode)
* Default backup storage with MinIO
* Allow postgres and pgbouncer parametrization from helm chart
* Generate default global and immutable configurations for stackgres clusters
* Added support for kubernetes 1.16
* Deleting clusters using foreground propagation policy
* Allow to scale down preventing disruption of master
* Added owner reference to all resourced created for a cluster
* Updated postgres to version 11.6,12.1 and patorni to version 1.6.1
* Updated pgbouncer version to 1.12.0
* Updated envoy version to 1.12.1 and add sidecar by default
* Updated postgres exporter version to 0.8.0
* Profile and pg configs can now be created, updated and deleted through the operator REST API.
* Rename app properties of CRD to use camelcase names

## FIXES

* Improved UI response time
* Avoid validate components versions in CRD open API v3 spec
* Archive command should never be specified by the user so it's now blacklisted
* Multiple UI usability fixes

# Release 0.7.1

## NOTES

* Added documentation
* Use default storage class in cluster helm chart

## FIXES

* Allow null and empty storage class names
* Use ClusterIP instead of LoadBalancer in stackgres services
* Added permissions to modify service monitors
* Multiple fixes and improvement in the UI
* Fixed certificate unknown authority when reinstalling operator
* Fixed the prometheus integration was colliding if two stackgres cluster has the same name

# Release 0.7

## NOTES

This release includes new functionalities and bug fixes. In particular you will be able to connect your StackGres cluster to an existing prometheus (automatically if you are using prometheus-operator in the same k8s cluster):

```
helm install stable/prometheus-operator
helm install --name stackgres-operator operator/install/kubernetes/chart/stackgres-operator
helm install --name stackgres-cluster operator/install/kubernetes/chart/stackgres-cluster
```

## CHANGELOG

* [Prometues postgres exporter](https://github.com/wrouesnel/postgres_exporter) sidecar with option to autobind to prometheus
* A flaming new StackGres Web UI that will allow to monitor the StackGres cluster
* Integration with grafana dashboard in the StackGres Web UI
* Validation admission webhooks and openAPIV3Schema validations to check correcteness of the created StackGres cluster and configurations CRs
* Support for PostgreSQL 12.0

# Release 0.6

## NOTES

This release includes new functionalities and bug fixes. Among other we added support for connection pooling with pgbouncer and the ability to install StackGres operator and StackGres clusters using helm:

```
helm install --name stackgres-operator operator/install/kubernetes/chart/stackgres-operator
helm install --name stackgres-cluster operator/install/kubernetes/chart/stackgres-cluster
```

## CHANGELOG

* Operator and cluster helm charts
* Connection pooling with pgbouncer.
* Anty affinity pattern to have only one postgresql running for each kubernetes node

