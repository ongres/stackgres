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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReconciliationCycle<T extends ResourceHandlerContext,
    H extends CustomResource, S extends ResourceHandlerSelector<T>> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractReconciliationCycle.class);

  protected final String name;
  protected final Supplier<KubernetesClient> clientSupplier;
  protected final Function<T, H> resourceGetter;
  protected final S handlerSelector;
  protected final ObjectMapper objectMapper;
  private final ExecutorService executorService;
  private final ArrayBlockingQueue<Boolean> arrayBlockingQueue = new ArrayBlockingQueue<>(1);

  private final CompletableFuture<Void> stopped = new CompletableFuture<>();
  private boolean close = false;

  private AtomicInteger reconciliationCount = new AtomicInteger(0);

  protected AbstractReconciliationCycle(String name, Supplier<KubernetesClient> clientSupplier,
      Function<T, H> resourceGetter, S handlerSelector,
      ObjectMapper objectMapper) {
    super();
    this.name = name;
    this.clientSupplier = clientSupplier;
    this.resourceGetter = resourceGetter;
    this.handlerSelector = handlerSelector;
    this.objectMapper = objectMapper;
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
    arrayBlockingQueue.offer(true);
    stopped.join();
  }

  public void reconcile() {
    arrayBlockingQueue.offer(true);
  }

  private void reconciliationCycleLoop() {
    LOGGER.info(name + " reconciliation cycle loop started");
    while (true) {
      try {
        arrayBlockingQueue.take();
        if (close) {
          break;
        }
        reconciliationCycle();
      } catch (Exception ex) {
        LOGGER.error(name + " reconciliation cycle loop was interrupted", ex);
      }
    }
    LOGGER.info(name + " reconciliation cycle loop stopped");
    stopped.complete(null);
  }

  private void reconciliationCycle() {
    final int cycleId = reconciliationCount.incrementAndGet();
    String cycleName = cycleId + "| " + name + " reconciliation cycle";

    LOGGER.trace(cycleName + " starting");
    try (KubernetesClient client = clientSupplier.get()) {
      LOGGER.trace(cycleName + " getting existing " + name.toLowerCase(Locale.US) + " list");
      ImmutableList<T> existingContexts = getExistingConfigs(client);
      for (T context : existingContexts) {
        HasMetadata contextResource = resourceGetter.apply(context);

        String contextId = contextResource.getMetadata().getNamespace() + "."
            + contextResource.getMetadata().getName();

        try {
          LOGGER.trace(cycleName + " working on " + contextId);
          ImmutableList<HasMetadata> existingResourcesOnly = getExistingResources(
              client, context);
          ImmutableList<HasMetadata> requiredResourcesOnly = getRequiredResources(
              context, existingResourcesOnly);
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
                  .orElseGet(() -> handlerSelector.find(client, context, requiredResource))))
              .collect(ImmutableList.toImmutableList());
          createReconciliator(client, context, requiredResources, existingResources).reconcile();
        } catch (Exception ex) {
          LOGGER.error(cycleName + " failed reconciling " + contextId, ex);
          try {
            onConfigError(context, contextResource, ex);
          } catch (Exception rex) {
            LOGGER.error(cycleName + " failed sending event while reconciling " + contextId, rex);
          }
        }
      }
      LOGGER.trace(cycleName + " ended successfully");
    } catch (Exception ex) {
      LOGGER.error(cycleName + " failed", ex);
      onError(ex);
    }
  }

  protected abstract void onError(Exception ex);

  protected abstract void onConfigError(T context, HasMetadata contextResource, Exception ex);

  protected abstract AbstractReconciliator<T, H, S> createReconciliator(KubernetesClient client,
      T context, ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources);

  protected abstract ImmutableList<HasMetadata> getRequiredResources(T context,
      ImmutableList<HasMetadata> existingResourcesOnly);

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

  protected abstract void onOrphanConfigDeletion(String namespace, String name);

  protected abstract ImmutableList<T> getExistingConfigs(KubernetesClient client);

  private ImmutableList<HasMetadata> getExistingResources(KubernetesClient client,
      T context) {
    return ImmutableList.<HasMetadata>builder()
        .addAll(handlerSelector.getResources(client, context)
            .iterator())
        .build()
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

}
