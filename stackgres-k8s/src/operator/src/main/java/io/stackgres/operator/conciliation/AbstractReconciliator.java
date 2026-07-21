/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.Quarkus;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.RetryUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operator.common.Metrics;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReconciliator<T extends CustomResource<?, ?>> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(
      AbstractReconciliator.class.getPackage().getName());

  private static final String STACKGRES_IO_RECONCILIATION = StackGresContext
      .RECONCILIATION_PAUSE_KEY;

  private final CustomResourceScanner<T> scanner;
  private final CustomResourceFinder<T> finder;
  private final AbstractConciliator<T> conciliator;
  private final DeployedResourcesCache deployedResourcesCache;
  private final HandlerDelegator<T> handlerDelegator;
  private final KubernetesClient client;
  private final OperatorLockHolder operatorLockReconciliator;
  private final String reconciliationName;
  private final ExecutorService executorService;
  private final ScheduledExecutorService backoffExecutorService;
  private final ScheduledExecutorService blockedExecutorService;
  private final AtomicReference<List<Optional<Tuple2<T, Integer>>>> atomicReference =
      new AtomicReference<>(List.of());
  private final ArrayBlockingQueue<Boolean> arrayBlockingQueue = new ArrayBlockingQueue<>(1);
  private final ReconciliatorWorkerThreadPool reconciliatorWorkerThreadPool;
  private final Metrics metrics;

  private final int reconciliationInitialBackoff;
  private final int reconciliationMaxBackoff;
  private final int reconciliationBackoffVariation;
  private final int reconciliationRestartOnBlockedDelay;

  private final CompletableFuture<Void> stopped = new CompletableFuture<>();
  private boolean close = false;

  protected AbstractReconciliator(
      CustomResourceScanner<T> scanner,
      CustomResourceFinder<T> finder,
      AbstractConciliator<T> conciliator,
      DeployedResourcesCache deployedResourcesCache,
      HandlerDelegator<T> handlerDelegator,
      KubernetesClient client,
      OperatorLockHolder operatorLockReconciliator,
      ReconciliatorWorkerThreadPool reconciliatorWorkerThreadPool,
      Metrics metrics,
      String reconciliationName) {
    this.scanner = scanner;
    this.finder = finder;
    this.conciliator = conciliator;
    this.deployedResourcesCache = deployedResourcesCache;
    this.handlerDelegator = handlerDelegator;
    this.client = client;
    this.reconciliationName = reconciliationName;
    this.operatorLockReconciliator = operatorLockReconciliator;
    this.executorService = Executors.newSingleThreadExecutor(
        r -> new Thread(r, reconciliationName + "-ReconciliationLoop"));
    this.backoffExecutorService = Executors.newSingleThreadScheduledExecutor(
        r -> new Thread(r, reconciliationName + "-BackoffReconciliationScheduler"));
    this.blockedExecutorService = Executors.newSingleThreadScheduledExecutor(
        r -> new Thread(r, reconciliationName + "-BlockedReconciliationScheduler"));
    this.reconciliatorWorkerThreadPool = reconciliatorWorkerThreadPool;
    this.metrics = metrics;
    this.reconciliationInitialBackoff = OperatorProperty.RECONCILIATION_INITIAL_BACKOFF
        .get()
        .map(Integer::parseInt)
        .filter(initial -> initial > 0)
        .orElse(5);
    this.reconciliationMaxBackoff = OperatorProperty.RECONCILIATION_MAX_BACKOFF
        .get()
        .map(Integer::parseInt)
        .orElse(300);
    this.reconciliationBackoffVariation = OperatorProperty.RECONCILIATION_BACKOFF_VARIATION
        .get()
        .map(Integer::parseInt)
        .orElse(10);
    this.reconciliationRestartOnBlockedDelay = OperatorProperty.RECONCILIATION_RESTART_ON_BLOCKED_DELAY
        .get()
        .map(Integer::parseInt)
        .orElse(3_600);
  }

  public AbstractReconciliator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.scanner = null;
    this.finder = null;
    this.conciliator = null;
    this.deployedResourcesCache = null;
    this.handlerDelegator = null;
    this.client = null;
    this.reconciliationName = null;
    this.operatorLockReconciliator = null;
    this.executorService = null;
    this.backoffExecutorService = null;
    this.blockedExecutorService = null;
    this.reconciliatorWorkerThreadPool = null;
    this.metrics = null;
    this.reconciliationInitialBackoff = 0;
    this.reconciliationMaxBackoff = 0;
    this.reconciliationBackoffVariation = 0;
    this.reconciliationRestartOnBlockedDelay = -1;
  }

  protected void start() {
    operatorLockReconciliator.register(this);
    executorService.execute(this::reconciliationLoop);
  }

  protected void stop() {
    close = true;
    reconcile(List.of());
    executorService.shutdown();
    reconcile(List.of());
    stopped.join();
  }

  protected String getReconciliationName() {
    return reconciliationName;
  }

  public void reconcileAll() {
    reconcile(List.of(Optional.empty()));
  }

  public void reconcile(T config) {
    reconcile(List.of(Optional.of(Tuple.tuple(config, 0))));
  }

  private void reconcile(T config, Integer retry) {
    reconcile(List.of(Optional.of(Tuple.tuple(config, retry))));
  }

  @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
      justification = "We do not care if queue is already filled")
  private void reconcile(List<Optional<Tuple2<T, Integer>>> configs) {
    atomicReference.updateAndGet(atomicConfigs -> Seq
        .seq(atomicConfigs)
        .append(configs)
        .toList());
    arrayBlockingQueue.offer(true);
  }

  private void reconciliationLoop() {
    LOGGER.info("{} reconciliation loop started", getReconciliationName());
    while (true) {
      try {
        if (!operatorLockReconciliator.isLeader()) {
          if (close) {
            break;
          }
          Thread.sleep(100);
          continue;
        }
        arrayBlockingQueue.take();
        List<Optional<Tuple2<T, Integer>>> configs = atomicReference.getAndSet(List.of());
        if (close) {
          break;
        }
        reconciliationsCycle(configs);
      } catch (Exception ex) {
        LOGGER.error("{} reconciliation loop was interrupted", getReconciliationName(), ex);
      }
    }
    LOGGER.info("{} reconciliation loop stopped", getReconciliationName());
    stopped.complete(null);
  }

  protected void reconciliationsCycle(List<Optional<Tuple2<T, Integer>>> configs) {
    mergedConfigs(configs).stream()
        // A custom resource that requires a finalizer is reconciled while it is being deleted even
        // if its reconciliation is paused, or pausing the reconciliation would make it impossible
        // to remove the finalizer and the custom resource could never be deleted.
        .filter(t -> (!getFinalizers().isEmpty()
            && t.v1.getMetadata().getDeletionTimestamp() != null)
            || Optional.ofNullable(t.v1.getMetadata().getAnnotations())
            .map(annotations -> annotations.get(STACKGRES_IO_RECONCILIATION))
            .map(Boolean::parseBoolean)
            .map(b -> !b)
            .orElse(true))
        .forEach(t -> reconciliatorWorkerThreadPool.scheduleReconciliation(
            () -> unblockableReconciliationCycle(t.v1, t.v2, t.v3),
            t.v4,
            t.v5));
  }

  private List<Tuple5<T, Integer, Boolean, String, Boolean>> mergedConfigs(List<Optional<Tuple2<T, Integer>>> configs) {
    var groupedConfigs = Seq.seq(configs)
        .flatMap(Optional::stream)
        .groupBy(t -> configId(t.v1));
    return Seq.seq(groupedConfigs)
        .map(config -> Tuple.tuple(
            config.v2.getFirst().v1, config.v2.getFirst().v2, true, config.v1, true))
        .append(Optional.of(configs.stream().anyMatch(Optional::isEmpty))
            .filter(anyMatch -> anyMatch)
            .stream()
            .flatMap(ignored -> getExistentSources().stream())
            .map(config -> Tuple.tuple(config, 0, true, configId(config), false))
            .filter(config -> !groupedConfigs.containsKey(config.v4)))
        .toList();
  }

  private List<T> getExistentSources() {
    try {
      return scanner.getResources();
    } catch (Exception ex) {
      LOGGER.error("Failed retrieving existing sources", ex);
      return List.of();
    }
  }

  private String configId(T config) {
    return config.getCRDName() + "/" + config.getMetadata().getNamespace() + "/" + config.getMetadata().getName();
  }

  private void unblockableReconciliationCycle(T configKey, int retry, boolean load) {
    if (this.reconciliationRestartOnBlockedDelay == -1) {
      reconciliationCycle(configKey, retry, load);
      return;
    }
    final AtomicReference<Instant> lastReconciliationCycleStats
        = new AtomicReference<>(null);
    ScheduledFuture<Void> restartJob = this.blockedExecutorService.schedule(() -> {
      var stats = lastReconciliationCycleStats.get();
      if (stats == null) {
        LOGGER.error("Reconciliation for " + configId(configKey) + " was running for more than "
            + this.blockedExecutorService + " seconds. Restarting the operator");
        Quarkus.asyncExit();
      }
      return null;
    }, this.reconciliationRestartOnBlockedDelay, TimeUnit.SECONDS);
    try {
      reconciliationCycle(configKey, retry, load);
    } finally {
      restartJob.cancel(true);
      lastReconciliationCycleStats.set(Instant.now());
    }
  }

  protected void reconciliationCycle(T configKey, int retry, boolean load) {
    final long startTimestamp = System.currentTimeMillis();
    final ObjectMeta metadata = configKey.getMetadata();
    final String configId = configKey.getKind()
        + " " + metadata.getNamespace() + "." + metadata.getName();

    List<Exception> exceptions = new ArrayList<>();
    try {
      T configLoaded;
      if (load) {
        var configFound = finder.findByNameAndNamespace(
            metadata.getName(), metadata.getNamespace());
        if (configFound.isEmpty()) {
          LOGGER.debug("{} not found, skipping reconciliation", configId);
          return;
        }
        configLoaded = configFound.get();
      } else {
        configLoaded = configKey;
      }
      final List<String> finalizers = getFinalizers();
      if (!finalizers.isEmpty()) {
        if (configLoaded.getMetadata().getDeletionTimestamp() != null) {
          if (hasAnyFinalizer(configLoaded, finalizers)) {
            reconcileDeletion(configLoaded, configId, finalizers, retry);
          } else {
            LOGGER.debug("{} is being deleted, skipping reconciliation", configId);
          }
          return;
        }
        if (!hasAnyFinalizer(configLoaded, finalizers)) {
          LOGGER.debug("Adding finalizers {} to {}", finalizers, configId);
          configLoaded = addFinalizers(configLoaded, finalizers);
        }
      }
      final T config = configLoaded;
      onPreReconciliation(config);
      LOGGER.debug("Checking reconciliation status of {}", configId);
      ReconciliationResult result = conciliator.evalReconciliationState(config);
      if (!result.isUpToDate()) {
        LOGGER.info("{} it's not up to date. Reconciling", configId);

        result.getCreations()
            .stream()
            .sorted(ReconciliationOperations.RESOURCES_COMPARATOR)
            .forEach(resource -> {
              try {
                LOGGER.debug("Found resource to create {} {}.{}",
                    resource.getKind(),
                    resource.getMetadata().getNamespace(),
                    resource.getMetadata().getName());
                var created = handlerDelegator.create(config, resource);
                deployedResourcesCache.put(config, resource, created);
              } catch (Exception ex) {
                if (resource instanceof Role
                    || resource instanceof RoleBinding) {
                  if (ex instanceof RuntimeException rex) {
                    throw rex;
                  }
                  throw new RuntimeException(ex);
                }
                exceptions.add(ex);
              }
            });

        result.getPatches()
            .stream()
            .sorted(Comparator.comparing(
                Tuple2::v1, ReconciliationOperations.RESOURCES_COMPARATOR))
            .forEach(resource -> {
              try {
                LOGGER.debug("Found resource to patch {} {}.{}",
                    resource.v2.getKind(),
                    resource.v2.getMetadata().getNamespace(),
                    resource.v2.getMetadata().getName());
                var patched = handlerDelegator.patch(config, resource.v1, resource.v2);
                deployedResourcesCache.put(config, resource.v1, patched);
              } catch (Exception ex) {
                exceptions.add(ex);
              }
            });

        result.getDeletions()
            .stream()
            .sorted(Collections.reverseOrder(
                ReconciliationOperations.RESOURCES_COMPARATOR))
            .forEach(resource -> {
              LOGGER.debug("Found resource to delete {}.{} of kind: {}",
                  resource.getMetadata().getNamespace(),
                  resource.getMetadata().getName(),
                  resource.getKind());
              try {
                deployedResourcesCache.remove(config, resource);
                handlerDelegator.delete(config, resource);
              } catch (Exception ex) {
                exceptions.add(ex);
              }
            });
        if (result.getDeletions().isEmpty() && result.getPatches().isEmpty()) {
          onConfigCreated(config, result);
        } else {
          onConfigUpdated(config, result);
        }
      } else {
        LOGGER.debug("{} it's up to date", configId);
      }

      onPostReconciliation(config);
    } catch (Exception ex) {
      exceptions.add(ex);
    }
    if (!exceptions.isEmpty()) {
      backoffExecutorService.schedule(() -> reconcile(configKey, retry + 1),
          RetryUtil.calculateExponentialBackoffDelay(
              reconciliationInitialBackoff,
              reconciliationMaxBackoff,
              reconciliationBackoffVariation,
              retry + 1), TimeUnit.SECONDS);
      var iterator = exceptions.listIterator();
      Exception ex = iterator.next();
      iterator.forEachRemaining(otherEx -> ex.addSuppressed(otherEx));
      LOGGER.error("Reconciliation of {} failed", configId, ex);
      try {
        onError(ex, configKey);
      } catch (Exception onErrorEx) {
        LOGGER.error("Failed executing on error event of {}", configId, onErrorEx);
      }
      metrics.incrementReconciliationTotalErrors(configKey.getClass());
    }
    metrics.incrementReconciliationTotalPerformed(configKey.getClass());
    metrics.setReconciliationLastDuration(configKey.getClass(), System.currentTimeMillis() - startTimestamp);
  }

  /**
   * The finalizers to set on the custom resource so that its deletion is delayed until
   * {@link #onFinalizer(CustomResource, String)} allows it, empty when the custom resource does not
   * require to delay its deletion.
   */
  protected List<String> getFinalizers() {
    return List.of();
  }

  /**
   * Invoked on each reconciliation cycle while the custom resource is being deleted and any of the
   * finalizers returned by {@link #getFinalizers()} is still set on it. Returns true when the
   * finalizers can be removed so that the deletion of the custom resource completes.
   */
  protected boolean onFinalizer(T config, String finalizer) {
    return true;
  }

  /**
   * Update the custom resource applying the setter to its latest version. Must be implemented when
   * {@link #getFinalizers()} is not empty since it is used to add and remove the finalizers.
   */
  protected T updateResource(T config, Consumer<T> setter) {
    throw new UnsupportedOperationException(
        "Updating " + config.getKind() + " is required in order to handle its finalizers");
  }

  private boolean hasAnyFinalizer(T config, List<String> finalizers) {
    return Optional.of(config.getMetadata())
        .map(ObjectMeta::getFinalizers)
        .stream()
        .flatMap(List::stream)
        .anyMatch(finalizers::contains);
  }

  private T addFinalizers(T config, List<String> finalizers) {
    return updateResource(config, currentConfig -> {
      if (currentConfig.getMetadata().getFinalizers() == null) {
        currentConfig.getMetadata().setFinalizers(new ArrayList<>());
      }
      for (String finalizer : finalizers) {
        if (!currentConfig.getMetadata().getFinalizers().contains(finalizer)) {
          currentConfig.getMetadata().getFinalizers().add(finalizer);
        }
      }
    });
  }

  private void reconcileDeletion(T config, String configId, List<String> finalizers, int retry) {
    List<String> pendingFinalizers = Optional
        .ofNullable(config.getMetadata().getFinalizers())
        .orElse(List.of())
        .stream()
        .filter(finalizers::contains)
        .toList();
    ArrayList<String> toRemoveFinalizers = new ArrayList<>(finalizers.size());
    for (String finalizer : pendingFinalizers) {
      if (onFinalizer(config, finalizer)) {
        toRemoveFinalizers.add(finalizer);
      }
    }
    if (!toRemoveFinalizers.isEmpty()) {
      LOGGER.debug("Removing finalizers {} from {}", toRemoveFinalizers, configId);
      updateResource(config, currentConfig -> Optional.of(currentConfig.getMetadata())
          .map(ObjectMeta::getFinalizers)
          .ifPresent(currentFinalizers -> currentFinalizers.removeIf(toRemoveFinalizers::contains)));
    }
    List<String> remainingFinalizers = pendingFinalizers
        .stream()
        .filter(Predicate.not(toRemoveFinalizers::contains))
        .toList();
    if (!remainingFinalizers.isEmpty()) {
      LOGGER.debug("{} is being deleted, waiting before removing the finalizers {}",
          configId, remainingFinalizers);
      // A watch event of any of the resources the custom resource is waiting for will trigger a new
      // reconciliation cycle. Reschedule anyway so that the finalizer is removed even if such an
      // event is missed, that would otherwise block the deletion forever.
      backoffExecutorService.schedule(() -> reconcile(config, retry + 1),
          RetryUtil.calculateExponentialBackoffDelay(
              reconciliationInitialBackoff,
              reconciliationMaxBackoff,
              reconciliationBackoffVariation,
              retry + 1), TimeUnit.SECONDS);
    }
  }

  protected abstract void onPreReconciliation(T config);

  protected abstract void onPostReconciliation(T config);

  protected abstract void onConfigCreated(T context, ReconciliationResult result);

  protected abstract void onConfigUpdated(T context, ReconciliationResult result);

  protected abstract void onError(Exception e, T context);

  public KubernetesClient getClient() {
    return client;
  }

}
