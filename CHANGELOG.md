# :rocket: Release 1.17.1 (2025-08-05)

## :notepad_spiral: NOTES

StackGres 1.17.1 is out! :confetti_ball: :champagne: 

This patch release fixes a blocking issue with the Web Console and a critical issue in SGStream for TOSTAble column with NOT NULL constraints.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

Nothing new here! :eyes:

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* SGStream migration fail when a TOASTable column has NOT NULL constraint

## Web Console

* SGCluster can not be edited
* SGCluster summary has errors

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.17.1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.17.1)

# :rocket: Release 1.17.0 (2025-07-18)

## :notepad_spiral: NOTES

StackGres 1.17.0 is out! :confetti_ball: :champagne: 

Finally, support for Kubernetes 1.33 and OpenShift 4.18 and 4.19 has been added.

Also, you will enjoy some important bugfixes and improvements all around the place.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support for Kubernetes 1.33
* Support for OpenShift 4.18 and 4.19
* Added Postgres 17.5, 16.9, 15.13, 14.18, 13.21
* Added Patroni 4.0.6
* Added Babelfish for PostgreSQL 16.6
* Update Quarkus to 3.22
* Updated base images and other components
* Disable Envoy by default
* Enable SSL by default
* Improved SGStream with newer parameters and better support for arrays

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* TOASed value are overwritten with placeholder on update for SGStream
* PgBouncer SSL is not set when enabled
* Missing affinity in operator deployment
* Continuos DNS unix requests
* Syntax error in install-extensions managed script on distributed logs cluster

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.17.0/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.17.0)

# :rocket: Release 1.17.0-rc1 (2025-07-15)

## :notepad_spiral: NOTES

StackGres 1.17.0-rc1 is out! :confetti_ball: :champagne: 

Finally, support for Kubernetes 1.33 and OpenShift 4.18 and 4.19 has been added.

Also, you will enjoy some important bugfixes and improvements all around the place.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support for Kubernetes 1.33
* Support for OpenShift 4.18 and 4.19
* Added Postgres 17.5, 16.9, 15.13, 14.18, 13.21
* Added Patroni 4.0.6
* Added Babelfish for PostgreSQL 16.6
* Update Quarkus to 3.22
* Updated base images and other components
* Disable Envoy by default
* Enable SSL by default
* Improved SGStream with newer parameters and better support for arrays

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* TOASed value are overwritten with placeholder on update for SGStream
* PgBouncer SSL is not set when enabled
* Missing affinity in operator deployment
* Continuos DNS unix requests
* Syntax error in install-extensions managed script on distributed logs cluster

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.17.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.17.0-rc1)

# :rocket: Release 1.16.3 (2025-05-28)

## :notepad_spiral: NOTES

StackGres 1.16.3 is out! :confetti_ball: :champagne: 

This release brings some fixes to improve overall stability.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

Nothing new here! :eyes:

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Files.list is not in a try-with-resources block generating growing opened file descriptors (and native memory usage).

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.16.3/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.16.3)

# :rocket: Release 1.16.2 (13-05-2025)

## :notepad_spiral: NOTES

StackGres 1.16.2 is out! :confetti_ball: :champagne: 

This release brings some fixes to improve overall stability.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

Nothing new here! :eyes:

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Helm chart sgconfig serialization issue
* Slot not being deleted when created with dynamicConfig
* Coordinator and shards services should inherit all the sharded cluster's services config
* Resharding fails when optional resharding section is not defined
* When more than one custom resource with the same name exists generated resources gets invalidated
* Major version upgrade SGDBOps is not respecting maxErrorsAfterUpgrade

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.16.2/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.16.2)

# :rocket: Release 1.16.1 (2025-04-21)

## :notepad_spiral: NOTES

StackGres 1.16.1 is out! :confetti_ball: :champagne: 

This patch release fix a few issues encountered in version 1.16.0, please upgrade as soon as possible.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Listen for PVC changes in order to reconcile clusters

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* SGShardedCluster fail adding Citus extensions for Postgres 17
* Readiness Web client is not closed
* When extension version is not set latest version is not chosen

## Web Console

* TPS not decoded for the delta encoding

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.16.1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.16.1)

# :rocket: Release 1.16.0 (2025-04-14)

## :notepad_spiral: NOTES

StackGres 1.16.0 reached GA! :muscle_tone1: :rocket: :rainbow: :sunny:

Less containers is better. Now you can reduce your SGCluster's Pod to only 2 containers (and just 1 init container) if you want leaner (but with less functionalities) Pods. :muscle:

A long waited functionality that removes the validation of referenced resources in Webhooks. You will no more receive messages about missing resources opening the door to a much more pleasant DevOps experience. We also tried to reduce the friction with tools like ArgoCD (still not officially supported though, please, be patient :sweat_smile:) by avoiding setting the infamous caBundle and other small changes that should improve the situation. Last but not least the creation of default resources is now handled by the operator reconciliation cycle and will not be created in the Webhook as we did before.

Setting a configuration parameter without having to create a separate SGPostgresConfig now is possible, and not only for Postgres configuration, we included setting directly Pod's resources, pooling and, now, you have the ability to modify postgres_exporter queries!

Improvements on SGStream should allow to fix some issues and make this component more useful for CDC use cases. We wait for your feedback!

Also much more functionalities and fixes where added to this release that comes strong, hoping you will enjoy it.

So, what you are waiting for? Try this release and have a look at the future of StackGres!

## :sparkles: NEW FEATURES AND CHANGES

* Envoy 1.33.2
* FluentBit 4.0.0
* OTEL Collector 0.123.1
* Bebelfish Compass 2025.04
* Added condition to check when initial scripts are applied
* Generate default configs using reconciliation cycle
* Removed reference validation on webhook calls
* Allow to override SGInstanceProfile, SGPostgresConfig and SGPoolingConfig
* Support for Grafana v10 and v11
* Support to overwrite and add postgres exporter queries
* Allow to encrypt stored object with libsodium or OpenPGP
* If possible choose the latest version of the extension when multiple versions are available and version is not set
* Support for Debezium asyncronous engine in SGStream
* Store the status of annotation signal in the PersistentVolume for SGStream
* Set the SGStream as Completed when running with a Deployment
* Support to update SGStream spec when locked or when maxRetries is not set (when using a Deployment).
* Support for skipping drop of replication slot and publication on tombstone for SGStream
* Include the node name of each Pod in the SGCluster status
* Allow to change the operator listening port, Service port and to set hostNetwork
* Added memory limiter and set resources for OTEL collector
* Avoid setting caBundle in webhooks
* Follow webhooks original ordering
* Avoid usage of nullable in CRDs
* Removed pgbouncer auth, setup-scripts and cluster controller init containers and merged remaining init containers
* Improved performance when using Kubernetes DCS for Patroni
* Removed priorityTimeout in favor of last execution fairness policy for reconciliation thread pool

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Extension installation permission issue
* Patroni reset is not skipped for replicas after major version upgrade
* Major version upgrade SGDbOps store wrong source backup path
* Wrong original SGStream service path in webhooks
* Separate resources cache by resource that generates them to avoid stale resources
* Failure to apply default naming strategy when topic has not the expected prefix in SGStream
* Can not change the shards clusters count if restored from a backup
* Controller fails with error when pgbouncer instance is not configured properly or unavailable
* NullPointerException in cleanupNonGarbageCollectedResources
* SGStream uses wrong SGCluster service name
* Prevent pg_total_relation_size from blocking WAL fetching for replicas
* Major version upgrade fail on any error
* SGStream source database used instead of target database

## Web Console

* Some properties are not checked for undefined

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.16.0-rc2/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.16.0)

# :rocket: Release 1.16.0-rc3 (2025-04-02)

## :notepad_spiral: NOTES

StackGres 1.16.0-rc3 has landed! :rocket: :rainbow: :sunny:

Less containers is better. Now you can reduce your SGCluster's Pod to only 2 containers (and just 1 init container) if you want leaner (but with less functionalities) Pods. :muscle:

A long waited functionality that changes a bit the validation of referenced resources in Webhooks. You will no more receive messages about missing resources opening the door to a much more pleasant DevOps experience. We also tried to reduce the friction with tools like ArgoCD (still not officially supported though, please, be patient :sweat_smile:) by avoiding setting the infamous caBundle and other small changes that should improve the situation. Last but not least the creation of default resources is now handled by the operator reconciliation cycle and will not be created in the Webhook as we did before.

Setting a configuration parameter without having to create a separate SGPostgresConfig now is possible, but not only for Postgres configuration, we included setting directly Pod's resources, pooling and, now, you have the ability to modify postgres_exporter queries!

Improvements on SGStream should allow to fix some issues and make this component more useful for CDC use cases. We wait for your feedback!

Also much more functionalities and fixes where added to this release that comes strong, hoping you will enjoy it.

So, what you are waiting for? Try this release and have a look at the future of StackGres!

## :sparkles: NEW FEATURES AND CHANGES

* Generate default configs using reconciliation cycle
* Removed reference validation on webhook calls
* Allow to override SGInstanceProfile, SGPostgresConfig and SGPoolingConfig
* Support for Grafana v10 and v11
* Support to overwrite and add postgres exporter queries
* Allow to encrypt stored object with libsodium or OpenPGP
* If possible choose the latest version of the extension when multiple versions are available and version is not set
* Support for Debezium asyncronous engine in SGStream
* Store the status of annotation signal in the PersistentVolume for SGStream
* Set the SGStream as Completed when running with a Deployment
* Support to update SGStream spec when locked or when maxRetries is not set (when using a Deployment).
* Support for skipping drop of replication slot and publication on tombstone for SGStream
* Include the node name of each Pod in the SGCluster status
* Allow to change the operator listening port, Service port and to set hostNetwork
* Added memory limiter and set resources for OTEL collector
* Avoid setting caBundle in webhooks
* Follow webhooks original ordering
* Avoid usage of nullable in CRDs
* Removed pgbouncer auth, setup-scripts and cluster controller init containers and merged remaining init containers
* Improved performance when using Kubernetes DCS for Patroni

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Extension installation permission issue
* Patroni reset is not skipped for replicas after major version upgrade
* Major version upgrade SGDbOps store wrong source backup path
* Wrong original SGStream service path in webhooks
* Separate resources cache by resource that generates them to avoid stale resources
* Failure to apply default naming strategy when topic has not the expected prefix in SGStream
* Can not change the shards clusters count if restored from a backup
* Controller fails with error when pgbouncer instance is not configured properly or unavailable
* NullPointerException in cleanupNonGarbageCollectedResources
* SGStream uses wrong SGCluster service name
* Prevent pg_total_relation_size from blocking WAL fetching for replicas
* Major version upgrade fail on any error
* SGStream source database used instead of target database

## Web Console

* Some properties are not checked for undefined

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.16.0-rc2/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.16.0-rc3)

# :rocket: Release 1.16.0-rc2 (2025-03-27)

## :notepad_spiral: NOTES

StackGres 1.16.0-rc2 has landed! :rocket: :rainbow: :sunny:

Less containers is better. Now you can reduce your SGCluster's Pod to only 2 containers (and just 1 init container) if you want leaner (but with less functionalities) Pods. :muscle:

A long waited functionality that changes a bit the validation of referenced resources in Webhooks. You will no more receive messages about missing resources opening the door to a much more pleasant DevOps experience. We also tried to reduce the friction with tools like ArgoCD (still not officially supported though, please, be patient :sweat_smile:) by avoiding setting the infamous caBundle and other small changes that should improve the situation. Last but not least the creation of default resources is now handled by the operator reconciliation cycle and will not be created in the Webhook as we did before.

Setting a configuration parameter without having to create a separate SGPostgresConfig now is possible, but not only for Postgres configuration, we included setting directly Pod's resources, pooling and, now, you have the ability to modify postgres_exporter queries!

Improvements on SGStream should allow to fix some issues and make this component more useful for CDC use cases. We wait for your feedback!

Also much more functionalities and fixes where added to this release that comes strong, hoping you will enjoy it.

So, what you are waiting for? Try this release and have a look at the future of StackGres!

## :sparkles: NEW FEATURES AND CHANGES

* Generate default configs using reconciliation cycle
* Removed reference validation on webhook calls
* Allow to override SGInstanceProfile, SGPostgresConfig and SGPoolingConfig
* Support for Grafana v10 and v11
* Support to overwrite and add postgres exporter queries
* Allow to encrypt stored object with libsodium or OpenPGP
* If possible choose the latest version of the extension when multiple versions are available and version is not set
* Support for Debezium asyncronous engine in SGStream
* Store the status of annotation signal in the PersistentVolume for SGStream
* Set the SGStream as Completed when running with a Deployment
* Support to update SGStream spec when locked or when maxRetries is not set (when using a Deployment).
* Support for skipping drop of replication slot and publication on tombstone for SGStream
* Include the node name of each Pod in the SGCluster status
* Allow to change the operator listening port, Service port and to set hostNetwork
* Added memory limiter and set resources for OTEL collector
* Avoid setting caBundle in webhooks
* Follow webhooks original ordering
* Avoid usage of nullable in CRDs
* Removed pgbouncer auth, setup-scripts and cluster controller init containers and merged remaining init containers

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Extension installation permission issue
* Patroni reset is not skipped for replicas after major version upgrade
* Major version upgrade SGDbOps store wrong source backup path
* Wrong original SGStream service path in webhooks
* Separate resources cache by resource that generates them to avoid stale resources
* Failure to apply default naming strategy when topic has not the expected prefix in SGStream
* Can not change the shards clusters count if restored from a backup
* Controller fails with error when pgbouncer instance is not configured properly or unavailable
* NullPointerException in cleanupNonGarbageCollectedResources
* SGStream uses wrong SGCluster service name
* Prevent pg_total_relation_size from blocking WAL fetching for replicas
* Major version upgrade fail on any error
* SGStream source database used instead of target database

## Web Console

* Some properties are not checked for undefined

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.16.0-rc2/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.16.0-rc2)

# :rocket: Release 1.16.0-rc1 (2025-03-19)

## :notepad_spiral: NOTES

StackGres 1.16.0-rc1 has landed! :rocket: :rainbow: :sunny:

Less containers is better. Now you can reduce your SGCluster's Pod to only 2 containers (and just 1 init container) if you want leaner (but with less functionalities) Pods. :muscle:

A long waited functionality that changes a bit the validation of referenced resources in Webhooks. You will no more receive messages about missing resources opening the door to a much more pleasant DevOps experience. We also tried to reduce the friction with tools like ArgoCD (still not officially supported though, please, be patient :sweat_smile:) by avoiding setting the infamous caBundle and other small changes that should improve the situation. Last but not least the creation of default resources is now handled by the operator reconciliation cycle and will not be created in the Webhook as we did before.

Setting a configuration parameter without having to create a separate SGPostgresConfig now is possible, but not only for Postgres configuration, we included setting directly Pod's resources, pooling and, now, you have the ability to modify postgres_exporter queries!

Improvements on SGStream should allow to fix some issues and make this component more useful for CDC use cases. We wait for your feedback!

Also much more functionalities and fixes where added to this release that comes strong, hoping you will enjoy it.

So, what you are waiting for? Try this release and have a look at the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Generate default configs using reconciliation cycle
* Removed reference validation on webhook calls
* Allow to override SGInstanceProfile, SGPostgresConfig and SGPoolingConfig
* Support for Grafana v10 and v11
* Support to overwrite and add postgres exporter queries
* Allow to encrypt stored object with libsodium or OpenPGP
* If possible choose the latest version of the extension when multiple versions are available and version is not set
* Support for Debezium asyncronous engine in SGStream
* Store the status of annotation signal in the PersistentVolume for SGStream
* Set the SGStream as Completed when running with a Deployment
* Support to update SGStream spec when locked or when maxRetries is not set (when using a Deployment).
* Support for skipping drop of replication slot and publication on tombstone for SGStream
* Include the node name of each Pod in the SGCluster status
* Allow to change the operator listening port, Service port and to set hostNetwork
* Added memory limiter and set resources for OTEL collector
* Avoid setting caBundle in webhooks
* Follow webhooks original ordering
* Avoid usage of nullable in CRDs
* Removed pgbouncer auth, setup-scripts and cluster controller init containers and merged remaining init containers

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Extension installation permission issue
* Patroni reset is not skipped for replicas after major version upgrade
* Major version upgrade SGDbOps store wrong source backup path
* Wrong original SGStream service path in webhooks
* Separate resources cache by resource that generates them to avoid stale resources
* Failure to apply default naming strategy when topic has not the expected prefix in SGStream
* Can not change the shards clusters count if restored from a backup
* Controller fails with error when pgbouncer instance is not configured properly or unavailable
* NullPointerException in cleanupNonGarbageCollectedResources
* SGStream uses wrong SGCluster service name

## Web Console

* Some properties are not checked for undefined

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.16.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.16.0-rc1)

# :rocket: Release 1.15.2 (2025-03-06)

## :notepad_spiral: NOTES

StackGres 1.15.2 is out! :confetti_ball: :champagne: 

This is a security release that includes new version of Patroni, Postgres and other components and libraries used by StackGres.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Patroni 4.0.5
* Postgres 17.4, 16.8, 15.12, 14.17, 13.20

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

Nothing new here! :eyes:

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.15.2/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.15.2)

# :rocket: Release 1.15.1 (2025-02-17)

## :notepad_spiral: NOTES

StackGres 1.15.1 is out! :confetti_ball: :champagne: 

