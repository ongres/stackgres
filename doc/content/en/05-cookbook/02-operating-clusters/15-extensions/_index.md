---
title: Managing Postgres extensions
weight: 15
url: /cookbook/operating-clusters/extensions
description: Deploy and upgrade PostgreSQL extensions declaratively.
showToc: true
---

## What it does

Declares which PostgreSQL extensions should be deployed on a running cluster. You add or
remove entries in `spec.postgres.extensions[]` on an existing
[SGCluster]({{% relref "06-crd-reference/01-sgcluster" %}}), and the operator downloads and
installs the requested extensions from the StackGres extensions catalog without re-creating
the cluster.

## When to use it

- You want to add functionality to PostgreSQL (for example `pg_stat_statements`,
  `pg_partman`, `postgis`, or `timescaledb`) after the cluster is already running.
- You need to pin a specific extension version for compatibility, or upgrade an extension to
  a newer release.
- You are standardizing which extensions are available across multiple clusters by declaring
  them in your YAML manifests.

## How to do it

Patch the running SGCluster to declare the desired extensions. Every entry requires only
`name`; `version` and `publisher` are optional and default to the latest release from
`com.ongres` when omitted:

```yaml
apiVersion: stackgres.io/v1
kind: SGCluster
metadata:
  namespace: my-cluster
  name: cluster
spec:
  instances: 3
  postgres:
    version: "16"
    extensions:
    - name: pg_stat_statements     # always built-in; listed here for clarity
    - name: pg_partman             # latest version, default publisher
    - name: postgis
      version: "3.4.2"            # pin to a specific release
    - name: timescaledb
      publisher: com.ongres       # explicit publisher (same as default)
  pods:
    persistentVolume:
      size: 10Gi
```

Apply the change:

```bash
kubectl apply -f cluster.yaml
```

Or patch in place without a full manifest:

```bash
kubectl patch sgcluster cluster -n my-cluster --type merge -p '
spec:
  postgres:
    extensions:
    - name: pg_partman
    - name: postgis
      version: "3.4.2"
'
```

Monitor reconciliation:

```bash
kubectl get sgcluster cluster -n my-cluster -o jsonpath='{.status.conditions}' | jq .
```

Once the extension is deployed, activate it in each database where it is needed:

```sql
CREATE EXTENSION pg_partman;
CREATE EXTENSION postgis;
```

## How it works

The operator watches `spec.postgres.extensions[]` for additions and removals. When a new
entry appears it:

1. Looks up the matching package in the StackGres extensions catalog (filtered by `name`,
   `version`, and `publisher`).
2. Downloads the extension package to each Pod that does not yet have it.
3. Installs the shared library and control files so Postgres can load it.

The resolved download coordinates are written to `spec.toInstallPostgresExtensions[]` —
a field managed entirely by the operator (do not edit it manually). After deployment the
extension appears in `status.extensions[]` and in
`status.podStatuses[*].installedPostgresExtensions[]`.

## What to expect

- Most extensions are deployed without a Pod restart. After the operator reconciles, the
  extension is available for `CREATE EXTENSION` immediately.
- Extensions that add an entry to `shared_preload_libraries` (for example `timescaledb`,
  `pg_cron`, `citus`) require a cluster restart before they become active. StackGres will
  indicate this in the cluster status; use an SGDbOps restart operation to perform it in a
  controlled, rolling fashion.
- Inspect what is currently installed on each Pod:

  ```bash
  kubectl get sgcluster cluster -n my-cluster \
    -o jsonpath='{.status.podStatuses[*].installedPostgresExtensions}' | jq .
  ```

## Pitfalls

- **Only catalog extensions are supported.** Only extensions published in the StackGres
  extensions catalog can be installed this way. Custom or privately built `.so` files cannot
  be injected through `spec.postgres.extensions[]`.
- **`CREATE EXTENSION` is still required.** Deploying an extension makes its files available
  on disk, but it does not activate it in any database. You must run `CREATE EXTENSION
  <name>` (or `CREATE EXTENSION IF NOT EXISTS <name>`) in every database that needs it.
- **`shared_preload_libraries` extensions need a restart.** Extensions that hook into
  Postgres at startup will not function until the cluster is restarted. Applying the YAML
  change alone is not sufficient; plan a maintenance window or use an SGDbOps restart.
- **Removing an extension requires a restart to take full effect.** After you remove an
  entry from `spec.postgres.extensions[]` the extension remains available until the cluster
  is restarted, because the shared library is already loaded by running Postgres processes.
- **Do not edit `toInstallPostgresExtensions`.** That field is filled by the operator and
  will be overwritten on the next reconciliation loop. Manage extensions exclusively through
  `spec.postgres.extensions[]`.
- **Version pinning vs. catalog availability.** If you pin a `version` that is no longer
  available in the catalog for your Postgres major version, reconciliation will stall with
  an error in the cluster status. Check the StackGres extensions catalog for available
  versions before pinning.
