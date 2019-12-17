/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.reconciliation;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;

import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReconciliationCycle<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractReconciliationCycle.class);

  protected final String name;
  protected final Supplier<KubernetesClient> clientSupplier;
  protected final Function<T, HasMetadata> configGetter;
  protected final ResourceHandlerSelector<T> handlerSelector;
  protected final ObjectMapper objectMapper;
  private final ExecutorService executorService;
  private final ArrayBlockingQueue<Boolean> arrayBlockingQueue = new ArrayBlockingQueue<>(1);

  private final CompletableFuture<Void> stopped = new CompletableFuture<>();
  private boolean close = false;

  private AtomicInteger reconciliationCount = new AtomicInteger(0);

  public AbstractReconciliationCycle() {
    this.name = null;
    this.clientSupplier = null;
    this.configGetter = null;
    this.handlerSelector = null;
    this.objectMapper = null;
    this.executorService = null;
  }

  protected AbstractReconciliationCycle(String name, Supplier<KubernetesClient> clientSupplier,
      Function<T, HasMetadata> configGetter, ResourceHandlerSelector<T> handlerSelector,
      ObjectMapper objectMapper) {
    super();
    this.name = name;
    this.clientSupplier = clientSupplier;
    this.configGetter = configGetter;
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
    String cycleName = "Reconciliation Cycle " + cycleId;

    LOGGER.trace("Starting " + cycleName);
    try (KubernetesClient client = clientSupplier.get()) {
      LOGGER.trace(cycleName + " getting existing clusters");
      ImmutableList<T> existingConfigs = getExistingConfigs(client);
      LOGGER.trace(cycleName + " deleting orphan resources");
      deleteOrphanResources(client, existingConfigs);

      for (T config : existingConfigs) {
        HasMetadata configResource = configGetter.apply(config);

        String configId = configResource.getMetadata().getNamespace() + "."
            + configResource.getMetadata().getName();

        try {
          LOGGER.trace(cycleName + " reconciling cluster "
              + configResource.getMetadata().getName());
          ImmutableList<HasMetadata> existingResourcesOnly = getExistingResources(
              client, config);
          ImmutableList<HasMetadata> requiredResourcesOnly = getRequiredResources(
              config, existingResourcesOnly);
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
                  .orElseGet(() -> handlerSelector.find(client, config, requiredResource))))
              .collect(ImmutableList.toImmutableList());
          createReconciliator(client, config, requiredResources, existingResources).reconcile();
        } catch (Exception ex) {
          LOGGER.error(cycleName + " failed reconciling " + name + " " + configId, ex);
          onConfigError(config, configResource, ex);
        }
      }
      LOGGER.trace(cycleName + " ended successfully");
    } catch (Exception ex) {
      LOGGER.error(name + " reconciliation cycle failed", ex);
      onError(ex);
    }
  }

  protected abstract void onError(Exception ex);

  protected abstract void onConfigError(T config, HasMetadata configResource, Exception ex);

  protected abstract AbstractReconciliator<T> createReconciliator(KubernetesClient client, T config,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> requiredResources,
      ImmutableList<Tuple2<HasMetadata, Optional<HasMetadata>>> existingResources);

  protected abstract ImmutableList<HasMetadata> getRequiredResources(T config,
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

  private void deleteOrphanResources(KubernetesClient client,
      ImmutableList<T> existingCOnfigs) {
    ImmutableList<HasMetadata> existingOrphanResources = getExistingOrphanResources(
        client, existingCOnfigs);
    Set<Tuple2<String, String>> deletedClusters = new HashSet<>();
    for (HasMetadata existingOrphanResource : existingOrphanResources) {
      LOGGER.debug("Deleteing resource {} of type {}"
          + " since does not belong to any cluster",
          getResourceIdentifier(existingOrphanResource),
          existingOrphanResource.getKind());
      handlerSelector.delete(client, null, existingOrphanResource);
      deletedClusters.add(Tuple.tuple(
          handlerSelector.getConfigNamespaceOf(existingOrphanResource),
          handlerSelector.getConfigNameOf(existingOrphanResource)));
    }

    for (Tuple2<String, String> deletedConfig : deletedClusters) {
      LOGGER.info(name + " deleted: '{}'",
          getResourceIdentifier(deletedConfig.v1, deletedConfig.v2));
      onOrphanConfigDeletion(deletedConfig.v1, deletedConfig.v2);
    }
  }

  protected abstract void onOrphanConfigDeletion(String namespace, String name);

  protected abstract ImmutableList<T> getExistingConfigs(KubernetesClient client);

  private ImmutableList<HasMetadata> getExistingOrphanResources(KubernetesClient client,
      ImmutableList<T> existingConfigs) {
    return ImmutableList.<HasMetadata>builder()
        .addAll(handlerSelector.getOrphanResources(client, existingConfigs)
            .iterator())
        .build()
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

  private ImmutableList<HasMetadata> getExistingResources(KubernetesClient client,
      T config) {
    return ImmutableList.<HasMetadata>builder()
        .addAll(handlerSelector.getResources(client, config)
            .iterator())
        .build()
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

  private String getResourceIdentifier(HasMetadata resource) {
    return getResourceIdentifier(resource.getMetadata().getNamespace(),
        resource.getMetadata().getName());
  }

  private String getResourceIdentifier(String namespace, String name) {
    if (namespace == null) {
      return name;
    }
    return namespace + "." + name;
  }

}
