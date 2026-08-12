# `matriarch-next/` — local matriarch library skeleton (north-star, draft)

Staging skeleton for the **single matriarch library** the north-star calls for
(P2, §3): a **plain-Java core with zero framework dependencies** (no CDI/Quarkus
in the core), meant to replace today's two divergent copies — the OSS standalone
(`../matriarch/`) and the cloud-embedded one. Like `proto-next/`, this is a
**design skeleton for discussion — not yet wired into the OSS reactor build.**

## Shape (P1 / P2 / §3)

- **Desired-state reconciliation engine** (§3.6, P1): domain state splits into a
  durable **desired spec** (the source of truth, persisted *before* provisioning)
  and **observed status** (rebuilt from agents/executors; a restart accelerant). A
  reconcile loop diffs the two and drives the Executor. Crash recovery = reload
  desired + re-observe + reconcile — **no persisted in-flight trackers**.
- **Two SPIs** (§3.2 / §3.3):
  - `StateStore` — pluggable persistence + source of truth (SQLite / etcd /
    ConfigMaps / CRD), optimistic-concurrency CAS for the CRD sync (§3.5),
    idempotency outcomes, the adoption credential, secret-classed fields (§3.7).
  - `Executor` — pluggable provisioning/lifecycle (`slony-linux` / `k8s-stackgres`
    / `k8s-native` / `external`); **level-triggered**.
- **Plain listener events** (no CDI) — the cloud wrapper turns these into
  sequenced `stackgres.control.v1.Event`s; **identity is an explicit parameter**
  (§7.1, P3), never ambient — the core is single-tenant.
- **Thin wrappers later**: a GraalVM-native standalone `main` (bare metal) and a
  Quarkus/CDI adapter (StackGres embedding) wrap this core; neither leaks into it.

## Layout

```
src/main/java/io/stackgres/matriarch/
  Matriarch.java            – the reconciliation engine / facade
  Identity.java             – explicit caller identity (audit + authorizer)
  MatriarchListener.java    – plain event listener (+ nested MatriarchEvent)
  spi/StateStore.java       – persistence SPI (+ nested VersionedCluster)
  spi/Executor.java         – provisioning/lifecycle SPI
  model/{ClusterId,ClusterSpec,ClusterStatus}.java – placeholder domain
```

> The `model/` types are **placeholders**. The real domain model is the strongly
> typed `stackgres.api.v1` proto messages (§3.2); these stubs just mark where those
> plug in. Method bodies throw `UnsupportedOperationException("skeleton")` — this
> is the *shape*, not the implementation.

The `pom.xml` has **zero dependencies on purpose** — it is the concrete statement
of "plain Java, no framework in the core."
