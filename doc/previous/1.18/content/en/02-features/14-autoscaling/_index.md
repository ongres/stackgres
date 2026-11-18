---
title: Automatic scaling
weight: 14
url: /features/autoscaling
description: Automatic scaling via KEDA and vertical Pod autoscaler
---

StackGres offers an integration that allows configuring automatic scaling of instances both horizontally (number of replicas) and vertically (CPU and memory).

Horizontal automatic scaling is based on [KEDA](https://keda.sh/) that extends the [HorizontalPodAutoscaler](https://kubernetes.io/docs/concepts/workloads/autoscaling/#scaling-workloads-horizontally) in order to scale the number of read only instances based on active connections statistics from the database itself.

Vertical automatic scaling is implemented through the [VerticalPodAutoscaler](https://kubernetes.io/docs/concepts/workloads/autoscaling/#scaling-workloads-vertically) that depending on usage on each instance will allow to increase or decrease the CPU and memory resource requests and limits and thus re-create an instance in a node with more or less resources.

> *IMPORTANT*: Note that using vertical autoscaling will disrupt the database service when the scaling is performed on the primary. High availability will minimize this disruption but your application must be prepared if you decide to use such functionality. 

You can configure the automatic scaling of the cluster in the [SGCluster CRD autoscaling section]({{% relref "06-crd-reference/01-sgcluster#sgclusterspecautoscaling" %}}).
