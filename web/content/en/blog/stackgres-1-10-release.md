+++
title = "StackGres 1.10: Autoscaling and Replication initialization from backups"
date = 2024-05-10T10:57:02+02:00
author = ["jorge"]
tags = ["postgres", "kubernetes", "autoscaling", "replication", "release"]
# planet = "jorsol"
draft = false
thumbnail = "img/blog/StackGres_1-0-0.jpg"
description = "StackGres is an Open Source operator for running production-ready Postgres on Kubernetes."
+++

## StackGres 1.10: Autoscaling and Replication initialization from backups

In this post we will talk about StackGres 1.10.0 – a release with improved autoscaling and replication from backups, plus several bug fixes for improved stability!

### TL;DR

* Support horizontal and vertical autoscaling with KEDA integration.
* Support replication initialization from backups.
* Fixes and stability improvements.

You can find the full changelog for this version in our GitLab [release page](https://gitlab.com/ongresinc/stackgres/-/releases/1.10.0).

### What's new?

#### Automated Scaling improvements

Prepare for automatic horizontal (and soon vertical) autoscaling: StackGres 1.9.0 lays the groundwork for seamless integration with [Horizontal Pod Autoscaler (HPA)](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) and [Kubernetes Event-Driven Autoscaler (KEDA)](https://keda.sh/). Now, StackGres 1.10.0 improves this feature to allow the use of resources more effectively.

This can be configured using the `SGCluster.spec.autoscaling` section:

In the below example (the full spec is omitted for brevity), when 75% of the average value of replicas connections are used the SGCluster will be scaled horizontally from 2 to 4 instances:

```
apiVersion: stackgres.io/v1
kind: SGCluster
spec:
...
  autoscaling:
    mode: horizontal
    minInstances: 2
    maxInstances: 4
    horizontal:
      replicasConnectionsUsageTarget: 0.75
```

 To allow autoscaling replication groups the field `SGCluster.spec.replication.groups.minInstances` has been added so that the number of instances in the group where the field is specified, instead of having a fixed number, will be calculated based on the following formula:

```
 <group instances> = max(<group minInstances>, <group minInstances> * <cluster instances> /
     <cluster autoscaling minInstances>)
```

> `VerticalPodAutoscaler`, used by StackGres vertical autoscaling, still do not implement in-place resources update, this makes vertical autoscaling viable in far less use cases since it requires the instances (including the primary) to be restarted.

Check more in the documentation: https://stackgres.io/doc/1.10/reference/crd/sgcluster/#sgclusterspecautoscaling

#### Replication initialization from backups

Replication initialization has been finally upgraded in order to allow using existing backups or, if configured so, to create a new backup in order to use it for initialization.

This feature allows to reduce the load of the primary instance since now is not required to create a base backup every time a replica is spin up, the replicas can be bootstrapped from a backup fetch from an object store.

To configure this feature you can check the modes allowed in `SGCluster.spec.replication.initialization`:

```
❯ kubectl explain SGCluster.spec.replication.initialization
GROUP:      stackgres.io
KIND:       SGCluster
VERSION:    v1

FIELD: initialization <Object>

DESCRIPTION:
    Allow to specify how the replicas are initialized.
    
FIELDS:
  backupNewerThan	<string>
    An ISO 8601 duration in the format `PnDTnHnMn.nS`, that specifies how old an
    SGBackup have to be in order to be seleceted
     to initialize a replica.
    
    When `FromExistingBackup` mode is set this field restrict the selection of
    SGBackup to be used for recovery newer than the
     specified value. 
    
    When `FromNewlyCreatedBackup` mode is set this field skip the creation
    SGBackup to be used for recovery if one newer than
     the specified value exists. 

  backupRestorePerformance	<Object>
    Configuration that affects the backup network and disk usage performance
    during recovery.

  mode	<string>
    Allow to specify how the replicas are initialized.
    
    Possible values are:
    
    * `FromPrimary`: When this mode is used replicas will be always created from
    the primary using `pg_basebackup`.
    * `FromReplica`: When this mode is used replicas will be created from
    another existing replica using
     `pg_basebackup`. Fallsback to `FromPrimary` if there's no replica or it
    fails.
    * `FromExistingBackup`: When this mode is used replicas will be created from
    an existing SGBackup. If `backupNewerThan` is set
     the SGBackup must be newer than its value. When this mode fails to restore
    an SGBackup it will try with a previous one (if exists).
     Fallsback to `FromReplica` if there's no backup left or it fails.
    * `FromNewlyCreatedBackup`: When this mode is used replicas will be created
    from a newly created SGBackup.
     Fallsback to `FromExistingBackup` if `backupNewerThan` is set and exists a
    recent backup newer than its value or it fails.
```

 `FromExistingBackup` as the name implies requires an existing backup, and `FromNewlyCreatedBackup` a new backup will be created and used if the `backupNewerThan` does't find a backup that is newer than the date provided.

#### Stability fixes

StackGres 1.10.0 contains as usual a set of stability fixes around all areas of the operator, so you can enjoy a stable management platform.

Some of the fixes includes:

* Backup retention was removing WALs of old unmanaged lifecycle backups (breaking them).
* Custom volume mounts model was broken.
* Remove PgBouncer queries from postgres exporter if cluster connection pooling is disabled.
* When recoverying from a volume snapshot the restore procedure do not throw an error when the data folder is not present.
* Allow restart, minor version upgrade and security upgrade to have primaryInstance and initialInstances set to null.

### Ready to Experience this new release?

We strongly encourage you to install or upgrade and explore the power it brings. Don't wait – [install StackGres](https://stackgres.io/install/) today.
