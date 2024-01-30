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

## Resources

When an SGInstanceProfile is created specifying only `cpu` and `memory` fields under section `SGInstanceProfile.spec` other sections will be created assigning values based on those:

```yaml
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  namespace: demo
  name: size-small
spec:
  cpu: "4"
  memory: 8Gi
  containers:
    backup.create-backup:
      cpu: "1"
      memory: 256Mi
    cluster-controller:
      cpu: 250m
      memory: 512Mi
    dbops.run-dbops:
      cpu: "1"
      memory: 256Mi
    dbops.set-dbops-result:
      cpu: "1"
      memory: 256Mi
    distributedlogs-controller:
      cpu: 250m
      memory: 512Mi
    envoy:
      cpu: "1"
      memory: 64Mi
    fluent-bit:
      cpu: 250m
      memory: 64Mi
    fluentd:
      cpu: "1"
      memory: 2Gi
    pgbouncer:
      cpu: 250m
      memory: 64Mi
    postgres-util:
      cpu: 250m
      memory: 64Mi
    prometheus-postgres-exporter:
      cpu: 250m
      memory: 256Mi
  initContainers:
    cluster-reconciliation-cycle:
      cpu: "4"
      memory: 8Gi
    dbops.set-dbops-running:
      cpu: "1"
      memory: 256Mi
    distributedlogs-reconciliation-cycle:
      cpu: "4"
      memory: 8Gi
    major-version-upgrade:
      cpu: "4"
      memory: 8Gi
    pgbouncer-auth-file:
      cpu: "4"
      memory: 8Gi
    relocate-binaries:
      cpu: "4"
      memory: 8Gi
    reset-patroni:
      cpu: "4"
      memory: 8Gi
    setup-arbitrary-user:
      cpu: "4"
      memory: 8Gi
    setup-scripts:
      cpu: "4"
      memory: 8Gi
  requests:
    cpu: "4"
    memory: 8Gi
    containers:
      backup.create-backup:
        cpu: "1"
        memory: 256Mi
      cluster-controller:
        cpu: 250m
        memory: 512Mi
      dbops.run-dbops:
        cpu: "1"
        memory: 256Mi
      dbops.set-dbops-result:
        cpu: "1"
        memory: 256Mi
      distributedlogs-controller:
        cpu: 250m
        memory: 512Mi
      envoy:
        cpu: "1"
        memory: 64Mi
      fluent-bit:
        cpu: 250m
        memory: 64Mi
      fluentd:
        cpu: "1"
        memory: 2Gi
      pgbouncer:
        cpu: 250m
        memory: 64Mi
      postgres-util:
        cpu: 250m
        memory: 64Mi
      prometheus-postgres-exporter:
        cpu: 250m
        memory: 256Mi
    initContainers:
      cluster-reconciliation-cycle:
        cpu: "4"
        memory: 8Gi
      dbops.set-dbops-running:
        cpu: "1"
        memory: 256Mi
      distributedlogs-reconciliation-cycle:
        cpu: "4"
        memory: 8Gi
      major-version-upgrade:
        cpu: "4"
        memory: 8Gi
      pgbouncer-auth-file:
        cpu: "4"
        memory: 8Gi
      relocate-binaries:
        cpu: "4"
        memory: 8Gi
      reset-patroni:
        cpu: "4"
        memory: 8Gi
      setup-arbitrary-user:
        cpu: "4"
        memory: 8Gi
      setup-scripts:
        cpu: "4"
        memory: 8Gi
```

This allows unexperienced users to create an SGInstanceProfile without requiring much knowledge on the usage of all the containers.

You may set any value of `cpu` and `memory` in any of the above sections to `null` in order to remove the corresponding resources limits or requests assignment.

### Resources limits

By default the SGInstanceProfile `cpu` and `memory` fields under section `SGInstanceProfile.spec` will be assigned as resources limits to the `patroni` container.
Other containers will not receive any resources limits unless the `SGCluster.spec.pods.resources.enableClusterLimitsRequirements` is set to `true`. In such case each container (and init container) will be assigned the resources limits of the corresponding value of fields `cpu` and `memory` specified in section `SGInstanceProfile.spec.containers.<container name>` (and `SGInstanceProfile.spec.initContainers.<container name>`).

For example without setting `SGCluster.spec.pods.resources.enableClusterLimitsRequirements` a Pod resources limits would look like:

```
$ kubectl get pod -n demo cluster-0 --template '{{ range .spec.containers }}{{ printf "%s:\n  limits: %s\n\n" .name .resources.limits }}{{ end }}'
patroni:
  limits: map[cpu:4 memory:8Gi]

envoy:
  limits: %!s(<nil>)

pgbouncer:
  limits: %!s(<nil>)

prometheus-postgres-exporter:
  limits: %!s(<nil>)

postgres-util:
  limits: %!s(<nil>)

fluent-bit:
  limits: %!s(<nil>)

cluster-controller:
  limits: %!s(<nil>)
```

