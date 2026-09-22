---
title: Upgrade check
weight: 15
url: /administration/database-operations/upgrade-check
description: Detecting available component updates
showToc: true
---

StackGres continuously checks whether newer components are available for a cluster and advertises this through the cluster status, so you can tell when a minor upgrade, a major upgrade or an extension upgrade is possible without manually comparing version catalogs.

When a newer Postgres minor or major version, or a newer version of any of the requested extensions, is available, the operator sets a status condition of type `ComponentsUpdated` on the `SGCluster`. This condition signals that one or more components of the cluster could be updated. The Web Console surfaces it as a warning on the affected cluster.

In addition to the condition, the operator fills in the following status fields, each of which is only present when a newer version than the one currently requested is actually available:

- **`SGCluster.status.latestPostgresMinor`** (string): set only when a newer Postgres minor version is available for the cluster's current major version. Its value is the latest available minor version you can upgrade to using a [minor version upgrade]({{% relref "04-administration-guide/06-database-operations/06-minor-version-upgrade" %}}).
- **`SGCluster.status.latestPostgresMajor`** (string): set only when a newer Postgres major version is available. Its value is the latest available major version you can upgrade to using a [major version upgrade]({{% relref "04-administration-guide/06-database-operations/07-major-version-upgrade" %}}).
- **`SGCluster.status.<extensions>[].latest`** (string): set per-extension, only when a newer version of that extension than the requested one is available. Its value is the latest available version of the extension.

You can inspect these fields with `kubectl` to know exactly which upgrades are available:

```bash
❯ kubectl get sgcluster.stackgres.io demo -o yaml
```

```yaml
status:
  conditions:
  - type: ComponentsUpdated
    status: "False"
    reason: NotLatestMinor-NotLatestMajor-AvailableExtensionsUpgrade
    message: PostgreSQL 16.2 is not the latest minor version for major 16 (latest is 16.3).
      A newer major version 17.1 is available. The following 1 extension can be upgraded:
      pg_stat_statements (1.10->1.11).
    lastTransitionTime: "2026-06-05T10:00:00Z"
  latestPostgresMinor: "16.3"
  latestPostgresMajor: "17.1"
  extensions:
  - name: pg_stat_statements
    version: "1.10"
    latest: "1.11"
```

In the example above the cluster can be moved to Postgres minor version `16.3`, can be upgraded to major version `17.1`, and the `pg_stat_statements` extension has a newer `1.11` version available. If any of these fields is absent, no newer version is available for that component. Once you perform the corresponding upgrade so that the cluster runs the latest available versions, the condition becomes `status: "True"` with reason `UpToDate` and the `latest*` fields are removed.

The `status` of the condition answers "is this cluster running the latest minor version?": it is `"True"` when the cluster is on the latest minor version of its major (even if a newer major version or newer extension versions are available), and `"False"` when a newer minor version can be applied. The `reason` details which of the three kinds of update are available.

## `ComponentsUpdated` is not `PendingUpgrade`

`SGCluster` and `SGShardedCluster` carry a second, unrelated condition named `PendingUpgrade`. The two are easy to confuse, so it is worth spelling out the difference:

| | `ComponentsUpdated` | `PendingUpgrade` |
|---|---|---|
| What it is about | The Postgres minor/major version and the extension versions of the cluster | The version of the **StackGres operator** that created the cluster resources |
| Set on | `SGCluster` only | `SGCluster` and `SGShardedCluster` |
| Meaning of `status: "True"` | The cluster runs the latest Postgres minor version | The cluster still runs resources created by an **older operator version** |
| How it is cleared | A [minor version upgrade]({{% relref "04-administration-guide/06-database-operations/06-minor-version-upgrade" %}}), [major version upgrade]({{% relref "04-administration-guide/06-database-operations/07-major-version-upgrade" %}}) or extension upgrade | A [security upgrade]({{% relref "04-administration-guide/06-database-operations/04-security-upgrade" %}}) (`SGDbOps`/`SGShardedDbOps` with `op: securityUpgrade`) |

In particular, a `restart` or a `minorVersionUpgrade` operation does **not** clear `PendingUpgrade`: only a security upgrade does, because only a security upgrade rewrites the cluster resources with the running operator version. A `PendingUpgrade` condition that stays `True` across restarts and minor version upgrades is therefore not stale — it is the operator telling you that a security upgrade has not been run yet. The condition message names the operator version the resources were created with and the security upgrade that clears it.

On a `SGShardedCluster` the `PendingUpgrade` condition is aggregated: it is `True` when the `SGShardedCluster` itself **or any of its child `SGCluster`s** was created by an older operator version.