This hotfix release solves a regression that may affect reconciliation of resources when a high number of CRs are present in the K8s cluster.

Please, proceed to upgrade StackGres as soon as possible! 

## :sparkles: NEW FEATURES AND CHANGES

* Disabled thread pool by default
* Added priority timeout to thread pool
* Improve thread pool to use also timestamps for priority
* Added monitoring to all StackGres components (operator, restapi, controllers and some database operations)

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Major version upgrade rollback broken when sync or strict-sync replication mode is set
* ConcurrentModificationException on ReconciliatorWorkerThreadPool
* Metrics are not exported as expected in the prometheus endpoint for SGStreams

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.15.1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.15.1)

# :rocket: Release 1.15.0 (2025-02-03)

## :notepad_spiral: NOTES

StackGres 1.15.0 has been released! :confetti_ball: :champagne: :soap: :tools: 

This release brings support for Patroni 4 that include some cool features like "Quorum-based failover" or "Register Citus secondaries in pg_dist_node" but also many bugfixes.

Another important change is the new SGDistributedLogs that after the upgrade will generate an SGCluster. This change allow to use SGDbOps in order to perform restart, security upgrade, minor version and major version upgrades operations.

Performance and responsiveness have been improved by allowing to reconcile StackGres custom resources in parallel with a queue that gives priority to reconciliation of StackGres custom resources that receive changes. This should improve the use cases with many StackGres custom resources.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.32
* Support for Patroni 4.0.4
* Support for Postgres 17.2, 16.6, 15.10, 14.15, 13.18 and 12.22
* Envoy 1.32.3
* PgBouncer 1.24.0
* Prometheus Postgres Exporter 0.16.0
* Fluent-bit 3.2.4
* Fluentd 1.18.0
* OTEL Collector 0.117.0
* Babelfish Compass 2024.12
* Make SGDistributedLogs generate an SGCluster
* Support for headless service
* Improve performance and responsiveness
* Allow to disable Envoy
* Allow to set parameter `track_commit_timestamp` (useful in some migration cases)
* Allow `pg_hba` section to be set for Patroni
* Allow to set any value for init containers in SGInstanceProfile
* Updated Debezium dependency to 3.0.7 in SGStream

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Backups using VolumeSnapshot do not work for Postgres 14 or less
* Make int-or-string fields real int-or-string and not just string fields
* Allow to rollback upgrade when CHECKPOINT fail
* SGScript and SGCluster script entry version are resetted to 0 if part of a reconciliation of another CR
* SGShardedCluster metrics are not collected by default
* SGScript fails if superuser username is changed
* Major version upgrade fails when replication mode is not async
* After restore from snapshot primary fail to restart
* Prometheus instances are not filtered by monitors name and namespace
* Replication initialization from newly created backup create many backups
* wal-g s3 CA not used on restore
* When SGConfig is not found on uninstall ignore it
* Installation fail on k8s 1.30+
* REST API logs endpoints role filter is not mapping correctly values
* SGShardedDbOps resharding operation fails when using old versions of patroni
* patronictl selected version is the oldest minor

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.15.0/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.15.0)

# :rocket: Release 1.15.0-rc4 (2025-01-30)

## :notepad_spiral: NOTES

StackGres 1.15.0-rc4 has been released! :confetti_ball: :champagne: 

This release brings support for Patroni 4 that include some cool features like "Quorum-based failover" or "Register Citus secondaries in pg_dist_node" but also many bugfixes.

Another important change is the new SGDistributedLogs that after the upgrade will generate an SGCluster. This change allow to use SGDbOps in order to perform restart, security upgrade, minor version and major version upgrades operations.

Performance and responsiveness have been improved by allowing to reconcile StackGres custom resources in parallel with a queue that gives priority to reconciliation of StackGres custom resources that receive changes. This should improve the use cases with many StackGres custom resources.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.32
* Support for Patroni 4.0.4
* Support for Postgres 17.2, 16.6, 15.10, 14.15, 13.18 and 12.22
* Envoy 1.32.3
* PgBouncer 1.24.0
* Prometheus Postgres Exporter 0.16.0
* Fluent-bit 3.2.4
* Fluentd 1.18.0
* OTEL Collector 0.117.0
* Babelfish Compass 2024.12
* Make SGDistributedLogs generate an SGCluster
* Support for headless service
* Improve performance and responsiveness
* Allow to disable Envoy
* Allow to set parameter `track_commit_timestamp` (useful in some migration cases)
* Allow `pg_hba` section to be set for Patroni
* Allow to set any value for init containers in SGInstanceProfile
* Updated Debezium dependency to 3.0.7 in SGStream

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Backups using VolumeSnapshot do not work for Postgres 14 or less
* Make int-or-string fields real int-or-string and not just string fields
* Allow to rollback upgrade when CHECKPOINT fail
* SGScript and SGCluster script entry version are resetted to 0 if part of a reconciliation of another CR
* SGShardedCluster metrics are not collected by default
* SGScript fails if superuser username is changed
* Major version upgrade fails when replication mode is not async
* After restore from snapshot primary fail to restart
* Prometheus instances are not filtered by monitors name and namespace
* Replication initialization from newly created backup create many backups
* wal-g s3 CA not used on restore
* When SGConfig is not found on uninstall ignore it
* Installation fail on k8s 1.30+
* REST API logs endpoints role filter is not mapping correctly values
* SGShardedDbOps resharding operation fails when using old versions of patroni
* patronictl selected version is the oldest minor

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.15.0-rc4/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.15.0-rc4)

# :rocket: Release 1.15.0-rc3 (2025-01-21)

## :notepad_spiral: NOTES

StackGres 1.15.0-rc3 has been released! :confetti_ball: :champagne: 

This release brings support for Patroni 4 that include some cool features like "Quorum-based failover" or "Register Citus secondaries in pg_dist_node" but also many bugfixes.

Another important change is the new SGDistributedLogs that after the upgrade will generate an SGCluster. This change allow to use SGDbOps in order to perform restart, security upgrade, minor version and major version upgrades operations.

Performance and responsiveness have been improved by allowing to reconcile StackGres custom resources in parallel with a queue that gives priority to reconciliation of StackGres custom resources that receive changes. This should improve the use cases with many StackGres custom resources.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.32
* Support for Patroni 4.0.4
* Support for Postgres 17.2, 16.6, 15.10, 14.15, 13.18 and 12.22
* Envoy 1.32.3
* PgBouncer 1.24.0
* Prometheus Postgres Exporter 0.16.0
* Fluent-bit 3.2.4
* Fluentd 1.18.0
* OTEL Collector 0.117.0
* Babelfish Compass 2024.12
* Make SGDistributedLogs generate an SGCluster
* Support for headless service
* Improve performance and responsiveness
* Allow to disable Envoy
* Allow to set parameter `track_commit_timestamp` (useful in some migration cases)
* Allow `pg_hba` section to be set for Patroni

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Make int-or-string fields real int-or-string and not just string fields
* Allow to rollback upgrade when CHECKPOINT fail
* SGScript and SGCluster script entry version are resetted to 0 if part of a reconciliation of another CR
* SGShardedCluster metrics are not collected by default
* SGScript fails if superuser username is changed
* Major version upgrade fails when replication mode is not async
* After restore from snapshot primary fail to restart
* Prometheus instances are not filtered by monitors name and namespace
* Replication initialization from newly created backup create many backups
* wal-g s3 CA not used on restore
* When SGConfig is not found on uninstall ignore it
* Installation fail on k8s 1.30+

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.15.0-rc3/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.15.0-rc3)

# :rocket: Release 1.14.3 (2025-01-13)

## :notepad_spiral: NOTES

StackGres 1.14.3 is out! :confetti_ball: :champagne: 

This hotfix release solve an important issue regarding SGClusters restored from VolumeSnapshots that may get unavailable after restart. Version 1.14.2 claimed to solve the issue but was not the case.

For more info see https://gitlab.com/ongresinc/stackgres/-/issues/2956

Do not wait more and upgrade StackGres to this latest version!

## :sparkles: NEW FEATURES AND CHANGES

Nothing new here! :eyes:

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Primary Pod does not start when restarted after a restore from snapshot backup

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.14.3/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.14.3)

# :rocket: Release 1.14.2 (2024-12-26)

## :notepad_spiral: NOTES

StackGres 1.14.2 is out! :confetti_ball: :champagne: 

This hotfix release solve an important issue regarding SGClusters restored from VolumeSnapshots that may get unavailable after restart.

For more info see https://gitlab.com/ongresinc/stackgres/-/issues/2956

Do not wait more and upgrade StackGres to this latest version! 

## :sparkles: NEW FEATURES AND CHANGES

Nothing new here! :eyes:

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Primary Pod does not start when restarted after a restore from snapshot backup
* Replication initialization from newly created backup create many backups
* Prometheus instances are not filtered by monitors name and namespace

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.14.2/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.14.2)

# :rocket: Release 1.15.0-rc2 (2024-12-11)

## :notepad_spiral: NOTES

StackGres 1.15.0-rc2 has been released! :confetti_ball: :champagne: 

This release brings support for Patroni 4 that include some cool features like "Quorum-based failover" or "Register Citus secondaries in pg_dist_node" but also many bugfixes.

Another important change is the new SGDistributedLogs that after the upgrade will generate an SGCluster. This change allow to use SGDbOps in order to perform restart, security upgrade, minor version and major version upgrades operations.

Performance and responsiveness have been improved by allowing to reconcile StackGres custom resources in parallel with a queue that gives priority to reconciliation of StackGres custom resources that receive changes. This should improve the use cases with many StackGres custom resources.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support for Patroni 4.0.4
* Support for Postgres 17.2, 16.6, 15.10, 14.15, 13.18 and 12.22
* Envoy 1.32.1
* Prometheus Postgres Exporter 0.16.0
* Fluent-bit 3.2.1
* OTEL Collector 0.114.0
* Make SGDistributedLogs generate an SGCluster
* Support for headless service
* Improve performance and responsiveness
* Allow to disable Envoy

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Make int-or-string fields real int-or-string and not just string fields
* Allow to rollback upgrade when CHECKPOINT fail
* SGScript and SGCluster script entry version are resetted to 0 if part of a reconciliation of another CR
* SGShardedCluster metrics are not collected by default

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.15.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.15.0-rc2)

# :rocket: Release 1.15.0-rc1 (2024-12-04)

## :notepad_spiral: NOTES

StackGres 1.15.0-rc1 is (finally) out! :confetti_ball: :champagne: 

This release brings support for Patroni 4 that include some cool features like "Quorum-based failover" or "Register Citus secondaries in pg_dist_node" but also many bugfixes.

Another important change is the new SGDistributedLogs that after the upgrade will generate an SGCluster. This change allow to use SGDbOps in order to perform restart, security upgrade, minor version and major version upgrades operations.

Performance and responsiveness have been improved by allowing to reconcile StackGres custom resources in parallel with a queue that gives priority to reconciliation of StackGres custom resources that receive changes. This should improve the use cases with many StackGres custom resources.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support for Patroni 4.0.4
* Support for Postgres 17.2, 16.6, 15.10, 14.15, 13.18 and 12.22
* Envoy 1.32.1
* Prometheus Postgres Exporter 0.16.0
* Fluent-bit 3.2.1
* OTEL Collector 0.114.0
* Make SGDistributedLogs generate an SGCluster
* Support for headless service
* Improve performance and responsiveness
* Allow to disable Envoy

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Make int-or-string fields real int-or-string and not just string fields
* Allow to rollback upgrade when CHECKPOINT fail
* SGScript and SGCluster script entry version are resetted to 0 if part of a reconciliation of another CR

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.15.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.15.0-rc1)

# :rocket: Release 1.14.1 (2024-11-25)

## :notepad_spiral: NOTES

StackGres 1.14.1 fix some issues that were encountered in 1.14.0 regarding the metrics collector but also other few issues :bug: :gun: :tada: 

So, what you are waiting for to try this release and harden your StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

Nothing new here! :eyes:

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Monitoring tab in showing stats/metrics only for a single Pod in each SGCluster and collector configuration is not reloaded
* When installing the operator for the first time the OpenTelemetry collector do not start as expected
* Major version upgrade to Postgres 17 fail with unrecognized configuration parameter "lc_collate"
* Operator seizes to function when a namespace listed in allowedNamespaces does not exist

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.14.1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.14.1)

# :rocket: Release 1.14.0 (04-11-2024)

## :notepad_spiral: NOTES

StackGres 1.14.0 is out! :confetti_ball: :champagne: 

This new release of StackGres add a new layer that will collect all the metrics in an OpenTelemetry collector Deployment, allowing to export them
 in a variety of platforms other than Prometheus. But we also included a lot of fixes to improve stability!

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Postgres 17
* Support OpenShift 4.17
* Added OpenTelemetry Collector layer
* Added common retry logic to backup and sharded backup and extended its usage

## Web Console

* Support for SGStreams in the Web Console
* Include graphs for TPS, percentile histogram and most used statements on becnhmark status

## :bug: FIXES

* SGStream fields are not rendered in the CSV of operator bundle
* Allow sgconfig to be installed before operator
* Backup using volume snapshot do not wait on conflict
* Sharded cluster is not setting any postgres services configuration
* Replication initialization is not using the latest backup
* Backups, dbops, sharded backups and sharded dbops change to completed state only on full resync
* Sharded dbops/backup do not cleanup previous dbops/backup when retrying
* Disable pager for patroni container
* Validating webhook returns 500 on wrong enum values
* Avoid removing resources when owner is deleted
* Avoid adding StatefulSet owner reference on PVC to avoid deletion on removal
* pg_replication_is_replica metric has no unique labels
* Disable pager for patroni container
* Sharded backup run forewer if a generated SGBackup not yet completed is removed
* Backup configuration interfere with replicate from

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.14.0/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.14.0)

# :rocket: Release 1.14.0-rc2 (29-10-2024)

## :notepad_spiral: NOTES

StackGres 1.14.0-rc2 is out! :confetti_ball: :champagne: 

This new release of StackGres add a new layer that will collect all the metrics in an OpenTelemetry collector Deployment, allowing to export them
 in a variety of platforms other than Prometheus. But we also included a lot of fixes to improve stability!

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Postgres 17
* Support OpenShift 4.17
* Added OpenTelemetry Collector layer
* Added common retry logic to backup and sharded backup and extended its usage

## Web Console

* Support for SGStreams in the Web Console
* Include graphs for TPS, percentile histogram and most used statements on becnhmark status

## :bug: FIXES

* SGStream fields are not rendered in the CSV of operator bundle
* Allow sgconfig to be installed before operator
* Backup using volume snapshot do not wait on conflict
* Sharded cluster is not setting any postgres services configuration
* Replication initialization is not using the latest backup
* Backups, dbops, sharded backups and sharded dbops change to completed state only on full resync
* Sharded dbops/backup do not cleanup previous dbops/backup when retrying
* Disable pager for patroni container
* Validating webhook returns 500 on wrong enum values
* Avoid removing resources when owner is deleted
* Avoid adding StatefulSet owner reference on PVC to avoid deletion on removal
* pg_replication_is_replica metric has no unique labels
* Disable pager for patroni container
* Sharded backup run forewer if a generated SGBackup not yet completed is removed
* Backup configuration interfere with replicate from

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.14.0-rc2/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.14.0-rc2)

# :rocket: Release 1.14.0-rc1 (24-10-2024)

## :notepad_spiral: NOTES

StackGres 1.14.0-rc1 is out! :confetti_ball: :champagne: 

This new release of StackGres add a new layer that will collect all the metrics in an OpenTelemetry collector layer, allowing to export them
 in a variety of other platform than Prometheus. But we also added a lot of fixes to improve stability!

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Postgres 17
* Added OpenTelemetry Collector layer
* Added common retry logic to backup and sharded backup and extended its usage

## Web Console

* Support for SGStreams in the Web Console
* Include graphs for TPS, percentile histogram and most used statements on becnhmark status

## :bug: FIXES

* SGStream fields are not rendered in the CSV of operator bundle
* Allow sgconfig to be installed before operator
* Backup using volume snapshot do not wait on conflict
* Sharded cluster is not setting any postgres services configuration
* Replication initialization is not using the latest backup
* Backups, dbops, sharded backups and sharded dbops change to completed state only on full resync
* Sharded dbops/backup do not cleanup previous dbops/backup when retrying
* Disable pager for patroni container
* Validating webhook returns 500 on wrong enum values
* Avoid removing resources when owner is deleted
* Avoid adding StatefulSet owner reference on PVC to avoid deletion on removal
* pg_replication_is_replica metric has no unique labels
* Disable pager for patroni container
* Sharded backup run forewer if a generated SGBackup not yet completed is removed
* Backup configuration interfere with replicate from

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.14.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.14.0-rc1)

# :rocket: Release 1.13.0 (09-09-2024)

## :notepad_spiral: NOTES

StackGres 1.13.0 is out! This release brings new feature for benchmark SGDbOps and SGStream and an important security patch that fix a bug allowing remote connections without password after a major version upgrade. :confetti_ball: :champagne: :bug: :gun:

