/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.reconciliation;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operatorframework.resource.ResourceHandlerContext;
import io.stackgres.operatorframework.resource.ResourceHandlerSelector;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ReconciliationCycle<T extends ResourceHandlerContext,
    H extends CustomResource<?, ?>, S extends ResourceHandlerSelector<T>> {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  protected final String name;
  protected final KubernetesClient client;
  protected final Reconciliator<T> reconciliator;
  protected final S handlerSelector;
  private final ExecutorService executorService;
  private final ArrayBlockingQueue<Boolean> arrayBlockingQueue = new ArrayBlockingQueue<>(1);

  private final CompletableFuture<Void> stopped = new CompletableFuture<>();
  private boolean close = false;

  private AtomicInteger reconciliationCount = new AtomicInteger(0);

  protected ReconciliationCycle(String name, KubernetesClient client,
      Reconciliator<T> reconciliator, S handlerSelector) {
    this.name = name;
    this.client = client;
    this.reconciliator = reconciliator;
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

  public synchronized ReconciliationCycleResult<H> reconciliationCycle() {
    final ImmutableMap.Builder<H, Exception> contextExceptions = ImmutableMap.builder();
    final int cycleId = reconciliationCount.incrementAndGet();
    final String cycleName = cycleId + "| " + name + " reconciliation cycle";

    logger.trace("{} starting", cycleName);
    logger.trace("{} getting existing {} list", cycleName, name.toLowerCase(Locale.US));
    final ImmutableList<H> existingContextResources;

    try {
      existingContextResources = getExistingContextResources();
    } catch (RuntimeException ex) {
      logger.error(cycleName + " failed", ex);
      try {
        onError(ex);
      } catch (RuntimeException rex) {
        logger.error(cycleName
            + " failed sending event while retrieving reconciliation cycle contexts", rex);
      }
      return new ReconciliationCycleResult<>(ex);
    }

    try {
      for (H contextResource : existingContextResources) {
        T context = getContextFromResource(contextResource);

        String contextId = contextResource.getMetadata().getNamespace() + "."
            + contextResource.getMetadata().getName();

        try {
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
          ReconciliationResult<?> reconciliationResult =
              reconciliator.reconcile(client, contextWithExistingAndRequiredResources);
          if (!reconciliationResult.success()) {
            contextExceptions.put(contextResource, reconciliationResult.getException());
          }
        } catch (Exception ex) {
          contextExceptions.put(contextResource, ex);
          logger.error(cycleName + " failed reconciling " + contextId, ex);
          try {
            onConfigError(context, contextResource, ex);
          } catch (RuntimeException rex) {
            logger.error(cycleName + " failed sending event while reconciling " + contextId, rex);
          }
        }
      }
      logger.trace(cycleName + " ended successfully");
      return new ReconciliationCycleResult<>(existingContextResources, contextExceptions.build());
    } catch (RuntimeException ex) {
      logger.error(cycleName + " failed", ex);
      try {
        onError(ex);
      } catch (RuntimeException rex) {
        logger.error(cycleName + " failed sending event while running reconciliation cycle", rex);
      }
      return new ReconciliationCycleResult<>(ex);
    }
  }

  public static class ReconciliationCycleResult<T extends CustomResource<?, ?>> {
    private final List<T> contextResources;
    private final ImmutableMap<T, Exception> contextResourceExceptions;
    private final Exception exception;

    public ReconciliationCycleResult(ImmutableList<T> contextResources,
        ImmutableMap<T, Exception> contextExceptions) {
      this.contextResources = contextResources;
      this.contextResourceExceptions = contextExceptions;
      this.exception = null;
    }

    public ReconciliationCycleResult(Exception exception) {
      this.contextResources = ImmutableList.of();
      this.contextResourceExceptions = ImmutableMap.of();
      this.exception = exception;
    }

    public List<T> getContextResources() {
      return contextResources;
    }

    public Optional<Exception> getException() {
      return Optional.ofNullable(exception);
    }

    public Map<T, Exception> getContextExceptions() {
      return contextResourceExceptions;
    }

    public boolean success() {
      return exception == null && contextResourceExceptions.isEmpty();
    }

    public void throwIfFailed() throws Exception {
      Optional<Exception> exception = Seq.seq(getException())
          .append(getContextExceptions().values())
          .reduce(Optional.<Exception>empty(),
              (optional, anException) -> {
                optional.ifPresent(firstException -> firstException.addSuppressed(anException));
                return optional.or(() -> Optional.of(anException));
              },
              (u, v) -> v);
      if (exception.isPresent()) {
        throw exception.get();
      }
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

  protected abstract ImmutableList<H> getExistingContextResources();

  protected abstract T getContextFromResource(H contextResource);

  private ImmutableList<HasMetadata> getExistingResources(KubernetesClient client, T context) {
    return ImmutableList.<HasMetadata>builder()
        .addAll(handlerSelector.getResources(client, context)
            .iterator())
        .build()
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

}