While with setting `SGCluster.spec.pods.resources.enableClusterLimitsRequirements` to `true` will look like:

```
$ kubectl get pod -n demo cluster-0 --template '{{ range .spec.containers }}{{ printf "%s:\n  limits: %s\n\n" .name .resources.limits }}{{ end }}'
patroni:
  limits: map[cpu:4 memory:8Gi]

envoy:
  limits: map[cpu:1 memory:64Mi]

pgbouncer:
  limits: map[cpu:250m memory:64Mi]

prometheus-postgres-exporter:
  limits: map[cpu:250m memory:256Mi]

postgres-util:
  limits: map[cpu:250m memory:64Mi]

fluent-bit:
  limits: map[cpu:250m memory:64Mi]

cluster-controller:
  limits: map[cpu:250m memory:512Mi]
```

### Resources requests

SGInstanceProfile `cpu` and `memory` fields under section `SGInstanceProfile.spec.requests` will be assigned as the total resources requests assigned to the all the container of the Pod. Each container (and init container) will be assigned the resources requests of the corresponding value of fields `cpu` and `memory` specified in section `SGInstanceProfile.spec.requests.containers.<container name>` (and `SGInstanceProfile.spec.requests.initContainers.<container name>`). The only exception is the `patroni` container which resources requests values will be the values of `cpu` and `memory` fields under section `SGInstanceProfile.spec.requests` minus the sum of `cpu` and `memory` specified in section `SGInstanceProfile.spec.requests.containers.<container name>` for each container that will be created by the operator depending on SGCluster configuration.

If `SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal` is set to `true` then `cpu` and `memory` fields under section `SGInstanceProfile.spec.requests` will be assigned as resources requests to the `patroni` container.

For example without setting `SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal` a Pod resources limits would look like:

```
$ kubectl get pod -n demo cluster-0 --template '{{ range .spec.containers }}{{ printf "%s:\n  limits: %s\n\n" .name .resources.requests }}{{ end }}'
patroni:
  limits: map[cpu:1750m memory:7Gi]

envoy:
  limits: map[cpu:1 memory:64Mi]

pgbouncer:
  limits: map[cpu:250m memory:64Mi]

prometheus-postgres-exporter:
  limits: map[cpu:250m memory:256Mi]

postgres-util:
  limits: map[cpu:250m memory:64Mi]

fluent-bit:
  limits: map[cpu:250m memory:64Mi]

cluster-controller:
  limits: map[cpu:250m memory:512Mi]
```

While with setting `SGCluster.spec.pods.resources.disableResourcesRequestsSplitFromTotal` to `true` will look like:

```
$ kubectl get pod -n demo cluster-0 --template '{{ range .spec.containers }}{{ printf "%s:\n  limits: %s\n\n" .name .resources.limits }}{{ end }}'
patroni:
  limits: map[cpu:4 memory:8Gi]

envoy:
  limits: map[cpu:1 memory:64Mi]

pgbouncer:
  limits: map[cpu:250m memory:64Mi]

prometheus-postgres-exporter:
  limits: map[cpu:250m memory:256Mi]

postgres-util:
  limits: map[cpu:250m memory:64Mi]

fluent-bit:
  limits: map[cpu:250m memory:64Mi]

cluster-controller:
  limits: map[cpu:250m memory:512Mi]
```

In the latter case the total accounting of `cpu` resources requests for the Pod would be `6250m` instead of `4` and for `memory` would be of `9Gi` instead of `8Gi`.

## Huge Pages

Huge pages can be configured for the `patroni` container by setting the value of `hugepages-1Gi` or `hugepages-2Mi` (for huge pages of `1Gi` or `2Mi` respectively).

> Make sure that the total amount of memory requested for huge pages do not surpass the total memory resources limits.

For example to specify 8 huge pages of `1Gi`:

```yaml
apiVersion: stackgres.io/v1
kind: SGInstanceProfile
metadata:
  namespace: demo
  name: size-small
spec:
  cpu: "4"
  memory: 8Gi
  hugePages:
    hugepages-1Gi: 8Gi
```

## Custom containers

Any custom container (or init cunstom containers) resources limits and huge pages can be configured by creating a section `SGInstanceProfile.spec.containers.custom-<custom container name>` (or `SGInstanceProfile.spec.initContainers.custom-<custom init container name>`) and specifying `cpu`, `memory` and/or `hugePages`.