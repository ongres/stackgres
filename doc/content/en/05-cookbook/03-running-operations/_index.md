---
title: Running operations
weight: 3
url: /cookbook/running-operations
description: Recipes for day-two operations performed on a cluster through SGDbOps.
showToc: true
---

The recipes in this section use the [SGDbOps]({{% relref "06-crd-reference/08-sgdbops" %}})
custom resource to perform **operations** against a running cluster: restarts, version
upgrades, benchmarks, and maintenance like `vacuum` and `pg_repack`.

Unlike SGCluster fields, an SGDbOps is not a desired-state knob you edit — it is a
**one-shot job**. You create an SGDbOps that names the target cluster and the operation to
run; the operator executes it and records the outcome in the resource's `status`. The
resource stays around afterwards as a record of what happened.

{{% children style="li" depth="1" description="true" %}}

## Anatomy of an SGDbOps

Every operation shares the same envelope:

```yaml
apiVersion: stackgres.io/v1
kind: SGDbOps
metadata:
  namespace: my-cluster
  name: my-operation
spec:
  sgCluster: cluster        # the SGCluster to operate on
  op: restart               # which operation to run
  # ...operation-specific configuration
```

The `op` field selects the operation and which configuration block applies. Common
execution controls — scheduling the run for later, timeouts, and retries — are shared
across all operations and are covered in
[Scheduling, timeouts and retries]({{% relref "05-cookbook/03-running-operations/08-execution-control" %}}).

## What to expect

- Operations run asynchronously. Watch progress and the result in the resource status:

  ```bash
  kubectl get sgdbops -n my-cluster my-operation -o yaml
  ```

- Some operations (restart, upgrades, repack) cause a rolling restart or brief switchover.
- While an SGDbOps holds the cluster lock, edits to the SGCluster spec are rejected until
  the operation completes.