Please make sure to apply the changes as mentioned in [this issue](https://gitlab.com/ongresinc/stackgres/-/issues/2873) to mitigate the bug for existing clusters.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.31
* Support Postgres 16.4, 15.8, 14.13, 13.16, 12.20
* Support wal-g 3.0.3
* Support PgBouncer 1.23.1
* Support prometheus exporter 0.15.0
* Support FluentBit 3.1.6
* Support Fluentd 1.17.1
* Support Babelfish Compass 2024.07
* Added sampling and pgbench replay to benchmark SGDbOps
* Support PgLambda target in SGStream
* Improved pgbench spec and status

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* SGDbOps major version upgrade leave a pg_hba.conf with trust permissions that allow the connection of remote users without requesting a password.
* SGStream REST API GET endpoint returns 500 on sourceEventPosition
* Added SGStream to the can-i REST API endpoint
* Typo in SGStream.spec.pods.scheduling (was schedule)
* Support OIDC providers that don't implement end_session_endpoint

## Web Console

* Replace PITR graph default selectors with custom ID

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.13.0/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.13.0)

# :rocket: Release 1.13.0-rc2 (2024-09-04)

## :notepad_spiral: NOTES

StackGres 1.13.0-rc2 is out! This release brings new feature for benchmark SGDbOps and SGStream. :confetti_ball: :champagne: 

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.31
* Support Postgres 16.4, 15.8, 14.13, 13.16, 12.20
* Support wal-g 3.0.3
* Support PgBouncer 1.23.1
* Support prometheus exporter 0.15.0
* Support FluentBit 3.1.6
* Support Fluentd 1.17.1
* Support Babelfish Compass 2024.07
* Added sampling and pgbench replay to benchmark SGDbOps
* Support PgLambda target in SGStream
* Improved pgbench spec and status

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* SGDbOps major version upgrade leave a pg_hba.conf with trust permissions that allow the connection of remote users without requesting a password. 
* SGStream REST API GET endpoint returns 500 on sourceEventPosition
* Added SGStream to the can-i REST API endpoint
* Typo in SGStream.spec.pods.scheduling (was schedule)
* Support OIDC providers that don't implement end_session_endpoint

## Web Console

* Replace PITR graph default selectors with custom ID

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.13.0-rc2/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.13.0-rc2)

# :rocket: Release 1.13.0-rc1 (2024-08-27)

## :notepad_spiral: NOTES

StackGres 1.13.0-rc1 is out! This release brings new feature for benchmark SGDbOps and SGStream. :confetti_ball: :champagne: 

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.31
* Support Postgres 16.4, 15.8, 14.13, 13.16, 12.20
* Support wal-g 3.0.3
* Support PgBouncer 1.23.1
* Support prometheus exporter 0.15.0
* Support FluentBit 3.1.6
* Support Fluentd 1.17.1
* Support Babelfish Compass 2024.07
* Added sampling and pgbench replay to benchmark SGDbOps
* Support PgLambda target in SGStream
* Improved pgbench spec and status

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* SGStream REST API GET endpoint returns 500 on sourceEventPosition
* Added SGStream to the can-i REST API endpoint
* Typo in SGStream.spec.pods.scheduling (was schedule)
* Support OIDC providers that don't implement end_session_endpoint

## Web Console

* Replace PITR graph default selectors with custom ID

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.13.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.13.0-rc1)

# :rocket: Release 1.12.0 (2024-07-22)

## :notepad_spiral: NOTES

StackGres 1.12.0 has made it! :space_invader: :sparkles: :confetti_ball: :champagne: 

This release comes with a new shiny feature that allows to do change data capture (CDC) of your Postgres instances in an easy way with SGStream CRD! CDC provides real-time or near-real-time movement of data by moving and processing data continuously as new database events occur. In particular SGStream offer two main functionality: streaming Postgres CDC events to a CloudEvent service or moving data to another SGCluster instance. The feature is in an alpha stage so your feedback will be a great way to make it more stable.

This release also brings fixes and security features. In particular Envoy has been updated to 1.30.4 solving a bug that was haunting us for some time preventing the new version to be included. Sadly we can not upgrade to latest version of PgBouncer due to a [regression found in version 1.23.0](https://github.com/pgbouncer/pgbouncer/issues/1103), we hope to be able to include a newer version as soon as it is released.

Go on and have a look to this new release of StackGres, we hope you envoy it!

## :sparkles: NEW FEATURES AND CHANGES

* Added Patroni 3.3.2
* Added Babelfish for PostgreSQL 16.2
* Added Envoy 1.30.4
* Added FluentBit 3.0.7
* Added Kubectl 1.30.2 and 1.28.11
* Added SGStream
* Support SGStream from Postgres or SGCluster to CloudEvent service
* Support SGStream from Postgres or SGCluster to SGCluster
* Allow to set Patroni callbacks, pre_promote and before_stop
* Allow to overwrite postgres binaries using customVolumeMounts under /usr/local/bin
* Add warning on setting manually backups paths in documentation

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Wrong PgBouncer queries for Postgres Exporter configuration
* Errors when setting imagePullSecrets
* SGDbOps, SGShardedDbOps and SGStream always fail on retry
* SGShardedCluster REST API endpoint was overlapping managed sql sections
* pg_stat_progress_vacuum and pg_stat_progress_cluster metric is missing schema and table names
* Broken storageclasses permissions for Web Console ClusterRoles

## Web Console

* Make sure sgdbop status conditions exist before requesting its data
* Remove fixed sgconfig name
* Include default case on isDeletable header method
* Show "loading data" message while processing rest api info
* Do not use deprecated fields for envoy config

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.12.0/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.12.0)

# :rocket: Release 1.12.0-rc1 (2024-07-17)

## :notepad_spiral: NOTES

StackGres 1.12.0-rc1 has finally landed! :sparkles: :confetti_ball: :champagne: 

This release comes with a new shiny feature that allows to do change data capture (CDC) of your Postgres instances in an easy way with SGStream CRD! CDC provides real-time or near-real-time movement of data by moving and processing data continuously as new database events occur. In particular SGStream offer two main functionality, stream Postgres CDC events to a CloudEvent service or move directly data to another SGCluster instance. The feature is in an alpha stage so your feedback will be a great way to make it more stable.

This release also brings fixes and security features. In particular Envoy has been updated to 1.30.4 solving a bug that was haunting us for some time preventing the new version to be included. Sadly we can not upgrade to latest version of PgBouncer due to a [regression found in version 1.23.0](), we hope to be able to include a newer version as soon as it is released.

Go on and have a look to this new release of StackGres, we hope you envoy it!

## :sparkles: NEW FEATURES AND CHANGES

* Added Patroni 3.3.2
* Added Babelfish for PostgreSQL 16.2
* Added Envoy 1.30.4
* Added FluentBit 3.0.7
* Added Kubectl 1.30.2 and 1.28.11
* Added SGStream
* Support SGStream from Postgres or SGCluster to CloudEvent service
* Support SGStream from Postgres or SGCluster to SGCluster
* Allow to set Patroni callbacks, pre_promote and before_stop
* Allow to overwrite postgres binaries using customVolumeMounts under /usr/local/bin
* Add warning on setting manually backups paths in documentation

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Errors when setting imagePullSecrets
* SGDbOps, SGShardedDbOps and SGStream always fail on retry
* SGShardedCluster REST API endpoint was overlapping managed sql sections
* pg_stat_progress_vacuum and pg_stat_progress_cluster metric is missing schema and table names
* Broken storageclasses permissions for Web Console ClusterRoles

## Web Console

* Make sure sgdbop status conditions exist before requesting its data
* Remove fixed sgconfig name
* Include default case on isDeletable header method
* Show "loading data" message while processing rest api info
* Do not use deprecated fields for envoy config

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.12.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.12.0-rc1)

# :rocket: Release 1.11.0 (2024-06-10)

## :notepad_spiral: NOTES

StackGres 1.11.0 brings namespaces scoped operator! :confetti_ball: :champagne: 

You will be able to install the operator under OLM (OperatorHub / OpenShift) or with helm specifying the list of namespaces where the operator will be able to work with. It also offers the ability to disable ClusterRoles for the operator completely (with limitations in functionalities). With the exception of a ClusterRole for the Web Console / REST API if you want to still enable it.

And another load of small features and bugfixes! Do not wait, try this release and help us improve StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support for Kubernetes 1.30
* Added PostgreSQL 16.3, 15.7, 14.12, 13.15 and 12.19
* Added wal-g 3.0.1
* Added usql 0.19.1, pgcenter 0.9.2 and pg_activity 3.5.1
* Added Babelfish for PostgreSQL 15.5 and 14.10
* Added fluent-bit 3.0.6
* Support allowed namespaces for operator
* Allow to inject imagePullSecrets from the global configuration
* Added more logs to fluentd and fluentbit
* Added field maxRetries for SGBackup and SGShardedBackup
* Added PodMonitor for patroni

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Fluent-bit is not showing postgres logs
* Using Envoy port name instead of port number in PodMonitor
* Set patroni requests to 0 instead of a negative value
* Remove ports from patroni container
* Password are not updated in pg_dist_authinfo after restoring a sharded backup for citus
* When setting resources in the SGDbOps job the oprerator fail with a StringIndexOutOfBoundsException
* Requests and limits can not be set for some SGDbOps
* Relocate binaries fail with permissions denied

## Web Console

* Reload user permissions list when namespaces list has been updated
* Flush user permissions on logout
* When listing storage classes is not allowed, allow users to enter class names manually
* Prevent graph elements from covering pitr markers

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.11.0/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.11.0)

# :rocket: Release 1.11.0-rc1 (2024-06-06)

## :notepad_spiral: NOTES

StackGres 1.11.0-rc1 brings namespaces scoped operator! :confetti_ball: :champagne: 

You will be able to install the operator under OLM (OperatorHub / OpenShift) or with helm specifying the list of namespaces where the operator will be able to work with. It also offers the ability to disable ClusterRoles for the operator completely (with limitations in functionalities). With the exception of a ClusterRole for the Web Console / REST API if you want to still enable it.

And another load of small features and bugfixes! Do not wait, try this release and help us improve StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Added PostgreSQL 16.3, 15.7, 14.12, 13.15 and 12.19
* Added wal-g 3.0.1
* Added usql 0.19.1, pgcenter 0.9.2 and pg_activity 3.5.1
* Added Babelfish for PostgreSQL 15.5 and 14.10
* Added Envoy 1.30.1
* Added Fluent-bit 3.0.4
* Added Fluentd 1.17.0
* Support allowed namespaces for operator
* Allow to inject imagePullSecrets from the global configuration
* Added more logs to fluentd and fluentbit
* Added field maxRetries for SGBackup and SGShardedBackup
* Added PodMonitor for patroni

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Fluent-bit is not showing postgres logs
* Using Envoy port name instead of port number in PodMonitor
* Set patroni requests to 0 instead of a negative value
* Remove ports from patroni container
* Password are not updated in pg_dist_authinfo after restoring a sharded backup for citus
* When setting resources in the SGDbOps job the oprerator fail with a StringIndexOutOfBoundsException
* Requests and limits can not be set for some SGDbOps
* Relocate binaries fail with permissions denied

## Web Console

* Reload user permissions list when namespaces list has been updated
* Flush user permissions on logout
* When listing storage classes is not allowed, allow users to enter class names manually
* Prevent graph elements from covering pitr markers

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.11.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.11.0-rc1)

# :rocket: Release 1.10.0 (2024-04-29)

## :notepad_spiral: NOTES

StackGres 1.10.0 is here with a load of improvements and new features! :confetti_ball: :champagne: 

* Prepare for automatic horizontal (and [hopefully soon](https://github.com/kubernetes/autoscaler/blob/master/vertical-pod-autoscaler/enhancements/4016-in-place-updates-support/README.md) vertical) autoscaling with [KEDA](https://keda.sh/) integration (or [HPA](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) for a naive approach).

* Replication initialization has been finally upgraded in order to allow using existing backups or, if configured so, to create a new backup in order to use it for initialization.

So, what you are waiting for, upgrade to latest version of StackGres to enjoy all the goods! 

## :sparkles: NEW FEATURES AND CHANGES

* Support horizontal and vertical autoscaling
* Support replication initialization from backups
* Support for PersistentVolumeClaim in custom volumes
* Add metadata validation for SGCluster, SGShardedCluster and SGDistributedLogs
* Allow to specify ShardingSphere operator service account in SGShardedCluster
* Include the creationTimestamp in default object storage paths for backups
* Added nodePorts support to PatroniServices and ShardedClusterServices
* Added more metadata fields to REST API DTOs
* Allow to retain WALs for unmanaged lifecycle backups

## Web Console

* Support standby cluster promotion
* Support resource requirements on non production options on cluster, sharded cluster and distributedlogs forms

## :bug: FIXES

* Backup retention was removing WALs of old unmanaged lifecycle backups (breaking them)
* Custom volume mounts model is broken
* Restore sharded backup does not work with PITR
* Sharded backup must make sure the WALs with restore label to be archived
* Set sharded backup completed only if status has been set with backup information
* Sharded cluster backups paths reused may require to be extended
* Protect PODs and PVCs from being orphaned before deleting an SGCluster
* NullPointerException when checking initialData
* Some annotations for SGConfig generated resources are not included
* Allow restart, minor version upgrade and security upgrade to have primaryInstance and initialInstances set to null
* When recoverying from a volume snapshot the restore procedure do not throw an error when the data folder is not present
* Remove PgBouncer queries from postgres exporter if cluster connection pooling is disabled

## Web Console

* Wrong permissions validation on roles table
* Remove namespace from user creation form

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.10.0/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.10.0)

# :rocket: Release 1.10.0-rc1 (2024-04-22)

## :notepad_spiral: NOTES

StackGres 1.10.0-rc1 is out! :confetti_ball: :champagne: 

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support horizontal and vertical autoscaling
* Support replication initialization from backups
* Support for PersistentVolumeClaim in custom volumes
* Add metadata validation for SGCluster, SGShardedCluster and SGDistributedLogs
* Allow to specify ShardingSphere operator service account in SGShardedCluster
* Include the creationTimestamp in default object storage paths for backups
* Added nodePorts support to PatroniServices and ShardedClusterServices
* Added more metadata fields to REST API DTOs
* Allow to retain WALs for unmanaged lifecycle backups

## Web Console

* Support standby cluster promotion
* Support resource requirements on non production options on cluster, sharded cluster and distributedlogs forms

## :bug: FIXES

* Backup retention was removing WALs of old unmanaged lifecycle backups (breaking them)
* Custom volume mounts model is broken
* Restore sharded backup does not work with PITR
* Sharded backup must make sure the WALs with restore label to be archived
* Set sharded backup completed only if status has been set with backup information
* Sharded cluster backups paths reused may require to be extended
* Protect PODs and PVCs from being orphaned before deleting an SGCluster
* NullPointerException when checking initialData
* Some annotations for SGConfig generated resources are not included
* Allow restart, minor version upgrade and security upgrade to have primaryInstance and initialInstances set to null
* When recoverying from a volume snapshot the restore procedure do not throw an error when the data folder is not present
* Remove PgBouncer queries from postgres exporter if cluster connection pooling is disabled

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.10.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.10.0-rc1)

# :rocket: Release 1.9.0 (2024-03-21)

## :notepad_spiral: NOTES

StackGres 1.9.0 is finally out! This release offers some long-waited features like the possibility to downscale to 0 instances or the support for scaling instances using [HorizontalPodAutoscaler](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) or [KEDA](https://keda.sh/) (more on this on next releases). Also, the new ability to use external DCS (like etcd, ZooKeeper and others) thanks to enabling them for the [patroni initial configurations](https://stackgres.io/doc/1.9/reference/crd/sgcluster/#sgclusterspecconfigurationspatroni). This change open the door to a lot of possibility like having a base for a multi-kubernetes cluster. And last but not least, the new shiny User Management support for our Web Console! :tada: :comet: :rocket: :arrow_up:

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Added PostgreSQL 12.18, 13.14, 14.11, 15.6 and 16.2
* Support PgBouncer 1.22.1
* Support external DCS for patroni
* Allow to set postgresql.pg_ctl_timeout for patroni initial config
* Support for custom volume mounts
* Allow to reduce SGCluster and SGShardedCluster instances to 0
* Support scale subresource

## Web Console

* Support user management
* Support inline backup restoration

## :bug: FIXES

* Removed caBundle placeholder from CRDs conversion webhooks to speed up installation of operator bundle
* ConcurrentModificationException thrown while watching resources
* Avoid patroni to restart due to timeout in postgres script
* Misleading info on managedLifecycle description
* SGCluster do not scale up when primary is latest Pod
* REST API users endpoints do not allow to update and delete omitting the password (to avoid change it when updating or avoid looking it up when deleting)

## Web Console

* Extensions version dropdown won't open on forms
* Include tooltips and minimal template on sgcluster wizard
* Set "Db Info" dashboard as default
* Replace timezone toggle icon
* Use postgres versions store for version filters
* Remove unwanted overflow on pitr graph legend
* Remove "open in new tab" icon from crd tables
* Odd layout on backups search form
* Make compressed size the default on sgbackups table listings
* Elapsed time on backups should be timezone independent
* Set minimum width for timestamp columns

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.9.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.9.0)

# :rocket: Release 1.9.0-rc1 (2024-03-05)

## :notepad_spiral: NOTES

StackGres 1.9.0-rc1 has landed! :confetti_ball: :champagne: 

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Added PostgreSQL 12.18, 13.14, 14.11, 15.6 and 16.2
* Support PgBouncer 1.22.1
* Support external DCS for patroni
* Allow to set postgresql.pg_ctl_timeout for patroni initial config
* Support for custom volume mounts
* Allow to reduce SGCluster and SGShardedCluster instances to 0
* Support scale subresource

## Web Console

* Support inline backup restoration

