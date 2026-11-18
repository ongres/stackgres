---
title: Liveness Probe and Fencing
weight: 5
url: /administration/patroni/fencing
description: How StackGres uses Kubernetes probes to fence Postgres when Patroni becomes unresponsive.
showToc: true
---

To prevent split brain scenarios, a Patroni instance that fails to renew the leader lock fences Postgres before any leader election may happen. This strict time ordering, layered on top of the linearizable properties of the underlying DCS, underpins split brain prevention as long as Patroni itself fences Postgres.

But what if Patroni becomes wholly unresponsive (process suspension or REST-server starvation) while Postgres remains writable? This could actually prevent Patroni fencing Postgres. To prevent this from happening StackGres also fences Postgres through the Kubernetes [liveness probe](https://kubernetes.io/docs/concepts/workloads/pods/probes/) of the `patroni` container. If the probe fails, the kubelet restarts the container, before another cluster member can acquire leadership. The default configuration is budgeted to complete this ordering with a nominal five-second margin over a conservative time budget, under the explicit assumption that the kubelet and the container runtime remain healthy and responsive (node-level watchdogs, covered at the end of this page, bound the exposure when that assumption fails). This budget applies to faults that make the `/liveness` endpoint stop responding promptly; see [What this fences, and what it does not](#what-this-fences-and-what-it-does-not) for the boundary. If you override any of the involved parameters, you must keep the margin inequality below satisfied.


## The liveness probe acts as a Kubernetes-native watchdog

One of Patroni's main tasks, if it cannot renew the leader lock, is to step down and demote (fence) Postgres, preventing a split-brain scenario. But if Patroni itself is down or suspended while Postgres remains writable, no demotion happens. Patroni supports a [watchdog](https://patroni.readthedocs.io/en/latest/watchdog.html) for this case, but a watchdog is a non-starter in Kubernetes: it requires elevated privileges, it can be assigned to only one workload per node, and when it fires it reboots the whole node, disrupting co-located workloads.

Instead, StackGres configures a liveness probe against Patroni's [`/liveness` REST endpoint](https://patroni.readthedocs.io/en/latest/rest_api.html) on port 8009. This endpoint is lightweight: it answers from Patroni's in-memory HA-loop state and performs no SQL query or disk health check, so a failing probe indicates a genuinely unresponsive Patroni. When the probe fails `failureThreshold` consecutive times, the kubelet restarts the container, restarting Postgres with it and thereby fencing it.

This mechanism is defense in depth, not a general replacement for independent node fencing: liveness probes can themselves fail to act, for example on an OOM-ed node, and node- or VM-wide suspension may also suspend the kubelet and the container runtime.


## What this fences, and what it does not

The probe budget below is designed around faults that make Patroni's `/liveness` endpoint (port 8009) stop answering **promptly**. It does not fence every conceivable Patroni fault before the leader lock expires. In detail:

- **Fenced within the budget.** Whole-process suspension (`SIGSTOP`), REST-server starvation, a crash, or anything else that makes port 8009 unresponsive: the probe times out and, after `failureThreshold` consecutive failures, the container is restarted. This is the fault class the timing below is budgeted for.
- **Not fenced strictly before expiry: an HA-loop-only wedge with a responsive REST thread.** Patroni's [`/liveness`](https://patroni.readthedocs.io/en/latest/rest_api.html) returns `503` for a primary only once its HA loop has been stale for **more than `ttl`** (Patroni's own source notes this will probably be after the leader key has already expired). If Patroni's HA loop wedges while its REST thread keeps answering `200`, the probe does not start failing until around leader-key expiry, so this mechanism cannot fence that case ahead of expiry. Patroni's own election safeguards (a candidate must observe the leader key as vacated and win the DCS race) remain the backstop here; the liveness probe does not add a fence-before-expiry guarantee for it.
- **Startup window.** While the startup probe has not yet succeeded for the first time, the kubelet keeps the liveness probe disabled (standard Kubernetes behavior). A freeze that lands in that window is bounded by the startup-probe budget (`failureThreshold × periodSeconds`, 900 seconds with the defaults), not by `ttl`. The startup probe is deliberately generous because Patroni can legitimately take a long time to become live (for example when restoring from a backup); if your clusters never have a slow startup you may lower `SGCluster.spec.pods.startupProbe.failureThreshold` or `periodSeconds` to shrink this window.


## Default probe and Patroni timing configuration

The startup probe, the liveness probe, and the Patroni timings can be customized (see [SGCluster.spec.pods.startupProbe]({{% relref "06-crd-reference/01-sgcluster" %}}), [SGCluster.spec.pods.livenessProbe]({{% relref "06-crd-reference/01-sgcluster" %}}) and [SGCluster.spec.configurations.patroni.dynamicConfig]({{% relref "06-crd-reference/01-sgcluster" %}})):

