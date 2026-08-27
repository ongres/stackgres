/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterList;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfile;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfileList;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ProfileScanner;
import io.stackgres.matriarch.Matriarch;
import io.stackgres.matriarch.model.Cluster;
import io.stackgres.operator.common.ResourceWatcherFactory;
import io.stackgres.operatorframework.resource.WatcherMonitor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

/**
 * Feeds the embedded matriarch core with the live StackGres cluster set. <strong>Watch-driven</strong>:
 * resilient fabric8 watches on SGClusters and SGInstanceProfiles (via the operator's own
 * {@link ResourceWatcherFactory} + {@link WatcherMonitor}, so they auto-reconnect) trigger a
 * <em>coalesced</em> {@link #refresh()} on any add/update/delete — so a change reaches
 * {@code stackgres cluster list} within ~sub-second, like the operator's own reconciliation. A periodic
 * resync runs as a backstop for missed events / dropped watches.
 *
 * <p>Refreshes run on the managed {@link ManagedExecutor} (never on the fabric8 watch-callback thread)
 * and are single-flighted: a burst of events collapses into at most one follow-up refresh — no
 * hand-rolled thread pool or timer to manage. Each refresh scans the CRs, maps via
 * {@link StackGresMapper}, and calls {@link Matriarch#reconcileObserved} (declarative — adopts new,
 * updates known, forgets removed). Read-only: it never mutates StackGres. v1 sources CR + profile only;
 * live pod data (address, DB size) arrives with {@code slon} in v1.1.
 */
@ApplicationScoped
public class StackGresObserver {

  private static final Logger LOG = Logger.getLogger(StackGresObserver.class);

  @Inject
  CustomResourceScanner<StackGresCluster> clusterScanner;

  @Inject
  ProfileScanner profileScanner;

  @Inject
  Matriarch matriarch;

  @Inject
  KubernetesClient client;

  @Inject
  ResourceWatcherFactory watcherFactory;

  @Inject
  ManagedExecutor executor;

  private final List<WatcherMonitor<?>> monitors = new CopyOnWriteArrayList<>();
  // Single-flight coalescing: `dirty` marks pending work; `running` guards a single in-flight drain.
  private final AtomicBoolean running = new AtomicBoolean();
  private volatile boolean dirty;

  void onStart(@Observes StartupEvent ev) {
    // Reconciliation off (also set during the image's AppCDS training run): nothing to observe.
    if (OperatorProperty.DISABLE_RECONCILIATION.getBoolean()) {
      LOG.info("Reconciliation disabled — matriarch observer not started");
      return;
    }
    // Seed + start the watches OFF the startup thread so a slow or UNREACHABLE API can never block boot
    // or readiness. The AppCDS training run points the client at an unreachable API (240.0.0.1); doing
    // this synchronously here would stall each k8s call on the TCP connect timeout (~2 min) and hang the
    // image build. The seed still precedes the watches (same task); later refreshes single-flight via
    // drain(), so this is the only place startup ordering matters.
    executor.execute(() -> {
      refresh();
      startWatchers();
    });
  }

  void onStop(@Observes ShutdownEvent ev) {
    monitors.forEach(WatcherMonitor::close);
    monitors.clear();
    // ManagedExecutor is owned by Quarkus — nothing to shut down here.
  }

  private void startWatchers() {
    monitors.add(watch(StackGresCluster.class, StackGresClusterList.class));
    monitors.add(watch(StackGresInstanceProfile.class, StackGresInstanceProfileList.class));
  }

  /** Watch a CR type in all namespaces; any event requests a coalesced refresh. */
  private <T extends HasMetadata, L extends KubernetesResourceList<T>> WatcherMonitor<T> watch(
      Class<T> crClass, Class<L> listClass) {
    BiConsumer<Action, T> onEvent = (action, resource) -> scheduleRefresh();
    return new WatcherMonitor<>(crClass.getSimpleName(),
        watcherListener -> client
            .resources(crClass, listClass)
            .inAnyNamespace()
            .watch(watcherFactory.createWatcher(onEvent, watcherListener)));
  }

  /** Periodic resync backstop (missed events / dropped watches) — funnels through the same path. */
  @Scheduled(every = "${stackgres.matriarch.resync:60s}")
  void resync() {
    scheduleRefresh();
  }

  /**
   * Request a refresh. Coalesces bursts: marks work pending and, if no drain is running, submits one to
   * the managed executor — so we never block the fabric8 watch-callback thread and never run two
   * refreshes at once.
   */
  private void scheduleRefresh() {
    dirty = true;
    if (running.compareAndSet(false, true)) {
      executor.execute(this::drain);
    }
  }

  private void drain() {
    try {
      do {
        dirty = false;
        refresh();
      } while (dirty);   // a change arrived while refreshing — make one more pass
    } finally {
      running.set(false);
      // A change that landed between the last read and releasing the flag: pick it back up.
      if (dirty && running.compareAndSet(false, true)) {
        executor.execute(this::drain);
      }
    }
  }

  /**
   * Scan CRs + profiles, map, and declaratively reconcile the core. Never runs concurrently — the
   * startup seed precedes the watches, and all later calls go through the single-flight {@link #drain}.
   */
  private void refresh() {
    try {
      Map<String, StackGresInstanceProfile> profiles = new HashMap<>();
      for (StackGresInstanceProfile p : profileScanner.getResources()) {
        profiles.put(p.getMetadata().getNamespace() + "/" + p.getMetadata().getName(), p);
      }
      List<Cluster> clusters = new ArrayList<>();
      for (StackGresCluster cr : clusterScanner.getResources()) {
        StackGresInstanceProfile profile = null;
        if (cr.getSpec() != null && cr.getSpec().getSgInstanceProfile() != null) {
          profile = profiles.get(
              cr.getMetadata().getNamespace() + "/" + cr.getSpec().getSgInstanceProfile());
        }
        clusters.add(StackGresMapper.toCluster(cr, profile));
      }
      matriarch.reconcileObserved(clusters);
    } catch (RuntimeException e) {
      // A scan failure (transient API error, RBAC, partition) must not kill the watches/scheduler.
      LOG.warnf(e, "matriarch observer refresh failed: %s", e.getMessage());
    }
  }
}
