+++
title = "StackGres 1.9: Scale to zero and external DCS (multi-k8s cluster)"
date = 2024-04-12T10:57:02+02:00
author = ["jorge"]
tags = ["postgres", "kubernetes", "downscale", "release", "multi-cluster"]
# planet = "jorsol"
draft = false
thumbnail = "img/blog/StackGres_1-0-0.jpg"
description = "StackGres is an Open Source operator for running production-ready Postgres on Kubernetes."
+++

## StackGres 1.9: New Features for Scaling, Management, and Flexibility

In this post we will talk about StackGres 1.9.0 – a release packed with features that will transform your Postgres experience!

### TL;DR

* The ability to downscale to 0 instances  (so called “scale to zero”).
* Support for scaling instances using HorizontalPodAutoscaler or KEDA.
* The ability to use an external DCS (like etcd, ZooKeeper and others).
* New User Management support for the Web Console.

You can find the full changelog for this version in our GitLab [release page](https://gitlab.com/ongresinc/stackgres/-/releases/1.9.0).

### What's new?

#### Enhanced Stability: Latest Stable PostgreSQL Versions Supported

Our core is PostgreSQL, so we always offer the latest stable updates from PostgreSQL. This ensures you have access to the most recent features and bug fixes while maintaining a reliable database environment.

In this release we added support for PostgreSQL version 12.18, 13.14, 14.11, 15.6 and 16.2, also we provide an updated PgBouncer with version 1.22.1 to deliver a highly reliable and performant database experience.

![PostgreSQL minior versions](/img/blog/SG_blog-StackGres_1-9-PostgresUpdates.png)

#### Effortless Scaling with Flexibility at Its Finest

* Scale to Zero: StackGres 1.9.0 empowers you to downscale your cluster to zero instances when not in use. This translates into cost savings and optimized resource allocation, especially for workloads with variable usage patterns. Many users manage development and testing clusters that can consume significant resources when idle. This update allows them to avoid wasting resources.

![Downscale](/img/blog/SG_blog-StackGres_1-9-Downscale.png)

* Horizontal Pod Autoscaler (HPA): StackGres 1.9.0 lays the groundwork for seamless integration with [Horizontal Pod Autoscaler (HPA)](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) and [Kubernetes Event-Driven Autoscaler (KEDA)](https://keda.sh/). This future-proof approach allows for automatic scaling based on predefined metrics, ensuring your cluster dynamically adapts to meet fluctuating workloads.

* These features leverage the use of the scale subresource, so you now can also easily scale your sgclusters with kubectl too:

![kubectl scale to zero](/img/blog/SG_blog-StackGres_1-9-kubectl-scale.png)

#### External DCS: Full control over Postgres HA, including multi-region

StackGres uses Patroni as HA solution, and by default it uses the DCS provided by Kubernetes (etcd), it serves pretty well for many use cases, and requires no aditional user configuration. But for advanced setups, it could be limited and also put pressure on the internal Kubernetes DCS.

StackGres 1.9.0 introduces compatibility to connect Patroni with an external Distributed Configuration Store (DCS) like `etcd` and `ZooKeeper` during patroni initialization. This reduce the load of the K8s DCS.

The ability to leverage external DCS paves the way for establishing multi-kubernetes cluster deployments. Now you could manage your StackGres instances across multiple Kubernetes clusters.

In the below example snippet two separate SGCluster instances deployed in separate Kubernetes instances will share the same DCS instance (an etcd server separated from the one used by Kubernetes) in order to form a single cluster of Postgres instances where only one instance among the instances of both SGCluster will be promoted as the leader:

* Near cluster

```
kind: SGCluster
metadata:
  name: near
spec:
  configurations:
    patroni:
      initialConfig:
        scope: multi-k8s-cluster
        etcd3:
          host: etcd
          username: etcd
          password: etcd
    credentials: ... # all credentials will point to a Secret with same value for both clusters
```

* Far cluster

```
kind: SGCluster
metadata:
  name: far
spec:
  configurations:
    patroni:
      initialConfig:
        scope: multi-k8s-cluster
        etcd3:
          host: etcd
          username: etcd
          password: etcd
    credentials: ... # all credentials will point to a Secret with same value for both clusters
```

In the above example both clusters must share the same credentials and an external etcd server must be already deployed. Also the two Kubernetes clusters must be connected at the network level exposing the same CIDR used by both clusters Pods in order for each Patroni instance to be able to communicate with all the other Patroni instances.

#### Web Console User Management

StackGres 1.9.0 introduces a feature for User Management on the Web Console! Empower your team by creating, managing, and assigning user permissions within the Web Console itself. This streamlines access control and enhances overall security.

![Web Console - User Management](/img/blog/SG_blog-StackGres_1-9-user-webconsole.png)

### Ready to Experience this new release?

We strongly encourage you to install or upgrade and explore the power it brings. Don't wait – [install StackGres](https://stackgres.io/install/) today.
