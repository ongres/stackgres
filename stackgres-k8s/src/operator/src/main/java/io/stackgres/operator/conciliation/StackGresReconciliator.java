/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javax.inject.Inject;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.resource.CustomResourceScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StackGresReconciliator<T extends CustomResource<?, ?>> {

  protected static final Logger LOGGER = LoggerFactory.getLogger("io.stackgres.reconciliator");

  private static final String STACKGRES_IO_RECONCILIATION = StackGresContext
      .RECONCILIATION_PAUSE_KEY;

  private CustomResourceScanner<T> clusterScanner;

  private Conciliator<T> clusterConciliator;

  private HandlerDelegator<T> handlerDelegator;

  private final ExecutorService executorService;
  private final ArrayBlockingQueue<Boolean> arrayBlockingQueue = new ArrayBlockingQueue<>(1);

  private final CompletableFuture<Void> stopped = new CompletableFuture<>();
  private boolean close = false;

  public StackGresReconciliator() {
    this.executorService = Executors.newSingleThreadExecutor(
        r -> new Thread(r, getReconciliationName() + "-ReconciliationLoop"));
  }

  protected void start() {
    executorService.execute(this::reconciliationLoop);
  }

  protected void stop() {
    close = true;
    reconcile();
    executorService.shutdown();
    reconcile();
    stopped.join();
  }

  protected abstract String getReconciliationName();

  @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
      justification = "We do not care if queue is already filled")
  public void reconcile() {
    arrayBlockingQueue.offer(Boolean.TRUE);
  }

  private void reconciliationLoop() {
    LOGGER.info("{} reconciliation loop started", getReconciliationName());
    while (true) {
      try {
        arrayBlockingQueue.take();
        if (close) {
          break;
        }
        reconciliationCycle();
      } catch (Exception ex) {
        LOGGER.error("{} reconciliation loop was interrupted", getReconciliationName(), ex);
      }
    }
    LOGGER.info("{} reconciliation loop stopped", getReconciliationName());
    stopped.complete(null);
  }

  public synchronized void reconciliationCycle() {
    getExistentSources().forEach(cluster -> {
      final ObjectMeta metadata = cluster.getMetadata();
      final String clusterId = metadata.getNamespace() + "/" + metadata.getName();

      try {
        onPreReconciliation(cluster);
        LOGGER.info("Checking reconciliation status of cluster {}", clusterId);
        ReconciliationResult result = clusterConciliator.evalReconciliationState(cluster);
        if (!result.isUpToDate()) {
          LOGGER.info("Cluster {} it's not up to date. Reconciling", clusterId);

          result.getCreations()
              .forEach(resource -> {
                LOGGER.info("Creating resource {} of kind: {}",
                    resource.getMetadata().getName(), resource.getKind());
                try {
                  handlerDelegator.create(resource);
                } catch (KubernetesClientException ex) {
                  if (ex.getCode() == 409) {
                    handlerDelegator.replace(resource);
                  } else {
                    throw ex;
                  }
                }
              });

          result.getPatches()
              .forEach(resource -> {
                LOGGER.info("Patching resource {} of kind: {}", resource.v2.getMetadata().getName(),
                    resource.v2.getKind());
                handlerDelegator.patch(resource.v1, resource.v2);
              });

          result.getDeletions()
              .forEach(resource -> {
                LOGGER.info("Deleting resource {} of kind: {}", resource.getMetadata().getName(),
                    resource.getKind());
                handlerDelegator.delete(resource);
              });
          if (result.getDeletions().size() == 0 && result.getPatches().size() == 0) {
            onConfigCreated(cluster, result);
          } else {
            onConfigUpdated(cluster, result);
          }
        } else {
          LOGGER.info("Cluster " + clusterId + " it's up to date");
        }

        onPostReconciliation(cluster);

      } catch (Exception e) {
        LOGGER.error("Reconciliation of cluster {} failed", clusterId, e);
        try {
          onError(e, cluster);
        } catch (Exception onErrorEx) {
          LOGGER.error("Failed of executing on error event of cluster {}", clusterId, onErrorEx);
        }
      }
    });

  }

  private Stream<T> getExistentSources() {
    return clusterScanner.getResources().stream()
        .filter(r -> Optional.ofNullable(r.getMetadata().getAnnotations())
            .map(annotations -> annotations.get(STACKGRES_IO_RECONCILIATION))
            .map(Boolean::parseBoolean)
            .map(b -> !b)
            .orElse(true));
  }

  public abstract void onPreReconciliation(T config);

  public abstract void onPostReconciliation(T config);

  public abstract void onConfigCreated(T context, ReconciliationResult result);

  public abstract void onConfigUpdated(T context, ReconciliationResult result);

  public abstract void onError(Exception e, T context);

  @Inject
  public void setClusterScanner(CustomResourceScanner<T> clusterScanner) {
    this.clusterScanner = clusterScanner;
  }

  @Inject
  public void setClusterConciliator(Conciliator<T> clusterConciliator) {
    this.clusterConciliator = clusterConciliator;
  }

  @Inject
  public void setHandlerDelegator(HandlerDelegator<T> handlerDelegator) {
    this.handlerDelegator = handlerDelegator;
  }

}