## :bug: FIXES

* Removed caBundle placeholder from CRDs conversion webhooks to speed up installation of operator bundle
* ConcurrentModificationException thrown while watching resources
* Avoid patroni to restart due to timeout in postgres script
* Misleading info on managedLifecycle description
* SGCluster do not scale up when primary is latest Pod

## Web Console

* Extensions version dropdown won't open on forms
* Include tooltips and minimal template on sgcluster wizard
* Set "Db Info" dashboard as default
* Replace timezone toggle icon
* Use postgres versions store for version filters
* Remove unwanted overflow on pitr graph legend
* Remove "open in new tab" icon from crd tables
* Odd layout on backups search form
* Make compressed size the default on sgbackups table listings
* Elapsed time on backups should be timezone independent
* Set minimum width for timestamp columns

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.9.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.9.0-rc1)

# :rocket: Release 1.8.1 (2024-02-27)

## :notepad_spiral: NOTES

StackGres 1.8.1 has been released! :confetti_ball: :champagne: 

This release aim to fix a couple of critical bugs that could affect your production environment so go on an upgrade! 

## :sparkles: NEW FEATURES AND CHANGES

Nothing new here! :eyes:

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* Recursive chmod on postgres start make pg_ctl fail
* Operator stop working after ConcurrentModificationException

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.8.1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.8.1)

# :rocket: Release 1.8.0 (2024-02-05)

## :notepad_spiral: NOTES

StackGres 1.8.0 has landed!  :confetti_ball: :champagne: :rocket: 

We introduced 2 new sharding technologies to our SGShardedCluster:

* [Apache ShardingSphere](https://shardingsphere.apache.org/) (requires installation of the [ShardingSphere operator](https://shardingsphere.apache.org/oncloud/))
* DDP, an in-house sharding technology based on Postgres partitioning functionality and Postgres Foreign Data Wrapper extension.

This release include also some improvements and bug fixes for SGBackup.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Added support for DDP as a sharding technology based on FDW
* Added support for ShardingSphere as a sharding technology
* Added securityUpgrade to SGShardedDbOps
* Allow to set secretref as source for the admin credentials
* Add can-i for SGConfig in REST API
* Validate SGInstanceProfile resources are not negative
* Changed default initdb auth host to scram-sha-256 and configurable via password_encryption
* Added timeouts to backups and improved logging and errors
* Allow operator bootstrap to be retried

## Web Console

* Support spec.profile for sgdistributedlogs
* Support spec.profile for sgshardedclusters
* Support K8s snapshots for SGCluster backups configuration
* Support K8s snapshots for SGShardedCluster backups configuration
* Support volumeSnapshot spec for SGBackups
* Graphic PITR selection feature on sgclusters creation form

## :bug: FIXES

* REST API ServiceAccount can not be deleted since owned by OLM
* Workaround for operator bundle upgrade where service account stackgres-restapi is being deleted somehow
* Skip SGConfig reconciliation when lock is updated
* Operator reconcile Web Console nginx ConfigMap in an endless loop when webCertName is set
* SGBackup is marked as Completed when the backupInformation is not set
* Relocate binaries fail when cp is interrupted while copying files
* Postgres exporter uses hardcoded username
* Connections panel GAUGE bar not working as expected

## Web Console

* Pod selector not working on cluster monitoring tab
* Replace pod ip with pod's name on sgcluster monitoring url
* Include dashboard selection dropdown on sgshardecluster monitoring tab
* Adjust sidebar namespace selector behavior
* Make sure only one sidebar crd submenu is open at a time
* Add default state to disableclusterpodantiaffinity field on distributed logs form
* Add default state to disableclusterpodantiaffinity field on sharded cluster form
* Show sgbackup tablespaceMap info only when not null
* Move user suppliedd pods sidecars to sidecars steps on sharded cluster form
* Make sharded cluster name on breadcrumbs clickable
* Collapse repeaters on cluster form
* Add classnames to support tests on cluster form
* Collapse repeaters on shardedcluster form
* Hide empty specs on sharded cluster summary
* Extensions version dropdown won't open on forms
* Include tooltips and minimal template on sgcluster wizard
* Set "Db Info" dashboard as default
* Replace timezone toggle icon
* Use postgres versions store for version filters
* Remove unwanted overflow on PITR graph legend
* Remove "open in new tab" icon from crd tables
* Support inline backup restoration
* Odd layout on backups search form
* Make compressed size the default on sgbackups table listings
* Misleading info on managedLifecycle description
* Elapsed time on backups should be timezone independent
* Set minimum width for timestamp columns

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.8.0/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.8.0)

# :rocket: Release 1.8.0-rc1 (2024-01-29)

## :notepad_spiral: NOTES

StackGres 1.8.0-rc1 is out!  :confetti_ball: :champagne: 

We introduced 2 new sharding technologies to our SGShardedCluster:

* [Apache ShardingSphere](https://shardingsphere.apache.org/) (requires installation of the [ShardingSphere operator](https://shardingsphere.apache.org/oncloud/))
* DDP, an in-house sharding technology based on Postgres partitioning functionality and Postgres Foreign Data Wrapper extension.

This release include also some improvements and bug fixes for SGBackup.

So, what you are waiting for to try this release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Added support for DDP as a sharding technology based on FDW
* Added support for ShardingSphere as a sharding technology
* Added securityUpgrade to SGShardedDbOps
* Allow to set secretref as source for the admin credentials
* Add can-i for SGConfig in REST API
* Validate SGInstanceProfile resources are not negative
* Changed default initdb auth host to scram-sha-256 and configurable via password_encryption
* Added timeouts to backups and improved logging and errors

## Web Console

* Support spec.profile for sgdistributedlogs
* Support spec.profile for sgshardedclusters
* Support K8s snapshots for SGCluster backups configuration
* Support K8s snapshots for SGShardedCluster backups configuration
* Support volumeSnapshot spec for SGBackups
* Graphic PITR selection feature on sgclusters creation form

## :bug: FIXES

* REST API ServiceAccount can not be deleted since owned by OLM
* Workaround for operator bundle upgrade where service account stackgres-restapi is being deleted somehow
* Skip SGConfig reconciliation when lock is updated
* Operator reconcile Web Console nginx ConfigMap in an endless loop when webCertName is set
* SGBackup is marked as Completed when the backupInformation is not set
* Relocate binaries fail when cp is interrupted while copying files
* Postgres exporter uses hardcoded username
* Connections panel GAUGE bar not working as expected

## Web Console

* Pod selector not working on cluster monitoring tab
* Replace pod ip with pod's name on sgcluster monitoring url
* Include dashboard selection dropdown on sgshardecluster monitoring tab
* Adjust sidebar namespace selector behavior
* Make sure only one sidebar crd submenu is open at a time
* Add default state to disableclusterpodantiaffinity field on distributed logs form
* Add default state to disableclusterpodantiaffinity field on sharded cluster form
* Show sgbackup tablespaceMap info only when not null
* Move user suppliedd pods sidecars to sidecars steps on sharded cluster form
* Make sharded cluster name on breadcrumbs clickable
* Collapse repeaters on cluster form
* Add classnames to support tests on cluster form
* Collapse repeaters on shardedcluster form
* Hide empty specs on sharded cluster summary

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.8.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.8.0-rc1)

# :rocket: Release 1.7.0 (2023-12-22)

## :notepad_spiral: NOTES

StackGres 1.7.0-rc1 has landed! Prepare to see how fast can be your backups using VolumeSnapshot support. :confetti_ball: :champagne: :runner: :santa: 

Finally the ability to overwrite Patroni dynamic configuration that allows to control better how failover behaves and when it is triggered.

Also a lot of bugfixes and small improvements, what you are waiting to upgrade StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.29
* Support OpenShift 4.14
* Support for backup with VolumeSnapshots
* Allow to overwrite patroni dynamic configuration
* Order extensions returned by REST API using build and version
* Added REST API endpoint to return results from named queries for an SGCluster
* Added REST API endpoint to list postgres versions for an existing SGCluster

## Web Console

* Support to manage SGConfig
* Added Wizard for SGCluster creation

## :bug: FIXES

* PgBouncer configuration is not reloaded
* Can not create SGShardedCluster for Postgres 13
* SGScript status is corrupted for SGShardedCluster
* SGConfig was missing spec.jobs.serviceAccount section and some field where not used in spec.cert
* Changes were detected when no change were present in applied required resources
* SGShardedBackups do not start if cronSchedule is not set
* Backup and restore secret are not correctly updated
* SGShardedCluster do not propagate usernames configured in credentials
* Operator bundle installation do not create the ClusterRoleBinding correctly for REST API

## Web Console

* Add link to redirect shard to specific configuration
* Prevent backup path from being erased on focus out
* Allow custom containers and custom init containters edition on sharded cluster form
* Allow custom containers and custom init containters edition on cluster form
* Fix credentials warning position

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.7.0/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.7.0)

# :rocket: Release 1.7.0-rc1 (2023-12-20)

## :notepad_spiral: NOTES

StackGres 1.7.0-rc1 has landed! Prepare to see how fast can be your backups using VolumeSnapshot support. :confetti_ball: :champagne: :runner: 

Finally the ability to overwrite Patroni dynamic configuration that allows to control better how failover behaves and when it is triggered.

Also a lot of bugfixes and small improvements, what you are waiting to upgrade StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.29
* Support OpenShift 4.14
* Support for backup with VolumeSnapshots
* Allow to overwrite patroni dynamic configuration
* Order extensions returned by REST API using build and version
* Added REST API endpoint to return results from named queries for an SGCluster
* Added REST API endpoint to list postgres versions for an existing SGCluster

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* PgBouncer configuration is not reloaded
* Can not create SGShardedCluster for Postgres 13
* SGScript status is corrupted for SGShardedCluster
* SGConfig was missing spec.jobs.serviceAccount section and some field where not used in spec.cert
* Changes were detected when no change were present in applied required resources
* SGShardedBackups do not start if cronSchedule is not set
* Backup and restore secret are not correctly updated
* SGShardedCluster do not propagate usernames configured in credentials
* Native image require fixing CRD schema before updating

## Web Console

* Add link to redirect shard to specific configuration
* Prevent backup path from being erased on focus out
* Allow custom containers and custom init containters edition on sharded cluster form
* Allow custom containers and custom init containters edition on cluster form
* Fix credentials warning position

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.7.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.7.0-rc1)

# :rocket: Release 1.6.1 (2023-12-04)

## :notepad_spiral: NOTES

StackGres 1.6.1 is here to fix some undesirable bugs! :confetti_ball: :champagne: :bug: :gun: 

Everybody is enconuraged to upgrade from 1.6.0 ASAP!

## :sparkles: NEW FEATURES AND CHANGES

Nothing new here! :eyes:

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

* SGDbOps for security upgrade delete all the pods first
* Pod are removed by the operator during restart in an uncontrolled way
* Operator fail when OwnerReferencesPermissionEnforcement is enabled
* Major version upgrade fails if ssl is enabled
* USE_ARBITRARY_USERS not set to true for OpenShift operator bundle
* Added missing SGBackupConfig.yaml for operator bundle
* Remove conversion webhook from SGConfig for operator bundle
* Extensions link folder not created
* OIDC application-type set to 'true'

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.6.1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.6.1)

# :rocket: Release 1.6.0 (2023-11-13)

## :notepad_spiral: NOTES

StackGres 1.6.0 is out! :confetti_ball: :champagne: 

With great pleasure and apologizing for the unexpected long wait, we are proud of this release that brings among
 other features stability and a much better installation experience thanks to the ability of the operator to
 reconfigure dynamically without the need of helm, by just modifying the new SGConfig CRD.

Pods resources configuration is quite hard and our approach was not very neat. So we decided to change it by making
 easy to know the total amount of CPU and memory that will be requested by the Pod using `SGInstanceProfile.spec.requests.cpu`
 and `SGInstanceProfile.spec.requests.memory` as the total where other sidecars will subtract their respective amounts in order
 to leave the rest for the postgres (patroni) container. Existing resources will still use the previous method to calculate the
 requests (we provided the flag `SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal` if you still want to use
 such method). Limits will still works as expected with `SGInstanceProfile.spec.cpu` and `SGInstanceProfile.spec.memory`
 targeting the patroni container only.

We could not leave the sharded clusters without backups and 2 day operation so we created a bunch of them in order to deal with
 restarts, resharding and be able to restore from a backup.

And this is yet not over do not wait any more and try out this new shining release of StackGres to find out many other new features,
 stability improvements and bug fixes! 

## :sparkles: NEW FEATURES AND CHANGES

* Added PostgreSQL 16.1, 15.5, 14.10, 13.13, 12.17
* Update Patroni to 3.2.0
* Update PgBouncer to 1.21.0
* Support for Kubernetes 1.28
* Support for OpenShift 4.13
* Updated all base images
* Dynamic operator configuration with SGConfig CRD
* Support for sharded backups with SGShardedBackup
* Support for sharded dbops with SGShardedDbOps for restart and resharding operations
* Support for Service Binding
* Support for cluster profile and new calculation for patroni resources
* Improve reconciliation cycle by avoiding rely on custom comparators to detect changes
* Move owner reference decorator as part of the reconciliation logic
* Replaced helm operator with the operator for the operator bundle
* Added skipRange lower bound for operator bundle
* Support only amd64 and arm64 archs for operator bundle
* Avoid check extensions index when extensions did not change
* Improve log output for fluent-bit container
* Make sure JVM and native images set a limit when only requests is set
* Added missing fields to the demo cluster helm chart
* Added dryRun parameter to all create, update and delete endpoints in StackGres REST API
* Added endpoint to create namespaces in StackGres REST API
* Added user management endpoints in StackGres REST API
* Added Grafana dashboards per section with new queries and reduced cardinality of some metrics
* Improvements for majorVersionUpgrade SGDbOps process
* Allow SGShardedCluster to change citus version
* Added sgpostgresconfig(s) as shortNames for SGPostgresConfig
* Added sgpoolingconfig(s) as shortNames for SGPoolingConfig
* Always set latest as the version of resources that can not be upgraded with SGDbOps

## Web Console

* Support for SGShardedCluster overrides
* Upgrade VueJS to version 2.7.14

## :bug: FIXES

* Major version upgrade fail to show logs and do not complete rollback on error
* create-backup.sh script fail to update message on error
* Add "streaming", "in archive recovery" as MemberState.RUNNING used by patroni since 3.0.4
* Added check for custom resources with too old versions
* PgBouncer configuration not reloaded
* Some prometheus queries are broken
* Container postgres-util is not removed when disabled
* Init container pgbouncer-auth-file is not removed when disableConnectionPooling is set
* Sharded cluster REST API does not load or store scripts for overrides
* Versioned core and contrib extensions are not allowed to change version and unversioned pick random version
* Skip check on resources that do not require a SGDbOps to upgrade if version less or equals to 1.5

## Web Console

* Disable editing/cloning/deleting coordinator and shards clusters from a SGShardedCluster
* Improve Distributed Logs location on SGCluster form
* Operator fails to edit a cluster created from a backup (again)
* Move Custom sidecars section (pods)  to the existing Sidecars section
* Allow users to revert non-required dropdown selection
* Prevent users from selecting label on timeout selects
* Reuse not found component
* Avoid overwriting syncInstances value when already set
* Prevent setting undefined configurations on sgshardedclusters
* Wrong path on sharded cluster breadcrumbs
* Wrong init of replication mode on sgshardedcluster edition
* Ensure proper storageClass init for coordinator and shards on sgshardecluster edition

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.6.0/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.6.0)

# :rocket: Release 1.6.0-rc2 (2023-11-07)

## :notepad_spiral: NOTES

StackGres 1.6.0-rc2 is out! :confetti_ball: :champagne: 

This release fix a couple of critical bugs we found in 1.6.0-rc1 that affect the correct functioning of StackGres operator, please upgrade ASAP for a safer experience.

## :sparkles: NEW FEATURES AND CHANGES

Nothing new here! :eyes:

## Web Console

* Support for creating namespaces if the user has permissions to do so

## :bug: FIXES

* Operator fail to upgrade to 1.6.0-rc1 due to errors updating the CRDs 
* SGDbOps Jobs fails due to missing permission

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.6.0-rc2/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.6.0-rc2)

# :rocket: Release 1.6.0-rc1 (2023-10-31)

## :notepad_spiral: NOTES

StackGres 1.6.0-rc1 is out! :confetti_ball: :champagne: :ghost: 

With great pleasure and apologizing for the unexpected long wait, we are proud of this release that brings among
 other features stability and a much better installation experience thanks to the ability of the operator to
 reconfigure dynamically without the need of helm, by just modifying the new SGConfig CRD.

Pods resources configuration is quite hard and our approach was not very neat. So we decided to change it by making
 easy to know the total amount of CPU and memory that will be requested by the Pod using `SGInstanceProfile.spec.requests.cpu`
 and `SGInstanceProfile.spec.requests.memory` as the total where other sidecars will subtract their respective amounts in order
 to leave the rest for the postgres (patroni) container. Existing resources will still use the previous method to calculate the
 requests (we provided the flag `SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal` if you still want to use
 such method). Limits will still works as expected with `SGInstanceProfile.spec.cpu` and `SGInstanceProfile.spec.memory`
 targeting the patroni container only.

We could not leave the sharded clusters without backups and 2 day operation so we created a bunch of them in order to deal with
 restarts, resharding and be able to restore from a backup.

