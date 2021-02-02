# Release 0.9.4

## NOTES

Here it comes StackGres 0.9.4!! :tada: :bottle_with_popping_cork: 

We want you to be safe and a bit more powerful so we bring to you some bugfixes and small changes!

## UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

```
NAMESPACE=stackgres
RELEASE=stackgres-operator
helm upgrade -n "$NAMESPACE" "$RELEASE" https://stackgres.io/downloads/stackgres-k8s/stackgres/0.9.4/helm/stackgres-operator.tgz
```

## CHANGES

* Added support for node tolerations
* Added support to pause reconciliation cycle for generated resources
* Show StackGres version in the admin UI console

## FIXES

* Use primary service backed by Patroni's managed Endpoints to avoid any possible data loss
* fluent-bit stop reading logs if line size is more than default buffer size (32k)
* After upgrade to ~"affected_version::0.9.3" the prometheus postgres exporter stop working due to missing functions
* Resources does not get the annotation after updating the sgcluster specs
* Backup Job does not release lock when it expire
* Removed prometheus-operator dependecy from StackGres operator helm chart since deprecated
* Multiple UI bugfixes

# KNOWN ISSUES

* Kubernetes 1.18 is not supported yet, see #439
* Kubernetes 1.11 requires PodShareProcessNamespace feature gate to be enabled (not tested in any kubernetes environment except for MiniShift 3.11)

# Release 0.9.3

## NOTES

Here it comes StackGres 0.9.3!! :tada: :bottle_with_popping_cork: 

We want you to be safe and a bit more powerful so we bring to you some bugfixes and small changes!

## UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

```
NAMESPACE=stackgres
RELEASE=stackgres-operator
helm upgrade -n "$NAMESPACE" "$RELEASE" https://stackgres.io/downloads/stackgres-k8s/stackgres/0.9.3/helm/stackgres-operator.tgz
```

## CHANGES

* Added cluster namespace and name labels to prometheus postgres exporter metrics
* Added pgbouncer and disk stats to prometheus postgres exporter metrics and dashboard

## FIXES

