# :rocket: Release 1.2.1 (2022-06-23)

## :notepad_spiral: NOTES

Here comes StackGres 1.2.1 bringing some important hotfixes like Postgres 14 [index corruption bugfix](https://www.postgresql.org/about/news/postgresql-144-released-2470/) or [the backups retention causing deletion of WAL files](https://gitlab.com/ongresinc/stackgres/-/issues/1840).

Please upgrade StackGres and your clusters (with a `securityUpgrade` `SGDbOps`) as soon as possible.

## :sparkles: NEW FEATURES AND CHANGES

* Update Postgres images to to 12.11, 13.7 and 14.4 
* Avoid delete Jobs automatically when SGBackup fails or complete and allow to delete Jobs without recreating them if the SGDbOps or SGBackup has failed or completed

## :bug: FIXES

* Forbid usage of PostgreSQL 14.0, 14.1, 14.2 and 14.3 due to [index corruption bugfix](https://www.postgresql.org/about/news/postgresql-144-released-2470/) on 14.4. A warning event will be created if a cluster is using one of the forbidden PostgreSQL version 14.0, 14.1, 14.2 or 14.3.
* Backup retention removes all the WAL files

### Web Console

* A warning is shown if a cluster is using one of the forbidden PostgreSQL version 14.0, 14.1, 14.2 or 14.3.

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368))
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.0 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.2.1)

# :rocket: Release 1.2.0 (2022-05-17)

## :notepad_spiral: NOTES

Here comes StackGres 1.2.0 bringing more fishy stuffs and let you be the first by not letting you lose even a bit of info!! :first_place: :fishing_pole_and_fish: :tada: :bottle_with_popping_cork:

The day has come, never more lose a single commit by enabling synchronous replication and add dedicated instances to be readonly. Also try out the Babelfish Compass application from StackGres Web UI and check if your SQLServer schema is compatible with Babelfish for Postgres!

## :sparkles: NEW FEATURES AND CHANGES

* Dropped support for Kubernetes 1.16 and 1.17
* Support Postgres 14.2, 13.6 and 12.10
* Support for synchronous replication
* Support for replication groups
* New Babelfish Compass application integrated in the Web UI
* Removed support for StackGres 0.9
* When SGCluster requires upgrade PendingUpgrade will be set
* Backups are now restored by name to avoid security issues
* Backups can now be copied across namespaces requiring the `.spec.sgCluster` field to be prepended the cluster name with the cluster namespace plus the `.` character so that it still points to the original cluster.
* Backup paths are stored in SGCluster configuration and SGBackup status for better visibility and to allow to be changed
* Set `restore_command` when backup is enabled to allow replica to catch up from backup storage
* Deep validation of postgresql parameters
* Support for huge pages
* Added backupPath to SGCluster, SGBackup and SGDbOps (for op majorVersionUpgrade)
* SGDbOps restart operation restart the primary instance first only if it is pending restart and wait if already restarting

### Web Console

* Enhance the date picker to filter cluster logs
* Suggest default names for manual Backups and DbOps
* Improve mandatory fields notification

## :bug: FIXES

* SGDbOps pgbench benchmark is not setting correctly TPS
* When SGCluster requires upgrade PendingRestart should not be set
* Restart only if pending restart and wait if already restarting
* Removed idle timeout for Patroni REST API that were causing restart and other SGDbOps to fail
* Removed creation of default StackGres configurations in operator namespace
* Using new CSR v1 when creating certificates only for k8s 1.22+
* Labels are too generic and should be changed to avoid collisions
* Blocklisted parameters with a default value where not included in generated configurations for `SGPostgresConfig` and `SGPoolingConfig`
* Fixed helm validation to print the generated password
* Potential security issue in OpenJDK image
* Escape special characters in field returned on failed validation
* Backup breaks after major version upgrade
* Typo on SGBackupConfig and SGBackup bandwidth properties
* Set disableMetricsExporter to true does not remove the postgres exporter sidecar
* Fields property empty on a REST API response for babelfish flavor
* Only allow patching Job annotations and Pod annotations
* Set backup information timestamp as a String
* SGDbOps may get stuck running in some cases
* SGCluster does not validate restore section
* Restart based operation are failing and Patroni log error "BrokenPipeError: [Errno 32] Broken pipe"
* kubectl throttling in backup pod

### Web Console

* Support SGDistributedLogs retention spec on SGCluster form
* Namespace link on web console's breadcrumbs point to wrong path on SGInstanceProfile listings
* The disableClusterPodAntiAffinity config is not shown on SGDistributedLogs details
* Main dashboard does not validate user permissions
* Sidebar shows top level CRDs when user has no permissions
* Error message do not include details coming from the REST API
* Fix "go to default dashboard" link on not-found page
* Repeated names when creating restart SGDbOps
* Prevent input of invalid runAt values for SGDbOps
* Empty notification when toggling twice between timezones
* Selected extensions are not disabled when changing flavor
* Fixed dbops and distributedlogs doc links name
* Adjust computed property match on header section
* Prevent auto scrolling on log records when log details are visible
* SGBackupConfig summary is empty when on edit mode
* Notification won't load when resource has been deleted
* Unify Backup Config icons
* Initialization scripts from configmaps or secrets are not set when creating a cluster
* Improve layout of logs records
* Wrong mapping for S3Compatible storageClass info
* Repack Databases tables is missing styling
* Tooltips which are too long won't fit the screen
* CRD titles appear floating on collapsed Sidebar
* Missing 404/Not Found validations on monitoring tab
* Missing tooltip for initialization scripts source type
* Delete resource popup remains open when clicking anywhere else
* Notifications should allow HTML tags

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368))
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.0 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.2.0)