And this is yet not over do not wait any more and try out this new shining release of StackGres to find out many other new features,
 stability improvements and bug fixes! 

## :sparkles: NEW FEATURES AND CHANGES

* Added PostgreSQL 16.0
* Update Patroni to 3.2.0
* Update PgBouncer to 1.21.0
* Support for Kubernetes 1.28
* Support for OpenShift 4.13
* Updated all base images
* Dynamic operator configuration with SGConfig CRD
* Support for sharded backups with SGShardedBackup
* Support for sharded dbops with SGShardedDbOps for restart and resharding operations
* Support for Service Binding
* Support for cluster profile and new calculation for patroni resources
* Improve reconciliation cycle by avoiding rely on custom comparators to detect changes
* Move owner reference decorator as part of the reconciliation logic
* Replaced helm operator with the operator for the operator bundle
* Added skipRange lower bound for operator bundle
* Support only amd64 and arm64 archs for operator bundle
* Avoid check extensions index when extensions did not change
* Improve log output for fluent-bit container
* Make sure JVM and native images set a limit when only requests is set
* Added missing fields to the demo cluster helm chart
* Added dryRun parameter to all create, update and delete endpoints in StackGres REST API
* Added endpoint to create namespaces in StackGres REST API
* Added user management endpoints in StackGres REST API
* Added Grafana dashboards per section with new queries and reduced cardinality of some metrics
* Improvements for majorVersionUpgrade SGDbOps process
* Allow SGShardedCluster to change citus version

## Web Console

* Support for SGShardedCluster overrides
* Upgrade VueJS to version 2.7.14

## :bug: FIXES

* Major version upgrade fail to show logs and do not complete rollback on error
* create-backup.sh script fail to update message on error
* Add "streaming", "in archive recovery" as MemberState.RUNNING used by patroni since 3.0.4
* Added check for custom resources with too old versions
* PgBouncer configuration not reloaded
* Some prometheus queries are broken
* Container postgres-util is not removed when disabled
* Init container pgbouncer-auth-file is not removed when disableConnectionPooling is set
* Sharded cluster REST API does not load or store scripts for overrides
* Versioned core and contrib extensions are not allowed to change version and unversioned pick random version

## Web Console

* Disable editing/cloning/deleting coordinator and shards clusters from a SGShardedCluster
* Improve Distributed Logs location on SGCluster form
* Operator fails to edit a cluster created from a backup (again)
* Move Custom sidecars section (pods)  to the existing Sidecars section
* Allow users to revert non-required dropdown selection
* Prevent users from selecting label on timeout selects
* Reuse not found component
* Avoid overwriting syncInstances value when already set
* Prevent setting undefined configurations on sgshardedclusters
* Wrong path on sharded cluster breadcrumbs
* Wrong init of replication mode on sgshardedcluster edition
* Ensure proper storageClass init for coordinator and shards on sgshardecluster edition

## :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.6.0-rc1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.6.0-rc1)

# :rocket: StackGres 1.5.0 GA (2023-07-03)

The Postgres You Love :heart_eyes:, At Any Scale :sparkles:

We are thrilled to announce the release of StackGres 1.5.0! This update brings a host of new features, enhancements, and optimizations, further solidifying StackGres as the go-to solution for running your mission-critical Postgres workloads on Kubernetes. Let's dive into the exciting highlights of this release:

1. :fire: Sharding Cluster Support:

    StackGres 1.5.0 introduces seamless integration with Citus and Patroni, empowering you to scale your Postgres database horizontally. Now you can effortlessly shard your clusters, distributing data across multiple nodes for improved performance, scalability, and fault tolerance.

    Citus uses a technique called sharding to divide your data into smaller, manageable pieces called shards. Each shard contains a subset of your data and is stored on a separate node within the cluster. Sharding allows for parallel processing of queries and efficient data storage across multiple machines.
    When you execute a query, Citus intelligently routes the query to the relevant shards. It analyzes the query and determines which shards hold the necessary data to process the request. By distributing the workload across multiple nodes, Citus enables parallel query execution, leading to faster query performance.

    Citus provides a scalable and distributed database solution by leveraging PostgreSQL's powerful extensibility. It enables you to handle large datasets, process complex queries, and achieve high performance by harnessing the capabilities of a distributed cluster architecture, as you can add more nodes to your cluster to accommodate increasing data volumes or user traffic.

    A new CRD called `SGShardedCluster` will bring you the power of sharding by creating multiple SGClusters that will behave like a single one. This also features an automated SSL/TLS support by default on sharded clusters.

    StackGres provides with the `SGShardedCluster` a fault tolerance HA solution on each shard (as well as the coordinators) so that any failed node will be automatically recovered as if one node goes down, the other nodes can still continue serving your application.

    The Web Console has also been updated with first-class support for this feature, try it out :wink:.

    | :warning: WARNING          |
    |:---------------------------|
    | The feature is still in alpha so you might expect breaking changes in the CRD, if you encounter any issue please report it and we will provide a fix. Also, 2 day operations are currently not supported and will be implemented in 1.6 release. |

2. :elephant: Updated Postgres and components:

    In this release, we have updated the Postgres engine to the latest minor versions supported up to 15.3, 14.8, and 13.11, incorporating the latest advancements and bug fixes from the PostgreSQL community. This upgrade brings improved performance, enhanced security features, and better compatibility with the latest Postgres ecosystem. Also, we have updated Babelfish for PostgreSQL to the latest release 2.3.0 so you can continue experimenting with this SQL Server alternative with expanded language support, improved compatibility and bug fixes and stability improvements.

3. :anchor: Compatibility with Kubernetes 1.27 and 1.26:

    StackGres is now fully compatible with Kubernetes versions 1.27 and 1.26. This means you can confidently deploy and manage StackGres in Kubernetes clusters running these specific versions, taking advantage of their features and improvements.

4. :arrows_counterclockwise: Transition from Docker.io to Quay.io Container Registry:

     Due to Docker.io imposing lower download rate-limits, we are transitioning from Docker.io to Quay.io as our preferred container registry.

5. :bug: Bug Fixes and Stability Improvements:

    As part of our commitment to delivering a reliable and stable database solution, we have addressed reported issues and incorporated bug fixes in StackGres 1.5.0. The update offers enhanced stability, ensuring a smooth and uninterrupted experience for your critical workloads.

### Call to action

We encourage all users to upgrade to StackGres 1.5.0 to take advantage of these exciting new features and enhancements. We value your feedback and suggestions, so please don't hesitate to reach out to our dedicated support team with any questions or comments. Join us on our [Slack](https://slack.stackgres.io/)/[Discord](https://discord.stackgres.io/) to share any comments or to get help from [StackGres Community](https://stackgres.io/community/). 

Thank you for choosing StackGres as your trusted Postgres database stack. We remain dedicated to providing top-notch performance, security, and scalability for your data needs.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

Enjoy the power of StackGres 1.5.0!

The StackGres Team

## :up: Upgrade

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) Helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.5.0/helm/stackgres-operator.tgz`

#### :exclamation: IMPORTANT
After each operator upgrade, remember to perform a security upgrade SGDbOps on each existing SGCluster after the operator has been upgraded.

Example:
```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  name: secupgr
spec:
  sgCluster: my-cluster
  op: securityUpgrade
  maxRetries: 1
  securityUpgrade:
    method: InPlace
