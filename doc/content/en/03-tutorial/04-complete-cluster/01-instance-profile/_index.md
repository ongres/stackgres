---
title: Instance Profile
weight: 1
url: tutorial/complete-cluster/instance-profile
description: Details about how to create the instances profiles.
---

An Instance Profile is an abstraction over the resource characteristics of an instance (basically, as of today, CPU
"cores" and RAM). It is represented in StackGres with the CRD
[SGInstanceProfile]({{% relref "06-crd-reference/02-sginstanceprofile" %}}). You can think of instance profiles as
"t-shirt sizes", a way to create named t-shirt sizes, that you will reference when you create your clusters. It is a way
to enforce best practices by using standardized instance sizes.

Create the following file: `sginstanceprofile-small.yaml`:

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

and deploy it to Kubernetes:

```
kubectl apply -f sginstanceprofile-small.yaml
```

You may create other instance profiles with other sizes if you wish.

You can list the created (available) instance profiles from the web console or via:

```
$ kubectl -n demo get sginstanceprofiles
```

An instance profile enforces resource requests and limits for the container where Patroni and Postgres will set the Pod resource using the `cpu` and `memory` values for both requests and limits.
It also enforces resource requests for all the other containers under the section `.spec.containers` and `.spec.initContainers`.
Those sections contain the default values specified by `cpu` and `memory`, and can be tuned later depending on the requirements of your particular use case.
You may use `kubectl describe` on the created resource to inspect the values that are injected (tuned by default):

```
kubectl -n demo describe sginstanceprofile size-small
```

```
Name:         size-small
Namespace:    demo
Labels:       <none>
Annotations:  stackgres.io/operatorVersion: 1.3.3
API Version:  stackgres.io/v1
Kind:         SGInstanceProfile
Metadata:
  Creation Timestamp:  2022-10-26T10:36:53Z
  Generation:          1
  Managed Fields:
    API Version:  stackgres.io/v1
    Fields Type:  FieldsV1
    fieldsV1:
      f:spec:
        .:
        f:cpu:
        f:memory:
    Manager:         kubectl
    Operation:       Update
    Time:            2022-10-26T10:36:53Z
  Resource Version:  2595
  UID:               fb56789f-24a7-4a57-9722-817ca6114e67
Spec:
  Containers:
    backup.create-backup:
      Cpu:     1
      Memory:  256Mi
    Cluster - Controller:
      Cpu:     250m
      Memory:  512Mi
    dbops.run-dbops:
      Cpu:     1
      Memory:  256Mi
    dbops.set-dbops-result:
      Cpu:     1
      Memory:  256Mi
    Distributedlogs - Controller:
      Cpu:     250m
      Memory:  512Mi
    Envoy:
      Cpu:     1
      Memory:  64Mi
    Fluent - Bit:
      Cpu:     250m
      Memory:  64Mi
    Fluentd:
      Cpu:     1
      Memory:  2Gi
    Pgbouncer:
      Cpu:     250m
      Memory:  64Mi
    Postgres - Util:
      Cpu:     250m
      Memory:  64Mi
    Prometheus - Postgres - Exporter:
      Cpu:     250m
      Memory:  256Mi
  Cpu:         4
  Init Containers:
    Cluster - Reconciliation - Cycle:
      Cpu:     4
      Memory:  8Gi
    dbops.set-dbops-running:
      Cpu:     1
      Memory:  256Mi
    Distributedlogs - Reconciliation - Cycle:
      Cpu:     4
      Memory:  8Gi
    Major - Version - Upgrade:
      Cpu:     4
      Memory:  8Gi
    Pgbouncer - Auth - File:
      Cpu:     4
      Memory:  8Gi
    Relocate - Binaries:
      Cpu:     4
      Memory:  8Gi
    Reset - Patroni:
      Cpu:     4
      Memory:  8Gi
    Setup - Arbitrary - User:
      Cpu:     4
      Memory:  8Gi
    Setup - Scripts:
      Cpu:     4
      Memory:  8Gi
  Memory:      8Gi
Events:        <none>
```
