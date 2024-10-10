/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.GroupVersionKindBuilder;
import io.fabric8.kubernetes.api.model.GroupVersionResourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.authentication.UserInfoBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.Quarkus;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.RetryUtil;
import io.stackgres.common.StackGresKeys;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceWriter;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReconciliator<T extends CustomResource<?, ?>, R extends AdmissionReview<T>> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(
      AbstractReconciliator.class.getPackage().getName());

  private static final String STACKGRES_IO_RECONCILIATION = StackGresKeys
      .RECONCILIATION_PAUSE_KEY;

  private final String operatorServiceAccountName;
  private final CustomResourceScanner<T> scanner;
  private final CustomResourceFinder<T> finder;
  private final boolean enableReconciliationWebhooks;
  private final ObjectMapper objectMapper;
  private final MutationPipeline<T, R> mutationPipeline;
  private final ValidationPipeline<R> validationPipeline;
  private final CustomResourceWriter<T> writer;
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
      OperatorPropertyContext operatorPropertyContext,
      CustomResourceScanner<T> scanner,
      CustomResourceFinder<T> finder,
      ObjectMapper objectMapper,
      MutationPipeline<T, R> mutationPipeline,
      ValidationPipeline<R> validationPipeline,
      CustomResourceWriter<T> writer,
      AbstractConciliator<T> conciliator,
      DeployedResourcesCache deployedResourcesCache,
      HandlerDelegator<T> handlerDelegator,
      KubernetesClient client,
      OperatorLockHolder operatorLockReconciliator,
      ReconciliatorWorkerThreadPool reconciliatorWorkerThreadPool,
      Metrics metrics,
      String reconciliationName) {
    this.operatorServiceAccountName =
        operatorPropertyContext.getString(OperatorProperty.OPERATOR_NAME);
    this.scanner = scanner;
    this.finder = finder;
    this.enableReconciliationWebhooks =
        operatorPropertyContext.getBoolean(OperatorProperty.ENABLE_RECONCILIATION_WEBHOOKS);
    this.objectMapper = objectMapper;
    this.mutationPipeline = mutationPipeline;
    this.validationPipeline = validationPipeline;
    this.writer = writer;
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
    this.operatorServiceAccountName = null;
    this.scanner = null;
    this.finder = null;
    this.enableReconciliationWebhooks = false;
    this.objectMapper = null;
    this.mutationPipeline = null;
    this.validationPipeline = null;
    this.writer = null;
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
        .filter(t -> Optional.ofNullable(t.v1.getMetadata().getAnnotations())
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
        Quarkus.asyncExit(1);
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
      final T configUnmutated;
      if (load) {
        var configFound = finder.findByNameAndNamespace(
            metadata.getName(), metadata.getNamespace());
        if (configFound.isEmpty()) {
          LOGGER.debug("{} not found, skipping reconciliation", configId);
          return;
        }
        configUnmutated = configFound.get();
      } else {
        configUnmutated = configKey;
      }
      final T config;
      if (this.enableReconciliationWebhooks) {
        final T mutatedAndValidatedConfig = mutateAndValidate(configUnmutated);
        if (Objects.equals(
            configUnmutated,
            mutatedAndValidatedConfig)
            && Optional.of(configUnmutated.getMetadata())
            .map(ObjectMeta::getAnnotations)
            .map(annotations -> annotations.containsKey(StackGresKeys.PREVIOUS_KEY))
            .orElse(false)) {
          config = mutatedAndValidatedConfig;
        } else {
          config = writer.update(
              configUnmutated,
              currentConfig -> {
                String previous = fromResource(currentConfig);
                if (Objects.equals(
                    currentConfig.getMetadata().getResourceVersion(),
                    mutatedAndValidatedConfig.getMetadata().getResourceVersion())) {
                  currentConfig.setMetadata(mutatedAndValidatedConfig.getMetadata());
                  setSpecAndStatus(currentConfig, mutatedAndValidatedConfig);
                } else {
                  mutateAndValidate(currentConfig);
                }
                if (currentConfig.getMetadata().getAnnotations() == null) {
                  currentConfig.getMetadata().setAnnotations(new HashMap<>());
                }
                currentConfig.getMetadata().getAnnotations()
                    .put(StackGresKeys.PREVIOUS_KEY, previous);
              });
        }
      } else {
        config = configUnmutated;
      }
      onPreReconciliation(config);
      LOGGER.debug("Checking reconciliation status of {}", configId);
      ReconciliationResult result = conciliator.evalReconciliationState(config);
      if (!result.isUpToDate()) {
        LOGGER.debug("{} it's not up to date. Reconciling", configId);

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
    } catch (RuntimeException ex) {
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

  private T mutateAndValidate(final T configUnmutated) {
    AdmissionRequest<T> admissionRequest =
        new AdmissionRequest<T>();
    admissionRequest.setDryRun(false);
    admissionRequest.setKind(
        new GroupVersionKindBuilder()
        .withGroup(configUnmutated.getGroup())
        .withKind(configUnmutated.getKind())
        .withVersion(configUnmutated.getVersion())
        .build());
    admissionRequest.setName(configUnmutated.getMetadata().getName());
    admissionRequest.setNamespace(configUnmutated.getMetadata().getNamespace());
    admissionRequest.setObject(configUnmutated);
    final T oldConfig = Optional.of(configUnmutated.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(StackGresKeys.PREVIOUS_KEY))
        .map(this::toResource)
        .orElse(null);
    admissionRequest.setOldObject(oldConfig);
    admissionRequest.setOperation(oldConfig == null ? Operation.CREATE : Operation.UPDATE);
    admissionRequest.setOptions(
        new GroupVersionKindBuilder()
        .withGroup(configUnmutated.getGroup())
        .withKind(configUnmutated.getKind())
        .withVersion(configUnmutated.getVersion())
        .build());
    admissionRequest.setRequestKind(
        new GroupVersionKindBuilder()
        .withGroup(configUnmutated.getGroup())
        .withKind(configUnmutated.getKind())
        .withVersion(configUnmutated.getVersion())
        .build());
    admissionRequest.setRequestResource(
        new GroupVersionResourceBuilder()
        .withGroup(configUnmutated.getGroup())
        .withResource(configUnmutated.getFullResourceName())
        .withVersion(configUnmutated.getVersion())
        .build());
    admissionRequest.setRequestSubResource(null);
    admissionRequest.setResource(
        new GroupVersionResourceBuilder()
        .withGroup(configUnmutated.getGroup())
        .withResource(configUnmutated.getFullResourceName())
        .withVersion(configUnmutated.getVersion())
        .build());
    admissionRequest.setSubResource(null);
    admissionRequest.setUid(UUID.fromString(configUnmutated.getMetadata().getUid()));
    admissionRequest.setUserInfo(
        new UserInfoBuilder()
        .withUsername(ResourceUtil.getServiceAccountUsername(operatorServiceAccountName))
        .build());
    R admissionReview = createAdmissionReview();
    admissionReview.setRequest(admissionRequest);
    final T config = copyResource(configUnmutated);
    mutationPipeline.mutate(admissionReview, config);
    admissionRequest.setObject(config);
    try {
      validationPipeline.validate(admissionReview);
    } catch (ValidationFailed ex) {
      throw new RuntimeException(ex);
    }
    return config;
  }

  protected abstract R createAdmissionReview();

  protected abstract void setSpecAndStatus(T currentConfig, T mutatedAndValidatedConfig);

  protected abstract void onPreReconciliation(T config);

  protected abstract void onPostReconciliation(T config);

  protected abstract void onConfigCreated(T context, ReconciliationResult result);

  protected abstract void onConfigUpdated(T context, ReconciliationResult result);

  protected abstract void onError(Exception e, T context);

  private T toResource(String resource) {
    try {
      return objectMapper.readValue(resource, getResourceClass());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private T copyResource(T resource) {
    try {
      return objectMapper.treeToValue(objectMapper.valueToTree(resource), getResourceClass());
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private String fromResource(T resource) {
    try {
      return objectMapper.valueToTree(resource).toString();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  protected abstract Class<T> getResourceClass();

  public KubernetesClient getClient() {
    return client;
  }

}