| Setting | Startup probe (default) | Liveness probe (default) |
|---------|-----------------------|--------------------------|
| endpoint | Patroni's `/liveness` on `:8009` | Patroni's `/liveness` on `:8009` |
| periodSeconds | 5 | 5 |
| timeoutSeconds | 2 | 2 |
| failureThreshold | 180 | 3 |
| successThreshold | 1 | 1 |
| terminationGracePeriodSeconds | - | 3 |

| Patroni setting | Default |
|-----------------|---------|
| `ttl` | 45 |
| `loop_wait` | 10 |
| `retry_timeout` | 10 |


## Overrides must satisfy the fencing margin inequality

For the fault class this mechanism fences (see [What this fences, and what it does not](#what-this-fences-and-what-it-does-not)), fencing is budgeted to complete before the leader lock expires only if the following inequality holds:

```
ttl >= loop_wait
     + retry_timeout
     + failureThreshold × periodSeconds
     + timeoutSeconds
     + terminationGracePeriodSeconds (of the liveness probe)
     + safetyMargin
```

With the defaults: `45 >= 10 + 10 + 3×5 + 2 + 3 + 5`, leaving a five-second safety margin. The rationale: the maximum age of the last successful leader-lock renewal is approximately `loop_wait + retry_timeout` (Patroni's own invariant is `ttl >= loop_wait + 2 × retry_timeout`, budgeting two retry timeouts per renewal cycle; one is subtracted here), after which the probe needs up to `failureThreshold × periodSeconds + timeoutSeconds` to declare the container dead, and the container up to `terminationGracePeriodSeconds` to be killed.

Two important reading notes:

- The `failureThreshold × periodSeconds` term is deliberately conservative: it accounts for `(failureThreshold − 1)` inter-probe intervals plus up to one full period of probe-alignment slack, since a Patroni freeze does not land exactly on a probe tick. Do not "optimize" it down to `(failureThreshold − 1) × periodSeconds`.
- StackGres does not validate this inequality. If you override `ttl`, `loop_wait`, or `retry_timeout` (via `SGCluster.spec.configurations.patroni.dynamicConfig`), or any liveness probe timing (via `SGCluster.spec.pods.livenessProbe`), recompute it yourself.

Note that `ttl` was set to `30` before 1.19, and any such default copied over manually may be breaking the inequality.


## Relax the timings under tight CPU limits

The `patroni` container runs Postgres alongside Patroni, typically under CPU limits. Under CPU saturation, Patroni's REST endpoint can plausibly miss a 2-second deadline three times within 15 seconds, causing a false-positive liveness failure: the primary is killed after the 3-second grace period and undergoes crash recovery (which is safe in Postgres, but disruptive at peak load). If your clusters run with tight CPU limits and experience sustained saturation, relax `timeoutSeconds`, `periodSeconds`, or `failureThreshold`, and raise `ttl` together with them so the inequality above still holds.


## Kubernetes 1.25–1.27 requires the ProbeTerminationGracePeriod feature gate

Probe-level `terminationGracePeriodSeconds` is a beta feature enabled by default from Kubernetes 1.25 and GA (always on) from 1.28. On Kubernetes 1.25–1.27, ensure the [`ProbeTerminationGracePeriod` feature gate](https://kubernetes.io/docs/reference/command-line-tools-reference/feature-gates/) has not been disabled: if it is disabled, the probe-level 3-second value is silently ignored and the pod's `terminationGracePeriodSeconds` (60 seconds by default) applies instead, consuming the fencing margin entirely.


## Node-level watchdogs bound kubelet unresponsiveness (Kubernetes 1.32+)

Everything above assumes a responsive kubelet: an unresponsive kubelet neither executes nor enforces the liveness probe, so a simultaneously frozen Patroni (and both freezes could share a single node-level cause, such as severe memory or I/O pressure) may leave a writable Postgres unfenced after the leader lock expires. Two node-level watchdog layers, configured by the node administrator (not by StackGres), mitigate this:

- From Kubernetes 1.32, the kubelet supports a [systemd watchdog](https://kubernetes.io/docs/reference/node/systemd-watchdog/) (beta, `SystemdWatchdog` feature gate, enabled by default): systemd passes the polling period configured with `WatchdogSec` in the kubelet unit down to the kubelet through the standard `WATCHDOG_USEC` environment variable, and restarts the kubelet if it fails to call `sd_notify` within that period.
- systemd implements the analogous mechanism for the host itself (see [`RuntimeWatchdogSec`](https://manpages.debian.org/testing/systemd/systemd-system.conf.5.en.html)): backed by a hardware or software watchdog device, it reboots the host if systemd (PID 1) stops petting the device.

These layers bound the split-brain exposure window; they do not preserve the fence-before-lock-expiry guarantee. With a frozen kubelet, fencing completes only after `WatchdogSec` (detection) plus the kubelet restart plus the probe windows described above — a total that does not fit within the `ttl` budget for realistic `WatchdogSec` values. Without them, however, the window is unbounded (it lasts until the kubelet recovers or the node is manually fenced). Where you control the nodes, configure `WatchdogSec` as low as practical, and a hardware watchdog where available, to minimize that window.