# :rocket: Release 1.2.0-RC1 (2022-05-10)

## :notepad_spiral: NOTES

Here comes StackGres 1.2.0-RC1 the brings more fishy stuffs and let you be the first by not letting you lose even a bit of info!! :first_place: :fishing_pole_and_fish: :tada: :bottle_with_popping_cork:

The day has come, never more lose a single commit by enabling synchronous replication and add dedicated instances to be readonly. Also try out the Babelfish Compass application from StackGres Web UI and check if your SQLServer schema is compatible with Babelfish for Postgres!

## :sparkles: NEW FEATURES AND CHANGES

* Dropped support for Kubernetes 1.16 and 1.17
* Support Postgres 14.2, 13.6 and 12.10
* Support for synchronous replication
* Support for replication groups
* New Babelfish Compass application integrated in the Web UI
* Removed support for StackGres 0.9
* When SGCluster requires upgrade PendingUpgrade will be set
* Backups are now restored by name to avoid security issues
* Backups can now be copied across namespaces requiring the `.spec.sgCluster` field to be prepended the cluster name with the cluster namespace plus the `.` character so that it still points to the original cluster.
* Backup paths are stored in SGCluster configuration and SGBackup status for better visibility and to allow to be changed
* Set `restore_command` when backup is enabled to allow replica to catch up from backup storage
* Deep validation of postgresql parameters
* Support for huge pages
* Added backupPath to SGCluster, SGBackup and SGDbOps (for op majorVersionUpgrade)
* SGDbOps restart operation restart the primary instance first only if it is pending restart and wait if already restarting

## Web Console

* Enhance the date picker to filter cluster logs
* Suggest default names for manual Backups and DbOps
* Improve mandatory fields notification

## :bug: FIXES

* SGDbOps pgbench benchmark is not setting correctly TPS
* When SGCluster requires upgrade PendingRestart should not be set
* Restart only if pending restart and wait if already restarting
* Removed idle timeout for Patroni REST API that were causing restart and other SGDbOps to fail
* Removed creation of default StackGres configurations in operator namespace
* Using new CSR v1 when creating certificates only for k8s 1.22+
* Labels are too generic and should be changed to avoid collisions
* Blocklisted parameters with a default value where not included in generated configurations for `SGPostgresConfig` and `SGPoolingConfig`
* Fixed helm validation to print the generated password
* Potential security issue in OpenJDK image
* Escape special characters in field returned on failed validation
* Backup breaks after major version upgrade
* Typo on SGBackupConfig and SGBackup bandwidth properties
* Set disableMetricsExporter to true does not remove the postgres exporter sidecar
* Fields property empty on a REST API response for babelfish flavor
* Only allow patching Job annotations and Pod annotations
* Set backup information timestamp as a String
* SGDbOps may get stuck running in some cases
* SGCluster does not validate restore section
* Restart based operation are failing and Patroni log error "BrokenPipeError: [Errno 32] Broken pipe"
* kubectl throttling in backup pod
* Labels are too generic and should be changed to avoid collisions
* Panel duplicated in grafana settings dashboard

## Web Console

* Support SGDistributedLogs retention spec on SGCluster form
* Namespace link on web console's breadcrumbs point to wrong path on SGInstanceProfile listings
* The disableClusterPodAntiAffinity config is not shown on SGDistributedLogs details
* Main dashboard does not validate user permissions
* Sidebar shows top level CRDs when user has no permissions
* Error message do not include details coming from the REST API
* Fix "go to default dashboard" link on not-found page
* Repeated names when creating restart SGDbOps
* Prevent input of invalid runAt values for SGDbOps
* Empty notification when toggling twice between timezones
* Selected extensions are not disabled when changing flavor
* Fixed dbops and distributedlogs doc links name
* Adjust computed property match on header section
* Prevent auto scrolling on log records when log details are visible
* SGBackupConfig summary is empty when on edit mode
* Notification won't load when resource has been deleted
* Unify Backup Config icons
* Initialization scripts from configmaps or secrets are not set when creating a cluster
* Improve layout of logs records
* Wrong mapping for S3Compatible storageClass info
* Repack Databases tables is missing styling
* Tooltips which are too long won't fit the screen
* CRD titles appear floating on collapsed Sidebar
* Missing 404/Not Found validations on monitoring tab
* Missing tooltip for initialization scripts source type
* Delete resource popup remains open when clicking anywhere else
* Notifications should allow HTML tags

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368))
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.0 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.2.0-RC1)

