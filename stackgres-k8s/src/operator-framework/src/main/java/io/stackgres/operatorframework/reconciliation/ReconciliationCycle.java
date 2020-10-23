/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.reconciliation;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableList;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ReconciliationCycle<T extends ResourceHandlerContext,
    H extends CustomResource, S extends ResourceHandlerSelector<T>> {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected final String name;
  protected final Supplier<KubernetesClient> clientSupplier;
  protected final Reconciliator<T> reconciliator;
  protected final Function<T, H> resourceGetter;
  protected final S handlerSelector;
  private final ExecutorService executorService;
  private final ArrayBlockingQueue<Boolean> arrayBlockingQueue = new ArrayBlockingQueue<>(1);

  private final CompletableFuture<Void> stopped = new CompletableFuture<>();
  private boolean close = false;

  private AtomicInteger reconciliationCount = new AtomicInteger(0);

  protected ReconciliationCycle(String name, Supplier<KubernetesClient> clientSupplier,
      Reconciliator<T> reconciliator, Function<T, H> resourceGetter, S handlerSelector) {
    super();
    logger.debug("ReconciliationCycle", new Exception());
    this.name = name;
    this.clientSupplier = clientSupplier;
    this.reconciliator = reconciliator;
    this.resourceGetter = resourceGetter;
    this.handlerSelector = handlerSelector;
    this.executorService = Executors.newSingleThreadExecutor(
        r -> new Thread(r, name + "-ReconciliationCycle"));
  }

  public void start() {
    executorService.execute(this::reconciliationCycleLoop);
  }

  public void stop() {
    close = true;
    reconcile();
    executorService.shutdown();
    reconcile();
    stopped.join();
  }

  @SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED_BAD_PRACTICE",
      justification = "We do not care if queue is already filled")
  public void reconcile() {
    arrayBlockingQueue.offer(Boolean.TRUE);
  }

  private void reconciliationCycleLoop() {
    logger.info("{} reconciliation cycle loop started", name);
    while (true) {
      try {
        arrayBlockingQueue.take();
        if (close) {
          break;
        }
        reconciliationCycle();
      } catch (Exception ex) {
        logger.error(name + " reconciliation cycle loop was interrupted", ex);
      }
    }
    logger.info("{} reconciliation cycle loop stopped", name);
    stopped.complete(null);
  }

  void reconciliationCycle() {
    final int cycleId = reconciliationCount.incrementAndGet();
    String cycleName = cycleId + "| " + name + " reconciliation cycle";

    logger.trace("{} starting", cycleName);
    logger.trace("{} getting existing {} list", cycleName, name.toLowerCase(Locale.US));
    ImmutableList<T> existingContexts = getExistingContexts();
    try {
      for (T context : existingContexts) {
        HasMetadata contextResource = resourceGetter.apply(context);

        String contextId = contextResource.getMetadata().getNamespace() + "."
            + contextResource.getMetadata().getName();

        try (KubernetesClient client = clientSupplier.get()) {
          logger.trace("{} working on {}", cycleName, contextId);
          ImmutableList<HasMetadata> existingResourcesOnly = getExistingResources(
              client,
              context);
          T contextWithExistingResourcesOnly = getContextWithExistingResourcesOnly(
              context,
              existingResourcesOnly
                  .stream()
                  .map(existingResource -> Tuple.tuple(existingResource,
                      Optional.<HasMetadata>empty()))
                  .collect(ImmutableList.toImmutableList()));
          ImmutableList<HasMetadata> requiredResourcesOnly = getRequiredResources(
              contextWithExistingResourcesOnly);
          ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources =
              existingResourcesOnly
                  .stream()
                  .map(existingResource -> Tuple.tuple(existingResource,
                      findResourceIn(existingResource, requiredResourcesOnly)))
                  .collect(ImmutableList.toImmutableList());
          ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources =
              requiredResourcesOnly
                  .stream()
                  .map(requiredResource -> Tuple.tuple(requiredResource,
                      Optional.of(findResourceIn(requiredResource, existingResourcesOnly))
                          .filter(Optional::isPresent)
                          .orElseGet(
                              () -> handlerSelector.find(client, context, requiredResource))))
                  .collect(ImmutableList.toImmutableList());
          T contextWithExistingAndRequiredResources = getContextWithExistingAndRequiredResources(
              context, requiredResources, existingResources);
          reconciliator.reconcile(client, contextWithExistingAndRequiredResources);
        } catch (Exception ex) {
          logger.error(cycleName + " failed reconciling " + contextId, ex);
          try {
            onConfigError(context, contextResource, ex);
          } catch (RuntimeException rex) {
            logger.error(cycleName + " failed sending event while reconciling " + contextId, rex);
          }
        }
      }
      logger.trace(cycleName + " ended successfully");
    } catch (RuntimeException ex) {
      logger.error(cycleName + " failed", ex);
      onError(ex);
    }
  }

  protected abstract void onError(Exception ex);

  protected abstract void onConfigError(T context,
      HasMetadata contextResource, Exception ex);

  protected abstract T getContextWithExistingResourcesOnly(T context,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResourcesOnly);

  protected abstract ImmutableList<HasMetadata> getRequiredResources(T context);

  protected abstract T getContextWithExistingAndRequiredResources(
      T context,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources);

  private Optional<HasMetadata> findResourceIn(HasMetadata resource,
      ImmutableList<HasMetadata> resources) {
    return resources
        .stream()
        .filter(otherResource -> resource.getKind()
            .equals(otherResource.getKind()))
        .filter(otherResource -> Objects.equals(resource.getMetadata().getNamespace(),
            otherResource.getMetadata().getNamespace()))
        .filter(otherResource -> resource.getMetadata().getName()
            .equals(otherResource.getMetadata().getName()))
        .findAny();
  }

  protected abstract ImmutableList<T> getExistingContexts();

  private ImmutableList<HasMetadata> getExistingResources(KubernetesClient client, T context) {
    return ImmutableList.<HasMetadata>builder()
        .addAll(handlerSelector.getResources(client, context)
            .iterator())
        .build()
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

}