```

> :warning: NOTE: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

### :construction: Known issues

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :clipboard: List of features and changes

### :sparkles: NEW FEATURES AND CHANGES

* Added PostgreSQL 15.3, 15.2, 14.8, 14.7, 13.11, 13.10, 12.15, 12.14
* Added Babelfish for PostgreSQL 14.6, 13.9
* Added Patroni 3.0.2
* Support for Kubernetes 1.27 and 1.26
* Added SGShardedCluster CRD
* Support to specify Patroni initial config
* Support to set labels for Services (and Endpoints)
* Support to set much more services parameters for Services
* New sync-all and strict-sync-all to adjust the number of synchronous nodes to all replicas
* Shift from docker.io to quay.io
* Improved operator startup
* Improved cluster startup
* Enhanced cluster stats endpoint to fallback for cgroup v1 and v2
* Make CronJob generator for v1 or v1beta1 api versions dependent on Kubernetes version
* Enable SSL by default on SGShardedCluster
* Generate SSL certificate and private key if only enabled is specified
* Change ssl configuration at run time
* Use job backoffLimit to implements SGDbOps maxRetries
* Improve current PgBouncer variable check query
* Support for self signed certificate when using S3 compatible storage
* Added priority class support
* Changed Operator Bundle installation name to `stackgres-operator`
* Cleanup CSR in operator helm chart
* Return username key for cluster and sharded cluster info in REST API
* Added sharded cluster stats REST API endpoints
* Section sgshardedcluster.status.clusters returned by REST API the ordered names of the sgclusters that will be created
* Updated base images for builds and release images

#### Web Console

* Support for Sharded Clusters
* Support user-supplied sidecars for SGCluster services
* Add loadBalancerIP to SGCluster and SGDistributedLogs
* Support user-supplied sidecars for pods customVolumes, customInitContainers and customContainers
* Enable SSL specs
* Implemented cascading replication with WAL shipping
* Implemented cascade replication from an external instance
* Implemented cascade replication from a local sgcluster
* Improve performance on REST API requests
* Replace crd details table layouts with summary tree structure

### :bug: FIXES

* ClusterBootstrapCompleted sent every few seconds
* Empty backupPath for majorVersionUpgrade SGDbOps make the operation fail
* Repeating restart ReducedImpact with 1 instance fails
* Major version upgrade must never change extension version
* Patroni do not clean up history after converting a cluster to standby cluster
* Failed SGScript without retryOnError do not re-execute if version is changed
* SGScript retry on error do not respect the backoff
* Deleting a resource a SGDistributedLogs depends on should not be possible
* pgbench SGDbOps fail if scale contains point
* Helm Controller Manager selectors are too generic
* Duplicate restartPolicy lines in the test operator template
* REST API can-i do not return permissions for SGShardedCluster
* REST API model is exposing parameters not present in the sharded cluster CRD
* sgcluster REST API returns all pods for a sharded cluster
* restart SGDbOps does not work on SGCluster when patroni scope is set
* when SSL enable causes boostrap to fail and breaks synchronous replication
* Sharded cluster primary service and shards service can not be set to NodePort or LoadBalancer
* PgBouncer queries for postgres exporter are failing
* Command `readlink /proc/$$/exe` may fail
* During security upgrade cluster Pods are not found

#### Web Console

* Edit SGPostgresConfig form should only list custom parameters
* Remove Advanced switch from Azure section
* Summaries should not open unless all required fields are filled in
* Backup scheduling inserting trailing zero in cron job minutes config
* Script source not cleared when deleting a script
* notValid class not being removed on Babelfish Experimental Feature
* Unify click behavious when clicked on a switch and on its label
* Cluster name on SGDbOp details is not clickable
* Sidebar items hidden behind dialog popups
* PITR date and time picker not working
* Continue on SGScripts Error should not be visible if there are no Scripts set
* Fix Extensions table layout
* View Script button text and icon on Cluster Configuration tab have different behaviours
* Hide empty sections on summaries
* Disable Connection Pooling not working properly on Cluster Form
* Object Storage selector on Cluster form shows all Object Storages from all Namespaces
* Make sure crontab is shown on preferred timezone
* Script content not shown on summary when set from a ConfigMap
* Show secretKeySelectors for GCS service account json on edit mode
* Review interceptors to REST API responses
* SGBackups list won't load when start time is not present on a backup
* Allow empty Backup path generate errors after update
* Sets wrong path for SGCluster backup config
* Edit SGPoolingConfig form should only list custom parameters
* Replace instances dropdown with numeric input on SGCluster form
* Service Account JSON not shown on Summary
* Error when updating an SGCluster with a ConfigMap in managed SQL section
* Cluster Summary opens and displays empty Custom Port properties
* Only show form when ready to be edited

### :construction: KNOWN ISSUES

* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

### :twisted_rightwards_arrows: [Full list of commits](https://gitlab.com/ongresinc/stackgres/-/commits/1.5.0)

# :rocket: Release 1.5.0-rc2 (28-06-2023)

## :notepad_spiral: NOTES

StackGres 1.5.0-rc2  open the door to horizontal scaling by leveraging [Citus support from Patroni](https://patroni.readthedocs.io/en/master/citus.html).

A new CRD called SGShardedCluster will bring you the power of sharding by creating multiple SGClusters that will behave like a single one.
 The feature is still in alpha so you might expect breaking changes in the CRD.

## :sparkles: NEW FEATURES AND CHANGES

* Added PostgreSQL 15.3, 15.2, 14.8, 14.7, 13.11, 13.10, 12.15, 12.14
* Added Babelfish for PostgreSQL 14.6, 13.9
* Added Patroni 3.0.2
* Support for Kubernetes 1.27 and 1.26
* Added SGShardedCluster CRD
* Support to specify Patroni initial config
* Support to set labels for Services (and Endpoints)
* Support to set much more services parameters for Services
* New sync-all and strict-sync-all to adjust the number of synchronous nodes to all replicas
* Shift from docker.io to quay.io
* Improved operator startup
* Improved cluster startup
* Enhanced cluster stats endpoint to fallback for cgroup v1 and v2
* Make CronJob generator for v1 or v1beta1 api versions dependent on Kubernetes version
* Enable SSL by default on SGShardedCluster
* Generate SSL certificate and private key if only enabled is specified
* Change ssl configuration at run time
* Use job backoffLimit to implements SGDbOps maxRetries
* Improve current PgBouncer variable check query
* Support for self signed certificate when using S3 compatible storage
* Added priority class support
* Changed Operator Bundle installation name to `stackgres-operator`
* Cleanup CSR in operator helm chart
* Return username key for cluster and sharded cluster info in REST API
* Added sharded cluster stats REST API endpoints
* Section sgshardedcluster.status.clusters returned by REST API the ordered names of the sgclusters that will be created
* Updated base images for builds and release images

## Web Console

* Support for Sharded Clusters
* Support user-supplied sidecars for SGCluster services
* Add loadBalancerIP to SGCluster and SGDistributedLogs
* Support user-supplied sidecars for pods customVolumes, customInitContainers and customContainers
* Enable SSL specs
* Implemented cascading replication with WAL shipping
* Implemented cascade replication from an external instance
* Implemented cascade replication from a local sgcluster
* Improve performance on REST API requests
* Replace crd details table layouts with summary tree structure

## :bug: FIXES

* ClusterBootstrapCompleted sent every few seconds
* Empty backupPath for majorVersionUpgrade SGDbOps make the operation fail
* Repeating restart ReducedImpact with 1 instance fails
* Major version upgrade must never change extension version
* Patroni do not clean up history after converting a cluster to standby cluster
* Failed SGScript without retryOnError do not re-execute if version is changed
* SGScript retry on error do not respect the backoff
* Deleting a resource a SGDistributedLogs depends on should not be possible
* pgbench SGDbOps fail if scale contains point
* Helm Controller Manager selectors are too generic
* Duplicate restartPolicy lines in the test operator template
* REST API can-i do not return permissions for SGShardedCluster
* REST API model is exposing parameters not present in the sharded cluster CRD
* sgcluster REST API returns all pods for a sharded cluster
* restart SGDbOps does not work on SGCluster when patroni scope is set
* when SSL enable causes boostrap to fail and breaks synchronous replication
* Sharded cluster primary service and shards service can not be set to NodePort or LoadBalancer
* PgBouncer queries for postgres exporter are failing
* Command `readlink /proc/$$/exe` may fail
* During security upgrade cluster Pods are not found

## Web Console

* Edit SGPostgresConfig form should only list custom parameters
* Remove Advanced switch from Azure section
* Summaries should not open unless all required fields are filled in
* Backup scheduling inserting trailing zero in cron job minutes config
* Script source not cleared when deleting a script
* notValid class not being removed on Babelfish Experimental Feature
* Unify click behavious when clicked on a switch and on its label
* Cluster name on SGDbOp details is not clickable
* Sidebar items hidden behind dialog popups
* PITR date and time picker not working
* Continue on SGScripts Error should not be visible if there are no Scripts set
* Fix Extensions table layout
* View Script button text and icon on Cluster Configuration tab have different behaviours
* Hide empty sections on summaries
* Disable Connection Pooling not working properly on Cluster Form
* Object Storage selector on Cluster form shows all Object Storages from all Namespaces
* Make sure crontab is shown on preferred timezone
* Script content not shown on summary when set from a ConfigMap
* Show secretKeySelectors for GCS service account json on edit mode
* Review interceptors to REST API responses
* SGBackups list won't load when start time is not present on a backup
* Allow empty Backup path generate errors after update
* Sets wrong path for SGCluster backup config
* Edit SGPoolingConfig form should only list custom parameters
* Replace instances dropdown with numeric input on SGCluster form
* Service Account JSON not shown on Summary
* Error when updating an SGCluster with a ConfigMap in managed SQL section
* Cluster Summary opens and displays empty Custom Port properties
* Only show form when ready to be edited

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.5.0-rc2/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.5.0-rc2)

# :rocket: Release 1.5.0-RC1 (2023-06-07)

## :notepad_spiral: NOTES

StackGres 1.5.0-RC1 open the door to horizontal scaling by leveraging [Citus support from Patroni](https://patroni.readthedocs.io/en/master/citus.html).

A new CRD called SGShardedCluster will bring you the power of sharding by creating multiple SGClusters that will behave like a single one.
 The feature is still in alpha so you might expect breaking changes in the CRD.

## :sparkles: NEW FEATURES AND CHANGES

* Added PostgreSQL 15.3, 15.2, 14.8, 14.7, 13.11, 13.10, 12.15, 12.14
* Added Patroni 3.0.1, 3.0.2
* Support for Kubernetes 1.27 and 1.26
* Added SGShardedCluster CRD
* Support to specify Patroni initial config
* Support to set labels for Services (and Endpoints)
* Support to set much more services parameters for Services
* New sync-all and strict-sync-all to adjust the number of synchronous nodes to all replicas
* Shift from docker.io to quay.io
* Improved operator startup
* Improved cluster startup
* Enhanced cluster stats endpoint to fallback for cgroup v1 and v2
* Make CronJob generator for v1 or v1beta1 api versions dependent on Kubernetes version
* Enable SSL by default on SGShardedCluster
* Generate SSL certificate and private key if only enabled is specified
* Change ssl configuration at run time
* Use job backoffLimit to implements SGDbOps maxRetries
* Improve current PgBouncer variable check query
* Support for self signed certificate when using S3 compatible storage
* Added priority class support
* Changed Operator Bundle installation name to `stackgres-operator`
* Cleanup CSR in operator helm chart

## Web Console

* Support user-supplied sidecars for SGCluster services
* Add loadBalancerIP to SGCluster and SGDistributedLogs
* Support user-supplied sidecars for pods customVolumes, customInitContainers and customContainers
* Enable SSL specs
* Implemented cascading replication with WAL shipping
* Implemented cascade replication from an external instance
* Implemented cascade replication from a local sgcluster

## :bug: FIXES

* ClusterBootstrapCompleted sent every few seconds
* Empty backupPath for majorVersionUpgrade SGDbOps make the operation fail
* Repeating restart ReducedImpact with 1 instance fails
* Major version upgrade must never change extension version
* Patroni do not clean up history after converting a cluster to standby cluster
* Failed SGScript without retryOnError do not re-execute if version is changed
* SGScript retry on error do not respect the backoff
* Deleting a resource a SGDistributedLogs depends on should not be possible
* pgbench SGDbOps fail if scale contains point
* Helm Controller Manager selectors are too generic
* Duplicate restartPolicy lines in the test operator template
* REST API can-i do not return permissions for SGShardedCluster
* REST API model is exposing parameters not present in the sharded cluster CRD

## Web Console

* Edit SGPostgresConfig form should only list custom parameters
* Remove Advanced switch from Azure section
* Summaries should not open unless all required fields are filled in
* Backup scheduling inserting trailing zero in cron job minutes config
* Script source not cleared when deleting a script
* notValid class not being removed on Babelfish Experimental Feature
* Unify click behavious when clicked on a switch and on its label
* Cluster name on SGDbOp details is not clickable
* Sidebar items hidden behind dialog popups
* PITR date and time picker not working
* Continue on SGScripts Error should not be visible if there are no Scripts set
* Fix Extensions table layout
* View Script button text and icon on Cluster Configuration tab have different behaviours
* Hide empty sections on summaries
* Disable Connection Pooling not working properly on Cluster Form
* Object Storage selector on Cluster form shows all Object Storages from all Namespaces
* Make sure crontab is shown on preferred timezone
* Script content not shown on summary when set from a ConfigMap
* Show secretKeySelectors for GCS service account json on edit mode
* Review interceptors to REST API responses
* SGBackups list won't load when start time is not present on a backup
* Allow empty Backup path generate errors after update
* Sets wrong path for SGCluster backup config
* Edit SGPoolingConfig form should only list custom parameters
* Replace instances dropdown with numeric input on SGCluster form
* Service Account JSON not shown on Summary
* Error when updating an SGCluster with a ConfigMap in managed SQL section
* Cluster Summary opens and displays empty Custom Port properties

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.5.0-RC1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.5.0-RC1)

# :rocket: Release 1.5.0-beta2 (2023-05-17)

## :notepad_spiral: NOTES

StackGres 1.5.0-beta2 open the door to horizontal scaling by leveraging [Citus support from Patroni](https://patroni.readthedocs.io/en/master/citus.html).

A new CRD called SGShardedCluster will bring you the power of sharding by creating multiple SGCluster that will behave like a single one.
 The feature is still in alpha so you might expect braking changes in the CRD.

## :sparkles: NEW FEATURES AND CHANGES

* Patroni 3.0.1
* Postgres 15.2, 14.7, 13.10 and 12.14
* Added SGShardedCluster CRD
* Support to specify Patroni initial config
* Support to set labels for Services (and Endpoints)
* Support to set much more services parameters for Services
* New sync-all and strict-sync-all to adjust the number of synchronous nodes to all replicas
* Shift from docker.io to quay.io
* Improved operator startup
* Improved cluster startup
* Enhanced cluster stats endpoint to fallback for cgroup v1 and v2
* Make CronJob generator for v1 or v1beta1 api versions dependent on Kubernetes version

## Web Console

* Support user-supplied sidecars for SGCluster services
* Add loadBalancerIP to SGCluster and SGDistributedLogs
* Support user-supplied sidecars for pods customVolumes, customInitContainers and customContainers

## :bug: FIXES

* ClusterBootstrapCompleted sent every few seconds
* Empty backupPath for majorVersionUpgrade SGDbOps make the operation fail
* Enable SSL specs
* Implemented cascading replication with WAL shipping
* Implemented cascade replication from an external instance
* Implemented cascade replication from a local sgcluster

## Web Console

* Edit SGPostgresConfig form should only list custom parameters
* Remove Advanced switch from Azure section
* Summaries should not open unless all required fields are filled in
* Backup scheduling inserting trailing zero in cron job minutes config
* Script source not cleared when deleting a script
* notValid class not being removed on Babelfish Experimental Feature
* Unify click behavious when clicked on a switch and on its label
* Cluster name on SGDbOp details is not clickable
* Sidebar items hidden behind dialog popups
* PITR date and time picker not working
* Continue on SGScripts Error should not be visible if there are no Scripts set
* Fix Extensions table layout
* View Script button text and icon on Cluster Configuration tab have different behaviours
* Hide empty sections on summaries
* Disable Connection Pooling not working properly on Cluster Form
* Object Storage selector on Cluster form shows all Object Storages from all Namespaces
* Make sure crontab is shown on preferred timezone
* Script content not shown on summary when set from a ConfigMap
* Show secretKeySelectors for GCS service account json on edit mode
* Review interceptors to REST API responses
* SGBackups list won't load when start time is not present on a backup
* Allow empty Backup path generate errors after update
* Sets wrong path for SGCluster backup config
* Edit SGPoolingConfig form should only list custom parameters
* Replace instances dropdown with numeric input on SGCluster form
* Service Account JSON not shown on Summary
* Error when updating an SGCluster with a ConfigMap in managed SQL section
* Cluster Summary opens and displays empty Custom Port properties

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

Alpha or beta version should not be used to upgrade. Please wait for a release candidate or general availability version.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.5.0-beta2)

# :rocket: Release 1.5.0-beta1 (2023-03-27)

## :notepad_spiral: NOTES

StackGres 1.5.0-beta1 open the door to horizontal scaling by leveraging [Citus support from Patroni](https://patroni.readthedocs.io/en/master/citus.html).

A new CRD called SGShardedCluster will bring you the power of sharding by creating multiple SGCluster that will behave like a single one.
 The feature is still in alpha so you might expect braking changes in the CRD.

## :sparkles: NEW FEATURES AND CHANGES

* Patroni 3.0.1
* Postgres 15.2, 14.7, 13.10 and 12.14
* Added SGShardedCluster CRD
* Support to specify Patroni initial config
* Support to set labels for Services (and Endpoints)
* Shift from docker.io to quay.io
* Improved operator startup
* Improved cluster startup
* Enhanced cluster stats endpoint to fallback for cgroup v1 and v2
* Make CronJob generator for v1 or v1beta1 api versions dependent on Kubernetes version

## Web Console

* Support user-supplied sidecars for SGCluster services
* Add loadBalancerIP to SGCluster and SGDistributedLogs

## :bug: FIXES

* ClusterBootstrapCompleted sent every few seconds
* Empty backupPath for majorVersionUpgrade SGDbOps make the operation fail
* Enable SSL specs
* Implemented cascading replication with WAL shipping
* Implemented cascade replication from an external instance
* Implemented cascade replication from a local sgcluster

## Web Console

* Edit SGPostgresConfig form should only list custom parameters
* Remove Advanced switch from Azure section
* Summaries should not open unless all required fields are filled in
* Backup scheduling inserting trailing zero in cron job minutes config
* Script source not cleared when deleting a script
* notValid class not being removed on Babelfish Experimental Feature
* Unify click behavious when clicked on a switch and on its label
* Cluster name on SGDbOp details is not clickable
* Sidebar items hidden behind dialog popups
* PITR date and time picker not working
* Continue on SGScripts Error should not be visible if there are no Scripts set
* Fix Extensions table layout
* View Script button text and icon on Cluster Configuration tab have different behaviours
* Hide empty sections on summaries
* Disable Connection Pooling not working properly on Cluster Form
* Object Storage selector on Cluster form shows all Object Storages from all Namespaces
* Make sure crontab is shown on preferred timezone

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

Alpha or beta version should not be used to upgrade. Please wait for a release candidate or general availability version.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.5.0-beta1)

# :rocket: Release 1.4.3 (2023-02-20)

## :notepad_spiral: NOTES

StackGres 1.4.3 updates the metadata for openshift certification compliance.

## :sparkles: NEW FEATURES AND CHANGES

* Updated StackGres Bundle metadata

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

Nothing new here! :eyes:

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.2 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.4.3/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.4.3)

# :rocket: release 1.4.2 (2023-01-24)

## :notepad_spiral: notes

StackGres 1.4.2 update the ubi base images for openshift certification compliance.

## :sparkles: new features and changes

* Updated StackGres base images

### web console

nothing new here! :eyes: 

## :bug: fixes

nothing new here! :eyes: 

### web console

nothing new here! :eyes: 

## :construction: known issues

* major version upgrade fails if some extensions version are not available for the target postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* backups may be restored with inconsistencies when performed with a postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: upgrade

to upgrade from a previous installation of the stackgres operator's helm chart you will have to upgrade the helm chart release.
 for more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

to upgrade stackgres operator's (upgrade only works starting from 1.2 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.4.2/helm/stackgres-operator.tgz`

> important: this release is incompatible with previous `alpha` or `beta` versions. upgrading from those versions will require uninstalling completely stackgres including all clusters and stackgres crds (those in `stackgres.io` group) first.

thank you for all the issues created, ideas, and code contributions by the stackgres community!

## :twisted_rightwards_arrows: [full list of commits](https://gitlab.com/ongresinc/stackgres/-/commits/1.4.2)

# :rocket: Release 1.4.1 (2023-01-06)

## :notepad_spiral: NOTES

StackGres 1.4.1 is out, we finally reached OpenShift 4.8-4.11 certification! :confetti_ball: :tada:

## :sparkles: NEW FEATURES AND CHANGES

* Updated ongres/kubectl images to `1.25.5-build-6.19` `1.22.17-build-6.19` `1.19.16-build-6.19` versions.
* Updated stackgres base images to `openjdk-17-runtime:1.14-8` version

## Web Console

Nothing new here! :eyes:

## :bug: FIXES

Nothing new here! :eyes:

## Web Console

Nothing new here! :eyes:

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.2 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.4.1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.4.1)

# :rocket: Release 1.4.0 (2022-11-23)

## :notepad_spiral: NOTES

StackGres 1.4.0 is out, prepared to help in recovering from a disaster and to hook into the cluster by adding your own custom containers. :confetti_ball: :sos: :alien: :gift_heart:

This release also introduce more support for typical Kubernetes scheduling configuration by adding support for node labels, tolerations, node affinity, pod affinity, pod anty affinity and topology spread constraints on all the Pods generated by the operator.

Also, major version upgrade include the most wanted best-effort rollback feature so you will not have to recover a crashed major version upgrade manually if you do not specify any destructive option like using clone or link. The check option also changes the operation so that the upgrade will not be performed at all but only checks will run. And, last but not least, the operation now also performs a cleanup so that the old data will be wiped out when the operation completes successfully and the primary Pod reach the Ready state.

So, what you are waiting for to try this beta release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.25
* Updated kubectl image to use version 1.24
* PostgreSQL 15.1, 15.0, 14.6, 14.5, 14.4, 13.9, 13.8, 13.7, 12.13, 12.12 and 12.11
* Wal-g 2.0.1
* Envoy 1.24.0
* PgBouncer 1.17.0
* Fluent-Bit 1.9.9 and Fluentd to 1.15.3
* Postgres Exporter 0.11.1
* Babelfish for PostgreSQL 14.3 (still in alpha, also take into account 14.3 has the CREATE INDEX CONCURRENTLY bug in it so use at your own risk!)
* Babelfish Compass 2022.10
* Disallow creation using PostgreSQL 14.3- (except for Babelfish) and create a warning if a user is using it
* Added SGStorageObject CRD to configure where to store any kind of object (used initially only for backups) 
* Support to configure backups in SGCluster specifying automatic backup configuration and reference SGStorageObject CRs
* Automatic migration from SGBackupConfig CR to the new SGStorageObject CR in SGCluster and deprecation of SGBackupConfig
* Support to configure managed SQL in SGCluster specifying a reference to SGScript CRs
* Automatic migration from initial data script to managed SQL with SGScript CRs
* Support for resource restrictions for all Pod's containers
* Allow to specify requests and limit in SGInstanceProfile for non-production
* Support SGInstanceProfile and SGPostgresConfig on SGDistributedLogs 
* Removed compatibility with clusters created in StackGres 1.0
* Validate and integrate into tests OpenShift 4.9+
* Allow specifying loadBalancerIP for postgres services
* Tolerations for SGDbOps
* Allow specifying node selector and node affinity for SGBackup, SGDistributedLogs, and SGDbOps
* Show wal-g wal-verify output in backup Job logs
* Allow managing pods in parallel
* Improved operator helm chart upgrade
* Annotations, affinity, tolerations, and nodeSelector added in Operator Helm Chart
* Support for Cert Manager certificates added in Operator Helm Chart
* Change backup CronJob concurrencyPolicy to Forbid
* Support for HTTP gzip compression when fetching the extension's metadata
* Automatic reload of pgbouncer config

## Web Console

* Initial support for OpenID Connect
* Divide extensions according to their license
* Enhanced usability/discoverability of the "enable monitoring" option when creating a cluster
* Unify switches texts on forms
* Change the text of Close Details button
* Improve Backup configuration layout/order on SGCluster form
* Simplify action buttons names on CRD Details
* Update and improve the UI Connection Info popup
* Add button to go back to List view on Cluster Details

## :bug: FIXES

* Certificate is not issued for EKS 1.22+
* After upgrading operator from 1.3.3 the SGCluster StatefulSet was missing the patroni container.
* Support for Kubernetes 1.25 was not working
* Events service being suppressed during benchmark job
* Repeated error messages returned from REST API
* Backups Job shows some permission errors in the log
* Pending state during the creation of SGBackup for clusters without backup configuration
* Unable to restore PITR in any cluster
* Images with a non-root account fail to read the token file on EKS
* Mutating webhook bug make validation to be skipped when a wrong postgres version was issued
* Add missing resources to the can-i REST API endpoint
* The info property of all sgcluster related endpoints is returning the deprecated `<cluster name>-primary` service
* Set default log_statement value to none for SGPostgresConfig
* Lower the initial param autovacuum_work_mem
* Wrong message for wrong version on major/minor upgrade validation
* Cluster is not reconciled when prometheus auto bind is disabled in the operator
* Make the restart shell scripts more resilient

## Web Console

* General improvement of distributed logs and benchmark results
* General improvement of user permissions validations
* Monitoring tab is empty when there are no active pods
* Pods and time range selectors missing on the monitoring tab
* Namespace selector won't stay open
* Review and adjust tooltips that won't match reverse-logic specs
* Not Found appears on top of Header on Details views
* Details about Distributed logs configuration not shown in the logs server section
* Namespaces Overview header appears when logged out but won't show on login
* Managed backups specs not loading on SGCluster form
* Proposed default names contain non-valid characters
* Clone CRD function not working for SGClusters, SGPostgresConfigs and SGPoolingConfigs
* Adjust pagination color scheme on dark mode
* Wait Timeout on Repack databases appears empty
* Remove Enable Primary Service toggle from Distributed Logs form
* Missing service status on SGCluster and SGDistributedLogs details
* Fix misplaced warning icons

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.4.0/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.4.0)

# :rocket: Release 1.4.0-RC2 (2022-11-22)

## :notepad_spiral: NOTES

StackGres 1.4.0-RC2 is out, prepared to help in recovering from a disaster and to hook into the cluster by adding your own custom containers. :confetti_ball: :sos: :alien: :gift_heart:

This release also introduce more support for typical Kubernetes scheduling configuration by adding support for node labels, tolerations, node affinity, pod affinity, pod anty affinity and
 topology spread constraints on all the Pods generated by the operator.

Also, major version upgrade include the most wanted best-effort rollback feature so you will not have to recover a crashed major version upgrade manually if you do not specify any destructive
 option like using clone or link. The check option also changes the operation so that the upgrade will not be performed at all but only checks will run. And, last but not least, the operation
 now also performs a cleanup so that the old data will be wiped out when the operation completes successfully and the primary Pod reach the Ready state.