# :rocket: Release 1.2.0-beta1 (2022-03-23)

## :notepad_spiral: NOTES

Here comes StackGres 1.2.0-beta1 the brings more fishy stuffs and let you be the first by not letting you lose even a bit of info!! :first_place: :fishing_pole_and_fish: :tada: :bottle_with_popping_cork:

The day has come, never more lose a single commit by enabling synchronous replication and add dedicated instances to be readonly. Also try out the Babelfish Compass application from StackGres Web UI and check if your SQLServer schema is compatible with Babelfish for Postgres!

## :sparkles: NEW FEATURES AND CHANGES

* Support for synchronous replication
* Support for replication groups
* New Babelfish Compass application integrated in the Web UI
* Removed support for StackGres 0.9
* When SGCluster requires upgrade PendingUpgrade will be set
* Backups are now restored by name to avoid security issues
* Backups can now be copied across namespaces requiring the `.spec.sgCluster` field to be prepended the cluster name with the cluster namespace plus the `.` character so that it still points to the original cluster.
* Set `restore_command` when backup is enabled to allow replica to catch up from backup storage
* Deep validation of postgresql parameters

## Web Console

* Enhance the date picker to filter cluster logs
* Suggest default names for manual Backups and DbOps

## :bug: FIXES

* SGDbOps pgbench benchmark is not setting correctly TPS
* When SGCluster requires upgrade PendingRestart should not be set

## Web Console

* Support SGDistributedLogs retention spec on SGCluster form
* Namespace link on web console's breadcrumbs point to wrong path on SGInstanceProfile listings
* The disableClusterPodAntiAffinity config is not shown on SGDistributedLogs details
* Main dashboard does not validate user permissions
* Sidebar shows top level CRDs when user has no permissions
* Error message do not include details coming from the REST API
* Fix "go to default dashboard" link on not-found page
* Repeated names when creating restart SGDbOps
* Prevent input of invalid runAt values for SGDbOps
* Empty notification when toggling twice between timezones
* Selected extensions are not disabled when changing flavor

## :construction: KNOWN ISSUES

