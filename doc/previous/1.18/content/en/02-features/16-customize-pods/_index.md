---
title: Customize Pods
weight: 16
url: /features/customize-pods
description: Customize Pods adding containers, env vars, volumes, volume mounts and ports
---

StackGres allows customizing Pods by adding containers or init containers, environment variables, volumes, volume mounts and ports. This feature provides the liberty to extend any StackGres cluster and fit any use case as needed.

You can configure the pods of the cluster in the [SGCluster CRD pods section]({{% relref "06-crd-reference/01-sgcluster#sgclusterspecpodsupdatestartegy" %}}) where you can configure the following related sections:

* `customContainers`: Custom containers
* `customInitContainers`: Custom init containers
* `customVolumes`: Custom volumes
* `customVolumeMounts`: Custom volume mounts for containers
* `customInitVolumeMounts`: Custom volume mounts for init containers
* `customEnv`: Custom environment variables for containers
* `customInitEnv`: Custom environment variables for init containers
* `customEnvFrom`: Custom environment variables from source for containers
* `customInitEnvFrom`: Custom environment variables from source for init containers

You can configure the ports of the cluster in the [SGCluster CRD primary postgresServices section]({{% relref "06-crd-reference/01-sgcluster#sgclusterspecpostgresservicesprimary" %}}) and the [SGCluster CRD replicas postgresServices section]({{% relref "06-crd-reference/01-sgcluster#sgclusterspecpostgresservicesreplicas" %}}).