* Fixed default pooling configuration to not limit downstream connections to postgres (they are now limited to [`max_connections`](https://postgresqlco.nf/en/doc/param/max_connections/)). This only affect installation that uses default pooling configuration.
* Fixed slow queries in prometheus postgres exporter sidecar for table and index bloats
* Fixed some grafana dashboard panels units
* Added workaround for JIT memory leak in postgres. See https://www.postgresql.org/message-id/flat/20201111121420.GA666413%40roeckx.be#81aedc67713fbc01b4443ee586580fb5 
* Fixed some UI bugs

# KNOWN ISSUES

* Kubernetes 1.18 is not supported yet, see #439
* Kubernetes 1.11 requires PodShareProcessNamespace feature gate to be enabled (not tested in any kubernetes environment except for MiniShift 3.11)

# Release 0.9.2

## NOTES

Here it comes StackGres 0.9.2!! :tada: :bottle_with_popping_cork: 

We want you to be safe so we bring to you some buigfixes!

## UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release. For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/03-production-installation/02-installation-via-helm/#upgrade-operator).

To upgrade StackGres operator's helm chart issue following commands (replace namespace and release name if you used something different):

```
NAMESPACE=stackgres
RELEASE=stackgres-operator
helm upgrade -n "$NAMESPACE" "$RELEASE" https://stackgres.io/downloads/stackgres-k8s/stackgres/0.9.2/helm-operator.tgz
```

## CHANGES

* Reconciliation cycle can now be stopped, by annotating sgclusters or sgdistributedlogs with `stackgres.io/reconciliation: skip`

## FIXES

* Ensure StackGres pods have enough Shared Memory (SHM)
* pgBouncer configuration is repeting parameters in the pgbouncer.ini file
* UI: Wrong mapping of diskPsiAvg* cluster status props

# KNOWN ISSUES

* Kubernetes 1.18 is not supported yet, see #439
* Kubernetes 1.11 requires PodShareProcessNamespace feature gate to be enabled (not tested in any kubernetes environment

# Release 0.9.1

## NOTES

Here it comes StackGres 0.9.1!! :tada: :bottle_with_popping_cork: 

We want you to be safe and cool so we bring to you some component version upgrades and buigfixes!

## UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release. For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/03-production-installation/02-installation-via-helm/#upgrade-operator).

To upgrade StackGres operator's helm chart issue following commands (replace namespace and release name if you used something different):

```
NAMESPACE=stackgres
RELEASE=stackgres-operator
helm upgrade -n "$NAMESPACE" "$RELEASE" https://stackgres.io/downloads/stackgres-k8s/stackgres/0.9.1/helm-operator.tgz
```

This release comes with new images that fix a bug with the [JIT library not present in previous version](https://gitlab.com/ongresinc/stackgres/-/issues/648). To upgrade your running cluster refer to [our cluster restart documentation](https://stackgres.io/doc/latest/03-production-installation/04-cluster-restart)

## CHANGES

* PostgreSQL version 12.4 and 11.9 added
* Removing MinIO as a helm dependency

## FIXES

* Disable the 'idle_timeout' from TcpProxy to prevent finish connections for long running queries.
* Pod does not recover after failover if pg_rewind fails
* Performance drop when accessing via the service
* PendingRestart condition is not updating when expected
* Script stored on secret or configmap are not returned by REST API
* Only postgres database stats are collected by postgres-exporter
* Grafana dashboard does not refresh in the operator admin UI
* Seq page cost show an error in grafana dashboard of operator admin UI
* Unproper rendering of long namespace names in the operator admin UI
* Cluster status do not show the message when one node is failing in the operator admin UI
* Various fixes and small improvements in the operator admin UI
* Helm chart init jobs uses the same service account used by the operator
* Helm chart grafana integration fail if grafana.secret* are specified 

# KNOWN ISSUES

* Kubernetes 1.18 is not supported yet, see #439
* Kubernetes 1.11 requires PodShareProcessNamespace feature gate to be enabled (not tested in any kubernetes environment except for MiniShift 3.11)

# Release 0.9

## NOTES

Here it comes StackGres 0.9!! :tada: :bottle_with_popping_cork: 

The most prominent new feature in this release is easy access to postgres logs with an easy to use interface (through the we UI). It is backed by a dedicated postgres instance, with his own special CR called `SGDistributedLogs`. But even if that is the main feature of this release there are many other new features, changes and fixes that we are proud to bring to you.

To make this release our entire team did a great job, so we hope you will enjoy it!

## UPGRADE

To upgrade from a previous version you will have to re-install StackGres operator. Hot upgrades will be supported in the upcoming `1.0` version. Remember to backup any `SGCluster` data and StackGres CRs. For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/03-production-installation/02-installation-via-helm/#upgrade-operator).

To re-install StackGres issue following commands (replace namespace and release name if you used something different):

```
NAMESPACE=stackgres
RELEASE=stackgres-operator
helm uninstall -n "$NAMESPACE" "$RELEASE"
kubectl delete crd \
  customresourcedefinition.apiextensions.k8s.io/sgbackupconfigs.stackgres.io \
  customresourcedefinition.apiextensions.k8s.io/sgbackups.stackgres.io \
  customresourcedefinition.apiextensions.k8s.io/sgclusters.stackgres.io \
  customresourcedefinition.apiextensions.k8s.io/sgdistributedlogs.stackgres.io \
  customresourcedefinition.apiextensions.k8s.io/sginstanceprofiles.stackgres.io \
  customresourcedefinition.apiextensions.k8s.io/sgpgconfigs.stackgres.io \
  customresourcedefinition.apiextensions.k8s.io/sgpoolconfigs.stackgres.io
helm install -n "$NAMESPACE" "$RELEASE" https://stackgres.io/downloads/stackgres-k8s/stackgres/0.9/helm-operator.tgz
```

## CHANGES

* Logs can now be collected and analyzed in the UI or REST API
* Patroni 1.6.5
* WAL-G 0.2.15
* Clusters uses Postgres version 11.8 and 12.3
* Clusters uses Envoy version 1.15.0 with new [postgres Envoy network filter](https://www.envoyproxy.io/docs/envoy/v1.15.0/configuration/listeners/network_filters/postgres_proxy_filter)
* StackGres pods now run with non-root user (or arbitrary user in OpenShift)
* OpenShift 3.11 support added. Also Minishift is added to StackGres integrated test suite.
* Custom annotations and labels can now be specified for StackGres cluster pods.
* Reorganization of internal pod ports so that 5432 now points to postgres instance.
* Added scripts section for cluster initialization to load SQL snippets or small SQL files from ConfigMaps or Secrets
* New UI design for the web console
* UI and REST API now run in its own pod
* UI authentication / authorization based on JWT token and backed by kubernetes RBAC
* UI's URLs reload to the same page so they can be used for collaboration (share links to parts of the web console)
* UI include now interface to create/edit/delete distributed logs CRDs
* Match functionality between CRDs and UI (they are fully equivalent; use either, see the results on either too)
* Stats are now shown per Pod and have been improved
* Added distributed logs create/edit/view to the UI
* Custom Grafana dashboard is used by default when enabling Grafana integration
* Timeline is now exposed in backup status
* Allow GCP workload identity configuration for Postgres Backups
* Update default values for `nonProductionOptions` to be production oriented by default

## FIXES

* When deleting a SGBackup CR, the backup in the object storage is not deleted
* Automatic backups are removed when the cluster is removed
* Manual backups are apparently not working
* Connection does not work when SG cluster pod is "Ready"
* Creation of cluster for pg 11 with default configuration fail
* Fixed retention not honored
* Internal error when deleting a distributed logs CR
* Cluster resources are not deleted when deleting a cluster from the REST API
* Distributed logs REST API return an error relation does not exists
* Control Data field is not being stored in the backup CR
* Fields are not written in some versions of kubernetes generating cluster update events
* Automatic backup job does not enforce a non-root security context
* Empty deleted cluster name after deletion
* Automatic refreshment is not working
* Operator does not update correctly null values
* Using fixed version 8.13.4 for prometheus-operator dependency
* Using fixed version 5.0.26 for MinIO dependency

# KNOWN ISSUES

* Kubernetes 1.18 is not supported yet, see #439
* Kubernetes 1.11 requires PodShareProcessNamespace feature gate to be enabled (not tested in any kubernetes environment except for MiniShift 3.11)

# Release 0.9-RC3

## FIXES

* Empty deleted cluster name after deletion
* Automatic refreshment is not working
* Operator does not update correctly null values

# Release 0.9-RC2

## NOTES

* Support for setting node selector for cluster pods

## FIXES

* UI user sent to not found page when the token is expired
* Unable to update cluster helm charts

# Release 0.9-RC1

## NOTES

* Clusters uses Postgres version 11.8 and 12.3
* Clusters uses Envoy version 1.15.0 with new [postgres Envoy network filter](https://www.envoyproxy.io/docs/envoy/v1.15.0/configuration/listeners/network_filters/postgres_proxy_filter)
* Reorganization of internal pod ports so that 5432 now points to postgres instance.
* Added scripts section for cluster initialization to load SQL snippets or small SQL files from ConfigMaps or Secrets
* UI authentication / authorization based on JWT token and backed by kubernetes RBAC
* UI's URLs reload to the same page so they can be used for collaboration
* UI include now interface to create/edit/delete Logs CRDs
* Match functionality between CRDs and UI
* Stats are now shown per Pod and have been improved
* Added distributed logs create/edit/view to the UI
* Custom Grafana dashboard is used by default when enabling Grafana integration
* Timeline is now exposed in backup status
* Allow GCP workload identity configuration for Postgres Backups
* Update default values for nonProductionOptions to be production oriented by default

## FIXES

* Fixed retention not honored
* Internal error when deleting a distributed logs CR
* Cluster resources are not deleted when deleting a cluster from the REST API
* Distributed logs REST API return an error relation does not exists
* Control Data field is not being stored in the backup CR
* Fields are not written in some versions of kubernetes generating cluster update events
* Automatic backup job does not enforce a non-root security context

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