* Installation fails in EKS 1.21+ due to CSR not returning the certificate ([#1358](https://gitlab.com/ongresinc/stackgres/-/issues/1358)) 
* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* After major version upgrade continuous archiving and backups may hang ([#1383](https://gitlab.com/ongresinc/stackgres/-/issues/1383)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.0 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.2.0-beta1)

# :rocket: Release 1.1.0 (2022-02-01)

## :notepad_spiral: NOTES

Here comes StackGres 1.1.0 the awaited gift of this Christmas with a new tasty and fishy flavor ARMed to the teeth!! :christmas_tree: :fishing_pole_and_fish: :bomb: :tada: :bottle_with_popping_cork:

We have listened to you. Did you want ARM? Here it comes! Finally, you can run Postgres on Kubernetes on ARM64 nodes. This release brings support ARM64 architecture in order to run StackGres even on a Raspberry Pi (and of course on cool ARM hardware, like AWS Graviton 2/3). And the new experimental Postgres for Babelfish flavor allows you to connect to Postgres using the SQLServer protocol! (which is also available on ARM).

## :sparkles: NEW FEATURES AND CHANGES

* Add support for ARM64 architecture
* New Postgres versions 14.1, 13.5 and 12.9
* Support for [Postgres for Babelfish](https://babelfishpg.org/) version 13.4, both for AMD64 and ARM64
* Completely redesigned cluster creation form: it is now implemented wizard-style, with separate steps for each major relevant function
* Patroni 2.1.2
* PgBouncer 1.16.1
* Prometheus Postgres Exporter 0.10.1
* Check for Postgres timeline when failing or switching over a replica to avoid data loss as much as possible.
* Support to specify external IPs for Postgres services
* Support to configure resources in operator helm chart
* Support Kubernetes 1.23
* Suggest default names for manual backups and dbops in the Web UI

## :bug: FIXES

* Configuration does not include shared_preloaded_libraries when performing pg_upgrade
* Support allowing restart when primary pod is unavailable
* When deleting primary Pod cluster may become unresponsive
* Restart not restarting the cluster
* Internal DNS for services not using configured search names
* If backup used in initialData is removed the reconciliation cycle crashes
* Infinite running state when restarting Pod after invalid configuration
* Some resources are continuously patched
* Major version upgrade cannot be performed now if not all installed extensions are not available in the new postgres major version
* Init reconciliation cycle should fail for any error
* Support migration from Client Side Apply to Server Side Apply
* Error in endpoints stats from REST API

## :construction: KNOWN ISSUES

* Installation fails in EKS 1.21+ due to CSR not returning the certificate ([#1358](https://gitlab.com/ongresinc/stackgres/-/issues/1358)) 
* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* After major version upgrade continuous archiving and backups may hang ([#1383](https://gitlab.com/ongresinc/stackgres/-/issues/1383)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))
* After upgrading StackGres operator newer Postgres versions that can not be used for a not upgraded SGCluster may be erroneously presented in the Web UI ([#1540](https://gitlab.com/ongresinc/stackgres/-/issues/1540))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.1.0)

# :rocket: Release 1.1.0-RC2 (2022-01-26)

## :notepad_spiral: NOTES

Here comes StackGres 1.1.0-RC2 the awaited gift of this Christmas with a new tasty and fishy flavor ARMed to the teeth!! :christmas_tree: :fishing_pole_and_fish: :bomb: :tada: :bottle_with_popping_cork:

We have listened to you. Did you want ARM? Here it comes! Finally, you can run Postgres on Kubernetes on ARM64 nodes. This release brings support ARM64 architecture in order to run StackGres even on a Raspberry Pi (and of course on cool ARM hardware, like AWS Graviton 2/3). And the new experimental Postgres for Babelfish flavor allows you to connect to Postgres using the SQLServer protocol! (which is also available on ARM).

## :sparkles: NEW FEATURES AND CHANGES

* Add support for ARM64 architecture
* New Postgres versions 14.1, 13.5 and 12.9
* Support for [Postgres for Babelfish](https://babelfishpg.org/) version 13.4, both for AMD64 and ARM64
* Completely redesigned cluster creation form: it is now implemented wizard-style, with separate steps for each major relevant function
* Patroni 2.1.2
* PgBouncer 1.16.1
* Check for Postgres timeline when failing or switching over a replica to avoid data loss as much as possible.
* Support to specify external IPs for Postgres services
* Support to configure resources in operator helm chart
* Support Kubernetes 1.23

## :bug: FIXES

* Configuration does not include shared_preloaded_libraries when performing pg_upgrade
* Support allowing restart when primary pod is unavailable
* When deleting primary Pod cluster may become unresponsive
* Restart not restarting the cluster
* Internal DNS for services not using configured search names
* If backup used in initialData is removed the reconciliation cycle crashes
* Infinite running state when restarting Pod after invalid configuration
* Some resources are continuously patched
* Major version upgrade cannot be performed now if not all installed extensions are not available in the new postgres major version
* Init reconciliation cycle should fail for any error
* Support migration from Client Side Apply to Server Side Apply
* Error in endpoints stats from REST API

## :construction: KNOWN ISSUES

* Installation fails in EKS 1.21+ due to CSR not returning the certificate ([#1358](https://gitlab.com/ongresinc/stackgres/-/issues/1358)) 
* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* After major version upgrade continuous archiving and backups may hang ([#1383](https://gitlab.com/ongresinc/stackgres/-/issues/1383)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))
* After upgrading StackGres operator newer Postgres versions that can not be used for SGCluster not upgraded may be presented in the Web UI ([#1540](https://gitlab.com/ongresinc/stackgres/-/issues/1540))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.1.0-RC2)

# :rocket: Release 1.1.0-RC1 (2022-01-14)

## :notepad_spiral: NOTES

Here comes StackGres 1.1.0-RC1 the awaited gift of this Christmas with a new tasty and fishy flavor ARMed to the teeth!! :christmas_tree: :fishing_pole_and_fish: :bomb: :tada: :bottle_with_popping_cork:

We have listened to you. Did you want ARM? Here it comes! Finally, you can run Postgres on Kubernetes on ARM64 nodes. This release brings support ARM64 architecture in order to run StackGres even on a Raspberry Pi (and of course on cool ARM hardware, like AWS Graviton 2/3). And the new experimental Postgres for Babelfish flavor allows you to connect to Postgres using the SQLServer protocol! (which is also available on ARM).

## :sparkles: NEW FEATURES AND CHANGES

* Add support for ARM64 architecture
* New Postgres versions 14.1, 13.5 and 12.9
* Support for [Postgres for Babelfish](https://babelfishpg.org/) version 13.4, both for AMD64 and ARM64
* Completely redesigned cluster creation form: it is now implemented wizard-style, with separate steps for each major relevant function
* Patroni 2.1.2
* PgBouncer 1.16.1
* Check for Postgres timeline when failing or switching over a replica to avoid data loss as much as possible.
* Support to specify external IPs for Postgres services
* Support to configure resources in operator helm chart
* Enable Patroni `check_timeline` to avoid data corruption on failover and switchover

## :bug: FIXES

* Support allowing restart when primary pod is unavailable
* When deleting primary Pod cluster may become unresponsive
* Restart not restarting the cluster
* Internal DNS for services not using configured search names
* If backup used in initialData is removed the reconciliation cycle crashes
* Infinite running state when restarting Pod after invalid configuration
* Some resources are continuously patched
* Major version upgrade cannot be performed now if not all installed extensions are not available in the new postgres major version
* Init reconciliation cycle should fail for any error
* Support migration from Client Side Apply to Server Side Apply
* Error in endpoints stats from REST API

## :construction: KNOWN ISSUES

* Installation fails in EKS 1.21+ due to CSR not returning the certificate ([#1358](https://gitlab.com/ongresinc/stackgres/-/issues/1358)) 
* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* After major version upgrade continuous archiving and backups may hang ([#1383](https://gitlab.com/ongresinc/stackgres/-/issues/1383)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))
* After upgrading StackGres operator newer Postgres versions that can not be used for SGCluster not upgraded may be presented in the Web UI ([#1540](https://gitlab.com/ongresinc/stackgres/-/issues/1540))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.1.0-RC1)

# :rocket: Release 1.1.0-beta1 (2021-11-05)

## :notepad_spiral: NOTES

Here comes StackGres 1.1.0-beta1 GA with a new tasty and fishy flavor!! :fishing_pole_and_fish: :tada: :bottle_with_popping_cork:

This release brings the new experimental Postgres for Babelfish flavor, that allows to connect to Postgres using SQLServer protocol!

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

## :sparkles: CHANGES

* Support for [Postgres for Babelfish](https://babelfishpg.org/) version 13.4

## :bug: FIXES

* If backup used in initialData is removed the reconciliation cycle crashes

## :construction: KNOWN ISSUES

* Installation fails in EKS 1.21+ due to CSR not returning the certificate ([#1358](https://gitlab.com/ongresinc/stackgres/-/issues/1358)) 
* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* After major version upgrade continuous archiving and backups may hang ([#1383](https://gitlab.com/ongresinc/stackgres/-/issues/1383)) 
* When deleting primary pod cluster may become unresponsive ([#783](https://gitlab.com/ongresinc/stackgres/-/issues/783))

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.1.0-beta1)

# :rocket: Release 1.0.0 (2021-10-13)

## :notepad_spiral: NOTES

We are proud to announce firt StackGres 1.0.0 GA!! :medal: :tada: :bottle_with_popping_cork:

This release brings a lot of new features and bugfixes. The most significant one is the availability of 120+ extensions, and many more to come. This makes StackGres the Postgres platform with more extensions! Load and unload them dynamically, at will, onto your pods, via simple YAML or the Web Console.

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

## :sparkles: CHANGES

* Support for Kubernetes 1.20 and 1.21
* Support for Postgres 14 and Postgres 13. Following our open source policy of maintaining 2 major releases, Postgres 11 is dropped.
* All latest Postgres minor versions up to 14.0, 13.4 and 12.8
* Patroni 2.1.1
* Envoy 1.19.1
* Postgres Exporter 0.10.0
* Fluentd 1.13.2
* Fluent-bit 1.8.1
* Support to install Postgres extensions on demand
* Support for day-2 operations including minor, major, and security (for new operator versions) upgrade
* Support for recovery with PITR
* Support for configuring pgbouncer databases and users sections
* Enabled use of [Server Side Apply](https://kubernetes.io/docs/reference/using-api/server-side-apply/) in the operator
* Added extra column that is shown for resources in-group `stackgres.io` using `kubectl`
* CRDs promoted to apiVersion: stackgres.io/v1
* A completely new experience with the new Web UI and REST API

## :bug: FIXES

* Grafana integration is failing
* Prometheus integration does not work if the service monitor does not have matchLabels
* Reuse the Kubernetes Client in order to minimize resource usage on the operator, jobs, and controllers
* Improve REST API response when resource already exists
* Empty fields returned by REST API when validation fails
* REST API must not return any secret
* Exclusive lock was not correctly failing when lost for backup jobs
* When retrieving all context fail whole reconciliation cycle breaks
* REST API is not able to update `shared_preload_libraries` configuration parameters.
* Default `log_line_prefix` include quotes in the value
* Check and sanitize database names in initial data scripts
* Restrict CRD names to avoid failure in the reconciliation cycle
* Disabled envoy SQL parsing due to performance issues

## :construction: KNOWN ISSUES

* Installation fail in EKS 1.21+ due to CSR not returning the certificate ([#1358](https://gitlab.com/ongresinc/stackgres/-/issues/1358)) 
* Major version upgrade fail if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* When deleting primary pod cluster may become unresponsive ([#783](https://gitlab.com/ongresinc/stackgres/-/issues/783))

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.0.0)

# :rocket: Release 1.0.0-RC1 (2021-10-08)

## :notepad_spiral: NOTES

We are proud to announce firt StackGres 1.0.0-RC1 GA!! :medal: :tada: :bottle_with_popping_cork:

This release brings a lot of new features and bugfixes. The most significant one is the availability of 120+ extensions, and many more to come. This makes StackGres the Postgres platform with more extensions! Load and unload them dynamically, at will, onto your pods, via simple YAML or the Web Console.

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

## :sparkles: CHANGES

* Support Kubernetes 1.22
* Support for Postgres 14.
* Patroni 2.1.1
* Envoy 1.19.1
* Postgres Exporter 0.10.0
* Extensions cache (disabled by default)
* Support to use a proxy to access extensions repository
* Add events to pgbench sgdbops
* Support install extension using required shared library if already present and identical
* Re-implement SGDbOps for minor and major version upgrade to change Postgres version in the Job
* Allow to change image pull policy for all controllers and jobs images

## :bug: FIXES

* Generic labels were not correctly set in some cases generating loots of secrets when running sgdbops
* Validate that pg_repack extension is added before running the SGDbOps repack
* Relax default extension candidate finding logic
* Allow all databases connections even when databases section is configured in pgbouncer
* Update status with lock resources for sgcluster
* PgBouncer fail authentication for users defined in users section
* PgBouncer does not allow to connect remotely to the console at pgbouncer database
* sgpgconfig conversion webhooks fail to convert default parameters
* Wrong wal_keep_size used in Postgres 13 configurations
* Added pattern and maxLength validation on metada.name property for sgcluster, distributedlogs, sgdbops and sgbackup CRD.
* Native and jvm image APP_OPTS and JAVA_OPTS defaults are overwritten
* NullPointerException returned by cluster and dbops events endpoint in REST API
* Cleanup admission webhook validation message returned in REST API

## :construction: KNOWN ISSUES

* Installation fail in EKS 1.21+ due to CSR not returning the certificate ([#1358](https://gitlab.com/ongresinc/stackgres/-/issues/1358)) 

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.0.0-RC1)

# :rocket: Release 1.0.0-beta3 (2021-09-03)

## :notepad_spiral: NOTES

Here it comes StackGres 1.0.0-beta3!! :tada: :bottle_with_popping_cork:

This release brings new features and some bugfixes. Get safe and upgrade now!

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require to uninstall completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

## :sparkles: CHANGES

* Support for Kubernetes 1.20 and 1.21
* Support for Postgres 13 and drop of Postgres 11
* All latest Postgres minor versions up to 13.4 and 12.8
* Patroni 2.1.0
* Fluentd 1.13.2
* Fluent-bit 1.8.1
* Support for configuring cluster service in distirbuted logs
* Support for configuring cluster's pods node affinity
* Enabled use of [Server Side Apply](https://kubernetes.io/docs/reference/using-api/server-side-apply/) in the operator
* Include field `.spec.matadata.annotations.clusterPods`
* Move `.spec.postgres{Version,Extensions}` under `.spec.postgres` section
* Allow more customization for pgbouncer
* Installing extensions for newly created clusters create the extra mounts without requiring restart
* Implement `onlyPendingRestart` option for `restart` SGDbOps
* Changed SGPostgresConfig and SGPoolConfig to write default values the `.status` section rather than `.spec`
* Improve events message for StackGres clusters
* Add a validation to check any update is performed if SGCluster is locked
* Support management of SGDistributedLogs services configuration
* Generate events in SGCluster relative to Postgres extension operations
* Generate DbOps events for operations
* Check upgrade is not performed on alpha or beta releases
* Add CRD events tab on Web UI
* Implement full condensed view on Web UI
* Added a dashboard for minor version upgrades in the Web UI
* Minor version upgrade dashboard for the Web UI
* Include "Restart Now" button on clusters with a "PendingRestart" status in the Web UI
* Allow time interval change in grafana integration in the Web UI
* Enable initialization scripts using config maps and secrets in the Web UI
* Improve presentation of the results of a `benchmark` SGDbOps in the Web UI
* Improve Pods status info when the status is pending in the Web UI
* Changed REST API paths
* Support to retrieve events related to a SGCluster in the REST API
* Support to retrieve events related to a SGDbOps in the REST API

## :bug: FIXES

* Ignoring metadata managed fields during the reconciliation cycle to avoid unwanted resource patches
* Role value is not updated in distributed logs
* Component versions annotations doesn't reflect accurately what is installed in a SGCluster
* Wrong opRetries on DBOps
* Component versions annotations doesn't reflect accurately what is installed in a SGCluster
* Avoid unnecessary reconciliation cycle repetition
* All existing SGDBOps are re-execute if SGCluster is recreated
* Pods scheduling information is empty
* Fluentd throw undefined method  for nil:NilClass
* SGDbOps does not set correctly the `.status.<op>.switchoverInitiated` field
* Compression of SGBackup is being lost after operator upgrade
* Extensions mutating webhook does not behave correctly with missing extensions
* Subresource status is not added after operator upgrade
* Grafana integration is failing
* Restore annotations to pause reconciliation cycle for specific resources
* SGCluster or SGDistributedLogs the Pods are not created when tolerationSeconds is set
* Restart SGDbOps fail due to conflict on cluster update
* Prometheus integration does not work if the service monitor does not have matchLabels
* Reuse the Kubernetes Client in order to minimize resource usage on the operator, jobs and controllers
* Move tasks performed by upgrade-job.yaml to conversion webhooks
* Improve readability in tables for Web UI
* Postgres versions dropdown selector won't list versions properly in the Web UI
* Cloning a cluster to a different namespace wont clone dependencies in the Web UI
* Improve performance of logs listings on the Web UI
* Operator fails to edit a cluster created from a backup on the Web UI
* Other minor Web UI fixes and improvements
* Improve REST API response when resource already exists
* Empty fields returned by REST API when validation fails
* REST API must not return any secret

## :construction: KNOWN ISSUES

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.0.0-beta3)

# :rocket: Release 1.0.0-beta2 (2021-07-09)

## :notepad_spiral: NOTES

Here it comes StackGres 1.0.0-beta2!! :tada: :bottle_with_popping_cork:

This release brings some bugfixes. Get safe and upgrade now!

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` version. Upgrading from those versions will require to uninstall completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

## :sparkles: CHANGES

* Check upgrade is not performed on alpha or beta releases
* Review Web UI URL paths
* Improve Pods status info when the status is pending in the Web UI

## :bug: FIXES

* Ignoring metadata managed fields during the reconciliation cycle to avoid unnecesary patches of existent resources
* Subresource status is not added after operator upgrade
* Grafana integration is failing
* `stackgres.io/reconciliation-pause` and `stackgres.io/reconciliation-pause-until-restart` do not work on generated resources
* `sgcluster` or `sgdistributedlogs` the Pods are not created when tolerationSeconds is set
* Restart DbOps fail due to conflict on cluster update
* Prometheus integration does not work if the service monitor does not have `matchLabels`
* Operator fails to edit a cluster created from a backup in the Web UI
* Improve performance of logs listings on the Web UI
* Warning when cloning a cluster to a different namespace with missing dependencies in the Web UI

## :construction: KNOWN ISSUES

* Kubernetes 1.20+ is not supported yet, see #950 

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.0.0-beta2)

# :rocket: Release 1.0.0-beta1 (2021-06-29)

## :notepad_spiral: NOTES

Here it comes StackGres 1.0.0-beta1!! :tada: :bottle_with_popping_cork:

This release brings some bugfixes and nice improvements. Get powerful safe and upgrade now!

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `1.0.0-alpha1` version. Upgrading from that version will require to uninstall completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

## :sparkles: CHANGES

* SGDbOps to perform Cluster Upgrade
* Upgrade to Patroni 2
* `.status.dbOps` is not updating during operation in SGCluster custom resources
* Include cluster services names on the Web UI
* Show in the Web UI connection information to a given SG cluster
* Include timezone indicator on the Web UI
* Make Web UI table columns resizable
* Return component versions running in cluster's Pod in /sgcluster REST API endpoint
* Extend sgcluster logs REST API in order to allow specify multiple values

## :bug: FIXES

* Role value is not returned by the REST API
* Add default `.spec.postgresServices` section in SGCluster
* Disabled envoy SQL parsing due to performance issues
* Operator throws error when cluster name is not valid
* Cluster in restart pending mode when it shouldn't in the Web UI
* Web UI lists cluster already deleted from YAML
* Validate storageClass for s3 SGBackupConfig
* Web UI styling adjustments

## :construction: KNOWN ISSUES

* Kubernetes 1.20+ is not supported yet, see #950 

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.0.0-beta1)

# :rocket: Release 1.0.0-alpha4 (2021-06-08)

## :notepad_spiral: NOTES

Here it comes StackGres 1.0.0-alpha4!! :tada: :bottle_with_popping_cork:

This release brings some bugfixes and small improvements. Get safe and upgrade now!

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `1.0.0-alpha1` version. Upgrading from that version will require to uninstall completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

## :sparkles: CHANGES

* Introduce a small padding between StackGres logo and version in the Web UI
* Show expanded advanced options when edit cluster if any advanced option was already used in the Web UI

## :bug: FIXES

* Can not find candidate version of extension on the Web UI
* Grafana dashboard is not loading on the Web UI
* Bug on columns ordering on the Web UI

## :construction: KNOWN ISSUES

* Kubernetes 1.20+ is not supported yet, see #950 

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.0.0-alpha4)

# :rocket: Release 1.0.0-alpha3 (2021-06-01)

## :notepad_spiral: NOTES

Here it comes StackGres 1.0.0-alpha3!! :tada: :bottle_with_popping_cork:

This release brings a lot of bugfixes and improvements you can not miss. Get safe and upgrade now!

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `1.0.0-alpha1` version. Upgrading from that version will require to uninstall completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

## :sparkles: CHANGES

* pgbench benchmark SGDbOps operation now clean up the database after benchmark is completed (or failed).
* Use new kubectl image based on ubi8.

## :bug: FIXES

* Cannot load specific version of an extension.
* REST API is not able to update `shared_preload_libraries` configuration parameters.
* Default `log_line_prefix` include quotes in the value.
* Major version upgrade break cluster when version is not upgraded.
* Check and sanitize database names in initial data scripts.
* Custom Annotations are not updated in StatefulSet on change in SGCluster.
* Extensions with shared library make major version upgrade to fail.
* Extensions extra mount not correctly specified.
* Restrict cluster names to a 63 character limit.
* Improve Grafana tab on the Web UI when no pods info is available.
* Enable PITR feature on the Web UI.
* SGDbOps `.runAt` operates in UTC, but Web UI on browser's timezone.
* Cluster creation in Web UI send an empty string as database name for initial data script entries.
* Validate Web UI documentation links corresponds with current docs structure.
* Configure vue-markdown to avoid adding html line breaks in Web UI.
* Cannot restore from a backup from the Web UI.
* Fixed data types and added panels in the grafana dashboard.

## :construction: KNOWN ISSUES

* Kubernetes 1.20+ is not supported yet, see #950

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.0.0-alpha3)

# Release 1.0.0-alpha2 (2021-05-06)

## NOTES

Here it comes StackGres 1.0.0-alpha2!! :tada: :bottle_with_popping_cork:

This is time to "extend" your experience with our new PostgreSQL extension system that bring the ability to install extensions on the fly. This release also brings a ton of improvements and bugfixes!

## UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/latest/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `1.0.0-alpha1` version. Upgrading from that version will require to uninstall completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

## CHANGES

* PostgreSQL Extensions System
* Support for recovery with PITR
* PostgreSQL 11.6, 11.7, 11.10, 11.11, 12.1, 12.2, 12.5 and 12.6 (with WAL-G 0.2.19)
* Envoy 1.17.1, Pgbouncer 1.15.0, Prometheus Postgres Exporter 0.9.0, Fluentd 1.12.1 and Fluent-bit 1.6.4
* SGDbOps are now in the Web UI
* Added extra column that are show for resources in group `stackgres.io` using `kubectl`
* Documentation style improved
* Alert when missing sgbackupconfig when creating a sgbackup
* Allow to expose Admin UI and REST API with HTTP
* Allow to specify separate certificate and RSA key pair for admin UI and REST API
* Add sgcluster status property to object returned by REST API
* Extend cluster status REST API with opened connections

## FIXES

* When retrieving all context fail whole reconciliation cycle breaks
* Confirm every updatable spec on every CRD is updatable from the Web UI
* SGDbOps runAt field is not honored
* Property "clone" not defined in the REST API for sgdbops major version upgrade
* DbOps CR are not validated
* Debug logging is enabled by default in StackGres components causing performance issues
* Endpoint /stackgres/sgcluster/stats/ doesn't return the correct pod list
* Backups not working in GKE in the Web UI
* Various fixes in the Web UI

## [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.0.0-alpha2)

# Release 0.9.5 (2021-03-31)

## NOTES

Here it comes StackGres 0.9.5!! :tada: :bottle_with_popping_cork:

We want you to be safe and a bit more powerful so we bring to you some bugfixes and small changes!

## UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/0.9/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

```
NAMESPACE=stackgres
RELEASE=stackgres-operator
helm upgrade -n "$NAMESPACE" "$RELEASE" https://stackgres.io/downloads/stackgres-k8s/stackgres/0.9.5/helm/stackgres-operator.tgz
```

## CHANGES

* Postgres 11.11 and 12.6
* PgBouncer 1.13.0
* Envoy 1.15.3
* Prometheus Postgres Exporter 0.8.0
* FluentBit 1.4.6
* Fluentd 1.9.3
* Support for annotations in sgdistributedlogs
* Support for node selector in sgdistributedlogs
* Support for node tolerations in sgdistributedlogs

## FIXES

* Primary service is not updated after upgrade to 0.9.4
* Service account annotation is not updated
* Job's pods not created with specified annotations
* Bug when editing CRDs from the web console and new information is read from the API
* Connections to the database timed out through the Envoy port
* Patroni endpoint is open to everyone
* Backup size information is inverted
* Upgrading a cluster with new annotations for all resources break the reconciliation
* NullPointerException on PairUpdater visitMapTransformed
* Operator version is not shown in the UI
* Wrong used disk size of primary shown in cluster summary in the UI
* Adjust logs loader function on-screen resize in the UI
* Bug on Instance Profiles edition in the UI
* Not found page loads only on the light mode in the UI
* Full schedule not shown on backup config details row in the UI
* Enable editing of annotations on clusters and distributed log servers in the UI
* Hide content when the requested CRD name is not found in the UI

## KNOWN ISSUES

* Kubernetes 1.18+ is not supported yet, see #439
* Kubernetes 1.11 requires PodShareProcessNamespace feature gate to be enabled (not tested in any kubernetes environment except for MiniShift 3.11)

## [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/0.9.5)

# Release 1.0.0-alpha1

## NOTES

We are proud to present StackGres 1.0.0-alpha1!! :fireworks: :bottle_with_popping_cork:

This is our first 1.0 series release and it comes with some very useful features to automate your StackGres daily tasks. This is an alpha version so new features !

## UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 0.9 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

```
NAMESPACE=stackgres
RELEASE=stackgres-operator
helm upgrade -n "$NAMESPACE" "$RELEASE" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.0.0-alpha1/helm/stackgres-operator.tgz
```

## CHANGES

* StackGres CRD have now a stable version
* Database operations:
  * Major Version Upgrade
  * Minor Version Upgrade
  * Security Upgrade
  * Restart
  * Vacuum
  * Repack
  * Pgbench benchmark
* Fresh new amazing UI interface

## FIXES

* Exclusive lock was not correctly failing when lost for backup jobs

# KNOWN ISSUES

* StackGres 1.0.0-alpha1 only supports Kubernetes 1.16+

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

