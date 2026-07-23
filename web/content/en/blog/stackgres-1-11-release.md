+++
title = "StackGres 1.11: Scoped namespaces operation"
date = 2024-06-17T10:57:02+02:00
author = ["jorge"]
tags = ["postgres", "kubernetes", "namespaced", "release"]
# planet = "jorsol"
draft = false
thumbnail = "img/blog/StackGres_1-0-0.jpg"
description = "StackGres is an Open Source operator for running production-ready Postgres on Kubernetes."
+++

## StackGres 1.11: Scoped namespaces operation

In this post we will talk about StackGres 1.11.0 – a release with better security with scoped namespace operation functionality! Let's dive into the highlights.

### TL;DR

* Scoped namespaces, now you can also specify the list of namespaces where the operator will be able to work with.
* New and updated images including minor versions across the board.
* Support for Kubernetes 1.30.

You can find the full changelog for this version in our GitLab [release page](https://gitlab.com/ongresinc/stackgres/-/releases/1.11.0).

### What's new?

#### Scoped namespaces operation

StackGres now allows to specify a list of namespaces where the operator will be able to work with and improve the security by limiting where StackGres can act. It also offers the ability to disable `ClusterRoles` for the operator completely (with limitations in functionalities). With the exception of a `ClusterRole` for the Web Console / REST API if you want to still enable it.

This feature also allows Operator scoping with OperatorGroups when used with OperatorHub (OLM). Read more about this here: https://olm.operatorframework.io/docs/advanced-tasks/operator-scoping-with-operatorgroups/

To configure this using Helm set:
* `allowedNamespaces` or `allowedNamespaceLabelSelector` for namespace scoping.
* `disableClusterRole` to disable ClusterRoles
  * `allowImpersonationForRestApi` to enable just ClusterRole to allow the REST API to continue functioning.

Example install with three namespaces (default, dev, prod):

```bash
helm install --create-namespace --namespace stackgres \
  --set allowedNamespaces[0]=default \
  --set allowedNamespaces[1]=dev \
  --set allowedNamespaces[2]=prod \
  stackgres-operator stackgres-charts/stackgres-operator \
  --version 1.11.0
```

This restriction on the namespaces handling provides more security and is based on label selection on the namespaces.

#### Latest PostgreSQL Versions Supported

We added PostgreSQL 16.3, 15.7, 14.12, 13.15, and 12.19 minor releases to ensure you have access to the most recent features and bug fixes while maintaining a reliable database environment.

![PostgreSQL minior versions](/img/blog/SG_blog-StackGres_1-11-PostgresUpdates.png)

> PostgreSQL 12 will stop receiving fixes on November 14, 2024. If you are running PostgreSQL 12 in a production environment, we suggest that you make plans to upgrade to a newer, supported version of PostgreSQL.

#### Updated packages

Many binaries and packages are updated improving the security and stability of the platform like wal-g 3.0.1, pg_activity 3.5.1, usql 0.19.2, and others.

#### Support for Kubernetes 1.30

Now StackGres can also be installed on Kubernetes 1.30, and we offer community support on this version.

![Kubernetes 1.30](/img/blog/k8s-1.30.png)

* https://kubernetes.io/blog/2024/04/17/kubernetes-v1-30-release/

### Ready to Experience this new release?

We strongly encourage you to install or upgrade and explore the power it brings. Don't wait – [install StackGres](https://stackgres.io/install/) today.
