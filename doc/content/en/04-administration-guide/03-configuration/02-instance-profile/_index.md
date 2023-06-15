---
title: Instance Profile
weight: 2
url: /administration/configuration/instance
aliases: [ /tutorial/complete-cluster/instance-profile ]
---

An Instance Profile is an abstraction over the resource characteristics of an instance (basically, as of today, CPU "cores" and RAM).
StackGres represents such a profile with the CRD [SGInstanceProfile]({{% relref "06-crd-reference/02-sginstanceprofile" %}}).
You can think of instance profiles as "t-shirt sizes", a way to create named t-shirt sizes (such as S, M, L), that you will reference when you create your clusters.
It is a way to enforce best practices by using standardized instance sizes.

The `SGInstanceProfile` is referenced from one or more Postgres clusters.

This is an example config definition:

```yaml
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  namespace: demo
  name: size-small
spec:
  cpu: "4"
  memory: "8Gi"
```

This definition is created in Kubernetes (e.g. using `kubectl apply`) and can be inspected (`kubectl describe sginstanceprofile size-small`) like any other Kubernetes resource.

You may create other instance profiles with other sizes if you wish.

An instance profile enforces resource requests and limits for the container where Patroni and Postgres will set the Pod resource using the `cpu` and `memory` values for both requests and limits.
It also enforces resource requests for all the other containers under the section `.spec.containers` and `.spec.initContainers`.
Those sections contain the default values specified by `cpu` and `memory`, and can be tuned later depending on the requirements of your particular use case.

StackGres clusters can reference this configuration as follows:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: demo
  name: cluster
spec:
# [...]
  sgInstanceProfile: 'size-small'
```