So, what you are waiting for to try this beta release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.25
* Updated kubectl image to use version 1.24
* PostgreSQL 15.1, 15.0, 14.6, 14.5, 14.4, 13.9, 13.8, 13.7, 12.13, 12.12 and 12.11
* Wal-g 2.0.1
* Envoy 1.24.0
* PgBouncer 1.17.0
* Fluent-Bit 1.9.9 and Fluentd to 1.15.3
* Postgres Exporter 0.11.1
* Babelfish for PostgreSQL 14.3 (still in alpha, also take into account 14.3 has the CREATE INDEX CONCURRENTLY bug in it so use at your own risk!)
* Babelfish Compass 2022.10
* Disallow creation using PostgreSQL 14.3- (except for Babelfish) and create a warning if a user is using it
* Added SGStorageObject CRD to configure where to store any kind of object (used initially only for backups) 
* Support to configure backups in SGCluster specifying automatic backup configuration and reference SGStorageObject CRs
* Automatic migration from SGBackupConfig CR to the new SGStorageObject CR in SGCluster and deprecation of SGBackupConfig
* Support to configure managed SQL in SGCluster specifying a reference to SGScript CRs
* Automatic migration from initial data script to managed SQL with SGScript CRs
* Support for resource restrictions for all Pod's containers
* Allow to specify requests and limit in SGInstanceProfile for non-production
* Support SGInstanceProfile and SGPostgresConfig on SGDistributedLogs 
* Removed compatibility with clusters created in StackGres 1.0
* Validate and integrate into tests OpenShift 4.9+
* Allow specifying loadBalancerIP for postgres services
* Tolerations for SGDbOps
* Allow specifying node selector and node affinity for SGBackup, SGDistributedLogs, and SGDbOps
* Show wal-g wal-verify output in backup Job logs
* Allow managing pods in parallel
* Improved operator helm chart upgrade
* Annotations, affinity, tolerations, and nodeSelector added in Operator Helm Chart
* Support for Cert Manager certificates added in Operator Helm Chart
* Change backup CronJob concurrencyPolicy to Forbid
* Support for HTTP gzip compression when fetching the extension's metadata
* Automatic reload of pgbouncer config

## Web Console

* Initial support for OpenID Connect
* Divide extensions according to their license
* Enhanced usability/discoverability of the "enable monitoring" option when creating a cluster
* Unify switches texts on forms
* Change the text of Close Details button
* Improve Backup configuration layout/order on SGCluster form
* Simplify action buttons names on CRD Details
* Update and improve the UI Connection Info popup
* Add button to go back to List view on Cluster Details

## :bug: FIXES

* Certificate is not issued for EKS 1.22+
* After upgrading operator from 1.3.3 the SGCluster StatefulSet was missing the patroni container.
* Support for Kubernetes 1.25 was not working
* Events service being suppressed during benchmark job
* Repeated error messages returned from REST API
* Backups Job shows some permission errors in the log
* Pending state during the creation of SGBackup for clusters without backup configuration
* Unable to restore PITR in any cluster
* Images with a non-root account fail to read the token file on EKS
* Mutating webhook bug make validation to be skipped when a wrong postgres version was issued
* Add missing resources to the can-i REST API endpoint
* The info property of all sgcluster related endpoints is returning the deprecated `<cluster name>-primary` service
* Set default log_statement value to none for SGPostgresConfig
* Lower the initial param autovacuum_work_mem
* Wrong message for wrong version on major/minor upgrade validation
* Cluster is not reconciled when prometheus auto bind is disabled in the operator
* Make the restart shell scripts more resilient

## Web Console

* General improvement of distributed logs and benchmark results
* General improvement of user permissions validations
* Monitoring tab is empty when there are no active pods
* Pods and time range selectors missing on the monitoring tab
* Namespace selector won't stay open
* Review and adjust tooltips that won't match reverse-logic specs
* Not Found appears on top of Header on Details views
* Details about Distributed logs configuration not shown in the logs server section
* Namespaces Overview header appears when logged out but won't show on login
* Managed backups specs not loading on SGCluster form
* Proposed default names contain non-valid characters
* Clone CRD function not working for SGClusters, SGPostgresConfigs and SGPoolingConfigs
* Adjust pagination color scheme on dark mode
* Wait Timeout on Repack databases appears empty
* Remove Enable Primary Service toggle from Distributed Logs form
* Missing service status on SGCluster and SGDistributedLogs details
* Fix misplaced warning icons

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.4.0-RC2/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.4.0-RC2)

# :rocket: Release 1.4.0-RC1 (2022-11-15)

## :notepad_spiral: NOTES

StackGres 1.4.0-RC1 is out, prepared to help in recovering from a disaster and to hook into the cluster by adding your own custom containers. :confetti_ball: :sos: :alien: :gift_heart:

This release also introduce more support for typical Kubernetes scheduling configuration by adding support for node labels, tolerations, node affinity, pod affinity, pod anty affinity and
 topology spread constraints on all the Pods generated by the operator.

Also, major version upgrade include the most wanted best-effort rollback feature so you will not have to recover a crashed major version upgrade manually if you do not specify any destructive
 option like using clone or link. The check option also changes the operation so that the upgrade will not be performed at all but only checks will run. And, last but not least, the operation
 now also performs a cleanup so that the old data will be wiped out when the operation completes successfully and the primary Pod reach the Ready state.

So, what you are waiting for to try this beta release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.25
* Updated kubectl image to use version 1.24
* PostgreSQL 15.1, 15.0, 14.6, 14.5, 14.4, 13.9, 13.8, 13.7, 12.13, 12.12 and 12.11
* Wal-g 2.0.1
* Envoy 1.24.0
* PgBouncer 1.17.0
* Fluent-Bit 1.9.9 and Fluentd to 1.15.3
* Postgres Exporter 0.11.1
* Babelfish for PostgreSQL 14.3 (still in alpha, also take into account 14.3 has the CREATE INDEX CONCURRENTLY bug in it so use at your own risk!)
* Babelfish Compass 2022.10
* Disallow creation using PostgreSQL 14.3- (except for Babelfish) and create a warning if a user is using it
* Added SGStorageObject CRD to configure where to store any kind of object (used initially only for backups) 
* Support to configure backups in SGCluster specifying automatic backup configuration and reference SGStorageObject CRs
* Automatic migration from SGBackupConfig CR to the new SGStorageObject CR in SGCluster and deprecation of SGBackupConfig
* Support to configure managed SQL in SGCluster specifying a reference to SGScript CRs
* Automatic migration from initial data script to managed SQL with SGScript CRs
* Support for resource restrictions for all Pod's containers
* Allow to specify requests and limit in SGInstanceProfile for non-production
* Support SGInstanceProfile and SGPostgresConfig on SGDistributedLogs 
* Removed compatibility with clusters created in StackGres 1.0
* Validate and integrate into tests OpenShift 4.9+
* Allow specifying loadBalancerIP for postgres services
* Tolerations for SGDbOps
* Allow specifying node selector and node affinity for SGBackup, SGDistributedLogs, and SGDbOps
* Show wal-g wal-verify output in backup Job logs
* Allow managing pods in parallel
* Improved operator helm chart upgrade
* Annotations, affinity, tolerations, and nodeSelector added in Operator Helm Chart
* Support for Cert Manager certificates added in Operator Helm Chart
* Change backup CronJob concurrencyPolicy to Forbid
* Support for HTTP gzip compression when fetching the extension's metadata
* Automatic reload of pgbouncer config

## Web Console

* Initial support for OpenID Connect
* Divide extensions according to their license
* Enhanced usability/discoverability of the "enable monitoring" option when creating a cluster
* Unify switches texts on forms
* Change the text of Close Details button
* Improve Backup configuration layout/order on SGCluster form
* Simplify action buttons names on CRD Details
* Update and improve the UI Connection Info popup
* Add button to go back to List view on Cluster Details

## :bug: FIXES

* Certificate is not issued for EKS 1.22+
* Events service being suppressed during benchmark job
* Repeated error messages returned from REST API
* Backups Job shows some permission errors in the log
* Pending state during the creation of SGBackup for clusters without backup configuration
* Unable to restore PITR in any cluster
* Images with a non-root account fail to read the token file on EKS
* Mutating webhook bug make validation to be skipped when a wrong postgres version was issued
* Add missing resources to the can-i REST API endpoint
* The info property of all sgcluster related endpoints is returning the deprecated `<cluster name>-primary` service
* Set default log_statement value to none for SGPostgresConfig
* Lower the initial param autovacuum_work_mem
* Wrong message for wrong version on major/minor upgrade validation
* Cluster is not reconciled when prometheus auto bind is disabled in the operator
* Make the restart shell scripts more resilient

## Web Console

* General improvement of distributed logs and benchmark results
* General improvement of user permissions validations
* Monitoring tab is empty when there are no active pods
* Pods and time range selectors missing on the monitoring tab
* Namespace selector won't stay open
* Review and adjust tooltips that won't match reverse-logic specs
* Not Found appears on top of Header on Details views
* Details about Distributed logs configuration not shown in the logs server section
* Namespaces Overview header appears when logged out but won't show on login
* Managed backups specs not loading on SGCluster form
* Proposed default names contain non-valid characters
* Clone CRD function not working for SGClusters, SGPostgresConfigs and SGPoolingConfigs
* Adjust pagination color scheme on dark mode
* Wait Timeout on Repack databases appears empty
* Remove Enable Primary Service toggle from Distributed Logs form
* Missing service status on SGCluster and SGDistributedLogs details
* Fix misplaced warning icons

## :construction: KNOWN ISSUES

* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.4.0-RC1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.4.0-RC1)

# :rocket: Release 1.4.0-beta1 (2022-10-21)

## :notepad_spiral: NOTES

StackGres 1.4.0-beta1 is here, prepared to help in recovering from a disaster and to hook into the cluster by adding your own custom containers. :confetti_ball: :sos: :alien: :gift_heart:

This release also introduce more support for typical Kubernetes scheduling configuration by adding support for node labels, tolerations, node affinity, pod affinity, pod anty affinity and
 topology spread constraints on all the Pods generated by the operator.

Also, major version upgrade include the most wanted best-effort rollback feature so you will not have to recover a crashed major version upgrade manually if you do not specify any destructive
 option like using clone or link. The check option also changes the operation so that the upgrade will not be performed at all but only checks will run. And, last but not least, the operation
 now also performs a cleanup so that the old data will be wiped out when the operation completes successfully and the primary Pod reach the Ready state.

So, what you are waiting for to try this beta release and have a look to the future of StackGres! 

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.25
* Support for cascading, remote and WAL-based SGCluster replication
* Support user-supplied sidecars for SGCluster pods
* Set default_toast_compression=lz4 on PostgreSQL 14's default config
* Support for tolerations, affinity and topology spread constraints
* Major version upgrade improvements: rollback, cleanup and only check.
* Added support for Grafana 5.x
* Make local Pod controller aware of distinction between leader and stanby leader
* Improve JVM startup with Java CDS

### Web Console

* Hide Parameters title when that section is empty on SGPostgresConfig details
* Improve homepage when there are no namespaces in use
* Highlight Storage Types fields when they cause an error on submit
* Adjust layout on DbOps Overview when empty
* Adjust color of Open in new tab icons

## :bug: FIXES

* Script setup-data-path.sh not executing, resulting in broken permissions
* Backup are marked completed when they are not
* Generated prometheus stats services return ambiguous stats

### Web Console

* Cluster form title is "CREATE CLUSTER" even when editing
* Disable delete option on sginstanceprofiles and sgpostgresconfigs in use
* Postgres Version not shown on SGPostgresConfig details
* Cluster Config tab wont load because of Script without scriptSpec
* Resource name not shown on breadcrumbs
* SGCluster edit screen and summary assumes backup performance specs always exist
* Node Affinity match set when no inputs have been filled

## :construction: KNOWN ISSUES

