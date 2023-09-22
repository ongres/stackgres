/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.app.OperatorLockHolder;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
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
  private final AtomicReference<List<Optional<T>>> atomicReference =
      new AtomicReference<List<Optional<T>>>(List.of());
  private final ArrayBlockingQueue<Boolean> arrayBlockingQueue = new ArrayBlockingQueue<>(1);

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
    reconcile(List.of(Optional.of(config)));
  }

  @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
      justification = "We do not care if queue is already filled")
  private void reconcile(List<Optional<T>> configs) {
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
        List<Optional<T>> configs = atomicReference.getAndSet(List.of());
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

  protected void reconciliationsCycle(List<Optional<T>> configs) {
    mergedConfigs(configs).stream()
        .filter(t -> Optional.ofNullable(t.v1.getMetadata().getAnnotations())
            .map(annotations -> annotations.get(STACKGRES_IO_RECONCILIATION))
            .map(Boolean::parseBoolean)
            .map(b -> !b)
            .orElse(true))
        .forEach(t -> reconciliationCycle(t.v1, t.v2));
  }

  private List<Tuple2<T, Boolean>> mergedConfigs(List<Optional<T>> configs) {
    if (configs.stream().anyMatch(Optional::isEmpty)) {
      return getExistentSources().stream()
          .map(config -> Tuple.tuple(config, false))
          .toList();
    }
    return Seq.seq(configs)
        .map(Optional::get)
        .grouped(this::configId)
        .flatMap(t -> t.v2.limit(1))
        .map(config -> Tuple.tuple(config, true))
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
    return config.getMetadata().getNamespace() + "." + config.getMetadata().getName();
  }

  protected void reconciliationCycle(T configKey, boolean load) {
    final ObjectMeta metadata = configKey.getMetadata();
    final String configId = configKey.getKind()
        + " " + metadata.getNamespace() + "." + metadata.getName();

    List<Exception> exceptions = new ArrayList<>();
    try {
      final T config;
      if (load) {
        var configFound = finder.findByNameAndNamespace(
            metadata.getName(), metadata.getNamespace());
        if (configFound.isEmpty()) {
          LOGGER.debug("{} not found, skipping reconciliation", configId);
          return;
        }
        config = configFound.get();
      } else {
        config = configKey;
      }
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
                LOGGER.info("Creating {} {}.{}",
                    resource.getKind(),
                    resource.getMetadata().getNamespace(),
                    resource.getMetadata().getName());
                var created = handlerDelegator.create(config, resource);
                deployedResourcesCache.put(resource, created);
              } catch (Exception ex) {
                exceptions.add(ex);
              }
            });

        result.getPatches()
            .stream()
            .sorted(Comparator.comparing(
                Tuple2::v1, ReconciliationOperations.RESOURCES_COMPARATOR))
            .forEach(resource -> {
              try {
                LOGGER.info("Patching {} {}.{}",
                    resource.v2.getKind(),
                    resource.v2.getMetadata().getNamespace(),
                    resource.v2.getMetadata().getName());
                var patched = handlerDelegator.patch(config, resource.v1, resource.v2);
                deployedResourcesCache.put(resource.v1, patched);
              } catch (Exception ex) {
                exceptions.add(ex);
              }
            });

        result.getDeletions()
            .stream()
            .sorted(Collections.reverseOrder(
                ReconciliationOperations.RESOURCES_COMPARATOR))
            .forEach(resource -> {
              LOGGER.info("Deleting resource {}.{} of kind: {}",
                  resource.getMetadata().getNamespace(),
                  resource.getMetadata().getName(),
                  resource.getKind());
              try {
                deployedResourcesCache.remove(resource);
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
      var iterator = exceptions.listIterator();
      Exception ex = iterator.next();
      iterator.forEachRemaining(otherEx -> ex.addSuppressed(otherEx));
      LOGGER.error("Reconciliation of {} failed", configId, ex);
      try {
        onError(ex, configKey);
      } catch (Exception onErrorEx) {
        LOGGER.error("Failed executing on error event of {}", configId, onErrorEx);
      }
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