* Installation fails in EKS 1.22+ due to CSR not returning the certificate ([#1732](https://gitlab.com/ongresinc/stackgres/-/issues/1732)). Use cert-manager as a workaround. 
* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.4.0-beta1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.4.0-beta1)

# :rocket: Release 1.3.3 (2022-10-18)

## :notepad_spiral: NOTES

StackGres 1.3.3 fix a bug in a script introduced in version 1.3.2 that prevents the cluster from restarting.

## :sparkles: NEW FEATURES AND CHANGES

Nothing new here! :eyes: 

### Web Console

Nothing new here! :eyes: 

## :bug: FIXES

* Script setup-data-path.sh not executing, resulting in broken permissions 
* Backup are marked completed when they are not

### Web Console

Nothing new here! :eyes: 

## :construction: KNOWN ISSUES

* Installation fails in EKS 1.22+ due to CSR not returning the certificate ([#1732](https://gitlab.com/ongresinc/stackgres/-/issues/1732)). Use cert-manager as a workaround. 
* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.3.3/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.3.3)

# :rocket: Release 1.3.2 (2022-10-14)

## :notepad_spiral: NOTES

StackGres 1.3.2 brings some security improvements and fixes. In particular it comes to fix an issue related to how SGInstanceProfile custom resource works for version 1.3 by applying resource requirements and limits to all the StackGres generated Pods. Due to some default settings that were too conservative for containers some functionalities were broken for some users (in particular for backups and postgres exporter memory was set too low making some Pods getting killed by the OOM killer).
Also a bug in the Web Console that, when editing a StackGres custom resource (and other resources), changed some fields that are not yet implemented there. In particular, the latter bug removed a non production option flag set during operator upgrade that prevented existing cluster from from avoiding following the new behavior of resources requirements for the StackGres Pod's that apply to sidecars increasing the actual requirements of a Pod following the new `.spec.containers` and `.spec.initContainers` section of the SGInstanceProfile resource. If you had upgraded to 1.3.0 or 1.3.1 and your SGCluster's Pods can not restart due to resources not being available consider setting to `true` the field `.spec.nonProductionOptions.disableClusterResourceRequirements` to revert to the same behavior prior version 1.3 where resource requirements only affect the patroni container.

## :sparkles: NEW FEATURES AND CHANGES

* Increased default memory limits and requests requirements for some Pod and sidecars
* Lower the resource requirements for containers other than patroni to only set resource requests and not limits by default (resource limits can still be set for those containers by configuration)

### Web Console

Nothing new here! :eyes: 

## :bug: FIXES

* Default SGScript is not created for SGCluster with version previous to 1.3
* Event of missing SGBackupConfig is sent even if backup is working as expected
* Avoid errors for old versions of a SGBackup, SGDbOps and SGScript
* Field `.spec.pods.managementPolicy` is not implemented in the REST API
* Cluster crash-Loop due to permission change in data dir

### Web Console

*  Babelfish Compass had a cross site script issue that prevent it from functioning
*  When editing a resource unknown fields values are not preserved

## :construction: KNOWN ISSUES

* Installation fails in EKS 1.22+ due to CSR not returning the certificate ([#1732](https://gitlab.com/ongresinc/stackgres/-/issues/1732)). Use cert-manager as a workaround. 
* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.3.2/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.3.2)

# :rocket: Release 1.3.1 (2022-09-07)

## :notepad_spiral: NOTES

StackGres 1.3.1 has come to fix some issues reported by our users, a big thank you for all the feedback provided!! :top: :heartpulse: 

## :sparkles: NEW FEATURES AND CHANGES

Nothing new here! :eyes: 

### Web Console

Nothing new here! :eyes: 

## :bug: FIXES

* Some libraries from source postgres version are missing while running pg_upgrade
* Syntax error while setting target postgres version on major version upgrade SGDbOps
* SGDbOps's Job prefix is not generated correctly
* Fields configMapKeyRef and secretKeyRef are not filled on upgrade when generating SGScript from old SGCluster
* Field .spec.managedSql.scripts entry duplicates id when a new entry with id 0 is provided by the user

### Web Console

* Resource name not shown on breadcrumbs
* CPU & Memory alerts are shown on every cluster
* Improve homepage when there are no namespaces in use
* Adjust color of Open in new tab icons
* Adjust layout on DbOps Overview when empty
* Highlight Storage Types fields when they cause an error on submit
* Node Affinity match set when no inputs have been filled
* Unify switches texts on forms
* Postgres Utils missing on Cluster Details
* SGCluster edit screen and summary assumes backup performance specs always exist
* Backup EDIT button points to undefined resource
* Cluster Config tab wont load because of Script without scriptSpec

## :construction: KNOWN ISSUES

* Installation fails in EKS 1.22+ due to CSR not returning the certificate ([#1732](https://gitlab.com/ongresinc/stackgres/-/issues/1732)). Use cert-manager as a workaround. 
* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.3.1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.3.1)

# :rocket: Release 1.3.0 (2022-08-23)

## :notepad_spiral: NOTES

StackGres 1.3.0 is out and something is changed!! Thanks to all our growing community for all the feedback they are providing, StackGres is better also thanks to you!! :tada: :cyclone: :top: :heartpulse: 

*IMPORTANT*: PostgreSQL 14.0, 14.1, 14.2, and 14.3 had an issue with CREATE INDEX CONCURRENTLY and REINDEX CONCURRENTLY that could cause silent data corruption of indexes. 
 Please upgrade to PostgreSQL 14.5 as soon as possible to avoid any corruption of your data. For more info see https://www.postgresql.org/about/news/postgresql-144-released-2470/.

*IMPORTANT*: We have introduced a possible breaking change for your deployment since we replaced the `SGBackupConfig` CRD with the `SGObjectStorage` CRD. All your existing clusters will continue to work since the upgrade process
 will take care of converting `SGBackupConfig` CRs to the new `SGObjectStorage` CRs and apply the required changes to the `SGCluster` CRs. The change was needed in order to introduce more functionality in the next releases, but for
 now, the only difference is that `SGObjectStorage` will contain only the `.spec.storage` part of the previous `SGBackupConfig`. Also, the backup configuration in the `SGCluster` is moved to the section `.spec.configurations.backups` where you will be able to set the referenced `SGObjectStorage` and configure the automatic backup properties. Please make sure your deployment code reflects the changes introduced by the upgrade by changing the `.spec.configurations`
 section and converting the `SGBackupConfig` into an `SGObjectStorage`.

Before this release, you had to set SQL scripts at initialization, but now you will be able to change and add them live. Also, scripts will be re-usable since they are now stored in the new SGScript CRD.

When a new cluster is created you will be able to set resource requirements for all the StackGres Pod's containers using the new sections `.spec.containers` and `.spec.initContainers`. Those sections are pre-filled with default values
 and you may disable this behavior by setting to `true` the field `.spec.nonProductionOptions.disableClusterResourceRequirements` in the `SGCluster` and `SGDistributedLogs` CRs.

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.24
* Updated kubectl image to use version 1.24
* PostgreSQL 14.5, 14.4, 13.8, 13.7, 12.12 and 12.11
* Fluent-Bit 1.9.6 and Fluentd to 1.15.0
* Wal-g 2.0.0
* Disallow creation using PostgreSQL 14.3- and create a warning if a user is using it
* Added SGStorageObject CRD to configure where to store any kind of object (used initially only for backups) 
* Support to configure backups in SGCluster specifying automatic backup configuration and reference SGStorageObject CRs
* Automatic migration from SGBackupConfig CR to the new SGStorageObject CR in SGCluster and deprecation of SGBackupConfig
* Support to configure managed SQL in SGCluster specifying a reference to SGScript CRs
* Automatic migration from initial data script to managed SQL with SGScript CRs
* Support for resource restrictions for all Pod's containers
* Allow to specify requests and limit in SGInstanceProfile for non-production
* Support SGInstanceProfile and SGPostgresConfig on SGDistributedLogs 
* Removed compatibility with clusters created in StackGres 1.0
* Validate and integrate into tests OpenShift 4.9+
* Allow specifying loadBalancerIP for postgres services
* Tolerations for SGDbOps
* Allow specifying node selector and node affinity for SGBackup, SGDistributedLogs, and SGDbOps
* Show wal-g wal-verify output in backup Job logs
* Allow managing pods in parallel
* Improved operator helm chart upgrade
* Annotations, affinity, tolerations, and nodeSelector added in Operator Helm Chart
* Support for Cert Manager certificates added in Operator Helm Chart
* Change backup CronJob concurrencyPolicy to Forbid
* Support for HTTP gzip compression when fetching the extension's metadata

### Web Console

* Initial support for OpenID Connect
* Divide extensions according to their license
* Enhanced usability/discoverability of the "enable monitoring" option when creating a cluster
* Unify switches texts on forms
* Change the text of Close Details button
* Improve Backup configuration layout/order on SGCluster form
* Simplify action buttons names on CRD Details
* Update and improve the UI Connection Info popup
* Add button to go back to List view on Cluster Details

## :bug: FIXES

* Events service being suppressed during benchmark job
* Repeated error messages returned from REST API
* Backups Job shows some permission errors in the log
* Pending state during the creation of SGBackup for clusters without backup configuration
* Unable to restore PITR in any cluster
* Images with a non-root account fail to read the token file on EKS
* Mutating webhook bug make validation to be skipped when a wrong postgres version was issued
* Add missing resources to the can-i REST API endpoint
* The info property of all sgcluster related endpoints is returning the deprecated `<cluster name>-primary` service
* Set default log_statement value to none for SGPostgresConfig
* Lower the initial param autovacuum_work_mem

### Web Console

* General improvement of distributed logs and benchmark results
* General improvement of user permissions validations
* Monitoring tab is empty when there are no active pods
* Pods and time range selectors missing on the monitoring tab
* Namespace selector won't stay open
* Review and adjust tooltips that won't match reverse-logic specs
* Not Found appears on top of Header on Details views
* Details about Distributed logs configuration not shown in the logs server section
* Namespaces Overview header appears when logged out but won't show on login
* Managed backups specs not loading on SGCluster form
* Proposed default names contain non-valid characters
* Clone CRD function not working for SGClusters, SGPostgresConfigs and SGPoolingConfigs
* Adjust pagination color scheme on dark mode
* Wait Timeout on Repack databases appears empty
* Remove Enable Primary Service toggle from Distributed Logs form
* Missing service status on SGCluster and SGDistributedLogs details
* Fix misplaced warning icons

## :construction: KNOWN ISSUES

* Installation fails in EKS 1.22+ due to CSR not returning the certificate ([#1732](https://gitlab.com/ongresinc/stackgres/-/issues/1732)). Use cert-manager as a workaround. 
* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue the following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.3.0/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas, and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.3.0)

# :rocket: Release 1.3.0-RC1 (2022-08-16)

## :notepad_spiral: NOTES

StackGres 1.3.0-RC1 is out and something is changed!! Thanks to all our growing community for all the feedback they are providing, StackGres is better also thank to you!! :tada: :cyclone: :top: :heartpulse: 

*IMPORTANT*: PostgreSQL 14.0, 14.1, 14.2 and 14.3 had an issue with CREATE INDEX CONCURRENTLY and REINDEX CONCURRENTLY that could cause silent data corruption of indexes. 
 Please upgrade to PostgreSQL 14.4 as soon as possible to avoid any corruption to your data. For more info see https://www.postgresql.org/about/news/postgresql-144-released-2470/.

*IMPORTANT*: We have introduced a possible breaking change for your deployment since we replaced the `SGBackupConfig` CRD with the `SGObjectStorage` CRD. All your existing clusters will continue to work since the upgrade process
 will take care of converting `SGBackupConfig` CRs to the new `SGObjectStorage` CRs and apply the required changes into the `SGCluster` CRs. The change was needed in order to introduce more functionality in next releases, but for
 now the only difference is that `SGObjectStorage` will contain only the `.spec.storage` part of the previous `SGBackupConfig`. Also the backup configuration in the `SGCluster` is moved to the section `.spec.configurations.backups`
 where you will be able to set the referenced `SGObjectStorage` and configure the automatic backup properties. Please, make sure your deployment code reflect the changes introduced by the upgrade by changing the `.spec.configurations`
 section and converting the `SGBackupConfig` into an `SGObjectStorage`.

Before this release you had to set SQL scripts at initialization, but now you will be able to change and add them live. Also scripts will be re-usable since they are now stored in the new SGScript CRD.

When a new cluster is created you will be able to set resource requirements for all the StackGres Pod's containers using the new sections `.spec.containers` and `.spec.initContainers`. Those sections are pre-filled with default values
 and you may disable this behavior by setting to `true` the field `.spec.nonProductionOptions.disableClusterResourceRequirements` in the `SGCluster` and `SGDistributedLogs` CRs.

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.24
* Updated kubectl image to use version 1.24
* PostgreSQL 14.4, 14.5, 13.7, 13.8, 12.11 and 12.12
* Fluent-Bit 1.9.6 and Fluentd to 1.15.0
* Wal-g 2.0.0
* Disallow creation using PostgreSQL 14.3- and create a warning if user is using it
* Added SGStorageObject CRD to configure where to store any kind of object (used initially only for backups) 
* Support to configure backups in SGCluster specifying automatic backup configuration and to reference SGStorageObject CRs
* Automatic migration from SGBackupConfig CR to the new SGStorageObject CR in SGCluster and deprecation of SGBackupConfig
* Support to configure managed SQL in SGCluster specifying reference to SGScript CRs
* Automatic migration from initial data script to managed SQL with SGScript CRs
* Support for resource restrictions for all Pod's containers
* Allow specify request and limit in sginstanceprofile for non-production
* Support SGInstanceProfile and SGPostgresConfig on SGDistributedLogs 
* Removed support for StackGres 1.0
* Validate and integrate into tests OpenShift 4.9+
* Allow specify loadBalancerIP for postgres services
* Tolerations for SGDbOps
* Allow to specify node selector and node affinity for SGBackup, SGDistributedLogs and SGDbOps
* Show wal-g wal-verify output in backup Job logs
* Allow to manage pods in parallel
* Improved operator helm chart upgrade
* Annotations, affinity, tolerations, and nodeSelector added in Operator Helm Chart
* Support for Cert Manager certificates added in Operator Helm Chart
* Change backup CronJob concurrencyPolicy to Forbid
* Support for HTTP gzip compression

## Web Console

* Support for Open ID Connect
* Divide extensions according to their license
* Enhanced usability/discoverability of the "enable monitoring" option when creating a cluster
* Unify switches texts on forms
* Change text of Close Details button
* Improve Backup configuration layout/order on SGCluster form
* Simplify action buttons names on CRD Details
* Update and improve the UI Connection Info popup
* Add button to go back to List view on Cluster Details

## :bug: FIXES

* Events service being suppressed during benchmark job
* Repeated error messages returned from REST API
* Backups Job show some permission errors in the log
* Pending state during creation of SGBackup for clusters without backup configuration
* Unable to restore PITR in any cluster
* Images with non-root account fails to read token file on EKS
* Mutating webhook bug make validation to be skipped when a wrong postgres version was issued
* Add missing resources to the can-i REST API endpoint
* The info property of all sgcluster related endpoints is returning the deprecated <cluster name>-primary service
* Set default log_statement value to none for SGPostgresConfig
* Lower the initial param autovacuum_work_mem

## Web Console

* General improvement of distributed logs and benchmark results
* General improvement of user permissions validations
* Monitoring tab is empty when there are no active pods
* Pods and time range selectors missing on monitoring tab
* Namespace selector won't stay open
* Review and adjust tooltips that won't match reverse-logic specs
* Not Found appears on top of Header on Details views
* Details about Distributed logs configuration not shown in logs server section
* Namespaces Overview header appears when logged out but won't show on login
* Managed backups specs not loading on SGCluster form
* Proposed default names contain non-valid characters
* Clone CRD function not working for SGClusters, SGPostgresConfigs and SGPoolingConfigs
* Adjust pagination color scheme on darkmode
* Wait Timeout on Repack databases appears empty
* Remove Enable Primary Service toggle from Distributed Logs form
* Missing service status on SGCluster and SGDistributedLogs details
* Fix misplaced warning icons

## :construction: KNOWN ISSUES

* Installation fails in EKS 1.22+ due to CSR not returning the certificate ([#1732](https://gitlab.com/ongresinc/stackgres/-/issues/1732)). Use cert manager as a workaround. 
* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.3.0-RC1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.3.0-RC1)

# :rocket: Release 1.3.0-beta1 (2022-07-27)

## :notepad_spiral: NOTES

StackGres 1.3.0-beta1 is out and something is changed!! Thanks to all our growing community for all the feedback they are providing, StackGres is better also thank to you!! :tada: :cyclone: :top: :heartpulse: 

*IMPORTANT*: PostgreSQL 14.0, 14.1, 14.2 and 14.3 had an issue with CREATE INDEX CONCURRENTLY and REINDEX CONCURRENTLY that could cause silent data corruption of indexes. 
 Please upgrade to PostgreSQL 14.4 as soon as possible to avoid any corruption to your data. For more info see https://www.postgresql.org/about/news/postgresql-144-released-2470/.

*IMPORTANT*: We have introduced a possible breaking change for your deployment since we replaced the `SGBackupConfig` CRD with the `SGObjectStorage` CRD. All your existing clusters will continue to work since the upgrade process
 will take care of converting `SGBackupConfig` CRs to the new `SGObjectStorage` CRs and apply the required changes into the `SGCluster` CRs. The change was needed in order to introduce more functionality in next releases, but for
 now the only difference is that `SGObjectStorage` will contain only the `.spec.storage` part of the previous `SGBackupConfig`. Also the backup configuration in the `SGCluster` is moved to the section `.spec.configurations.backups`
 where you will be able to set the referenced `SGObjectStorage` and configure the automatic backup properties. Please, make sure your deployment code reflect the changes introduced by the upgrade by changing the `.spec.configurations`
 section and converting the `SGBackupConfig` into an `SGObjectStorage`.

Before this release you had to set SQL scripts at initialization, but now you will be able to change and add them live. Also scripts will be re-usable since they are now stored in the new SGScript CRD.

When a new cluster is created you will be able to set resource requirements for all the StackGres Pod's containers using the new sections `.spec.containers` and `.spec.initContainers`. Those sections are pre-filled with default values
 and you may disable this behavior by setting to `true` the field `.spec.nonProductionOptions.disableClusterResourceRequirements` in the `SGCluster` and `SGDistributedLogs` CRs.

## :sparkles: NEW FEATURES AND CHANGES

* Support Kubernetes 1.24
* PostgreSQL 14.4, 13.7 and 12.11
* Fluent-Bit 1.9.6 and Fluentd to 1.15.0
* Wal-g 2.0.0
* Disallow creation using PostgreSQL 14.3- and create a warning if user is using it
* Added SGStorageObject CRD to configure where to store any kind of object (used initially only for backups) 
* Support to configure backups in SGCluster specifying automatic backup configuration and to reference SGStorageObject CRs
* Automatic migration from SGBackupConfig CR to the new SGStorageObject CR in SGCluster and deprecation of SGBackupConfig
* Support to configure managed SQL in SGCluster specifying reference to SGScript CRs
* Automatic migration from initial data script to managed SQL with SGScript CRs
* Support for resource restrictions for all Pod's containers
* Allow specify request and limit in sginstanceprofile for non-production
* Support SGInstanceProfile and SGPostgresConfig on SGDistributedLogs 
* Removed support for StackGres 1.0
* Validate and integrate into tests OpenShift 4.9+
* Allow specify loadBalancerIP for postgres services
* Tolerations for SGDbOps
* Allow to specify node selector and node affinity for SGBackup, SGDistributedLogs and SGDbOps
* Show wal-g wal-verify output in backup Job logs
* Allow to manage pods in parallel
* Improved operator helm chart upgrade
* Annotations, affinity, tolerations, and nodeSelector added in Operator Helm Chart
* Support for Cert Manager certificates added in Operator Helm Chart

## Web Console

* Support for Open ID Connect
* Divide extensions according to their license
* Enhanced usability/discoverability of the "enable monitoring" option when creating a cluster
* Unify switches texts on forms
* Change text of Close Details button
* Improve Backup configuration layout/order on SGCluster form
* Simplify action buttons names on CRD Details
* Update and improve the UI Connection Info popup
* Add button to go back to List view on Cluster Details

## :bug: FIXES

* Unable to restore PITR in any cluster
* Images with non-root account fails to read token file on EKS
* Mutating webhook bug make validation to be skipped when a wrong postgres version was issued
* Add missing resources to the can-i REST API endpoint
* The info property of all sgcluster related endpoints is returning the deprecated <cluster name>-primary service

## Web Console

* General improvement of user permissions validations
* Monitoring tab is empty when there are no active pods
* Pods and time range selectors missing on monitoring tab
* Namespace selector won't stay open
* Review and adjust tooltips that won't match reverse-logic specs
* Not Found appears on top of Header on Details views
* Details about Distributed logs configuration not shown in logs server section
* Namespaces Overview header appears when logged out but won't show on login
* Managed backups specs not loading on SGCluster form
* Proposed default names contain non-valid characters
* Clone CRD function not working for SGClusters, SGPostgresConfigs and SGPoolingConfigs
* Adjust pagination color scheme on darkmode
* Wait Timeout on Repack databases appears empty
* Remove Enable Primary Service toggle from Distributed Logs form
* Missing service status on SGCluster and SGDistributedLogs details
* Fix misplaced warning icons

## :construction: KNOWN ISSUES

* Installation fails in EKS 1.22+ due to CSR not returning the certificate ([#1732](https://gitlab.com/ongresinc/stackgres/-/issues/1732)). Use cert manager as a workaround. 
* Major version upgrade fails if some extensions version are not available for the target Postgres version ([#1368](https://gitlab.com/ongresinc/stackgres/-/issues/1368)) 
* Backups may be restored with inconsistencies when performed with a Postgres instance running on a different architecture ([#1539](https://gitlab.com/ongresinc/stackgres/-/issues/1539))

## :up: UPGRADE

To upgrade from a previous installation of the StackGres operator's helm chart you will have to upgrade the helm chart release.
 For more detailed information please refer to [our documentation](https://stackgres.io/doc/latest/install/helm/upgrade/#upgrade-operator).

To upgrade StackGres operator's (upgrade only works starting from 1.1 version or above) helm chart issue following commands (replace namespace and release name if you used something different):

`helm upgrade -n "stackgres" "stackgres-operator" https://stackgres.io/downloads/stackgres-k8s/stackgres/1.3.0-beta1/helm/stackgres-operator.tgz`

> IMPORTANT: This release is incompatible with previous `alpha` or `beta` versions. Upgrading from those versions will require uninstalling completely StackGres including all clusters and StackGres CRDs (those in `stackgres.io` group) first.

Thank you for all the issues created, ideas and code contributions by the StackGres Community!

## :twisted_rightwards_arrows: [FULL LIST OF COMMITS](https://gitlab.com/ongresinc/stackgres/-/commits/1.3.0-beta1)

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

