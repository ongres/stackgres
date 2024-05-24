/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.api.model.GroupVersionKind;
import io.fabric8.kubernetes.api.model.GroupVersionResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.authentication.UserInfo;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractReconciliator<T extends CustomResource<?, ?>, R extends AdmissionReview<T>> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(
      AbstractReconciliator.class.getPackage().getName());

  private final String operatorName = OperatorProperty.OPERATOR_NAME.getString();

  private final MutationPipeline<T, R> mutatingPipeline;
  private final ValidationPipeline<R> validatingPipeline;
  private final CustomResourceScanner<T> scanner;
  private final CustomResourceFinder<T> finder;
  private final AbstractConciliator<T> conciliator;
  private final DeployedResourcesCache deployedResourcesCache;
  private final HandlerDelegator<T> handlerDelegator;
  private final KubernetesClient client;
  private final ObjectMapper objectMapper;
  private final OperatorLockHolder operatorLockReconciliator;
  private final String reconciliationName;
  private final ExecutorService executorService;
  private final AtomicReference<List<Optional<T>>> atomicReference =
      new AtomicReference<List<Optional<T>>>(List.of());
  private final ArrayBlockingQueue<Boolean> arrayBlockingQueue = new ArrayBlockingQueue<>(1);

  private final CompletableFuture<Void> stopped = new CompletableFuture<>();
  private boolean close = false;

  protected AbstractReconciliator(
      MutationPipeline<T, R> mutatingPipeline,
      ValidationPipeline<R> validatingPipeline,
      CustomResourceScanner<T> scanner,
      CustomResourceFinder<T> finder,
      AbstractConciliator<T> conciliator,
      DeployedResourcesCache deployedResourcesCache,
      HandlerDelegator<T> handlerDelegator,
      KubernetesClient client,
      ObjectMapper objectMapper,
      OperatorLockHolder operatorLockReconciliator,
      String reconciliationName) {
    this.mutatingPipeline = mutatingPipeline; 
    this.validatingPipeline = validatingPipeline; 
    this.scanner = scanner;
    this.finder = finder;
    this.conciliator = conciliator;
    this.deployedResourcesCache = deployedResourcesCache;
    this.handlerDelegator = handlerDelegator;
    this.client = client;
    this.objectMapper = objectMapper;
    this.reconciliationName = reconciliationName;
    this.operatorLockReconciliator = operatorLockReconciliator;
    this.executorService = Executors.newSingleThreadExecutor(
        r -> new Thread(r, reconciliationName + "-ReconciliationLoop"));
  }

  public AbstractReconciliator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.mutatingPipeline = null; 
    this.validatingPipeline = null; 
    this.scanner = null;
    this.finder = null;
    this.conciliator = null;
    this.deployedResourcesCache = null;
    this.handlerDelegator = null;
    this.client = null;
    this.objectMapper = null;
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
            .map(annotations -> annotations.get(StackGresContext.RECONCILIATION_PAUSE_KEY))
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
      final T mutatedConfig;
      if (OperatorProperty.DISABLE_WEBHOOKS.getBoolean()
          && !resourceHashIsValid(config)) {
        Optional<T> previousConfig = Optional.of(config)
            .map(CustomResource::getMetadata)
            .map(ObjectMeta::getAnnotations)
            .map(annotations -> annotations.get(StackGresContext.VALID_RESOURCE))
            .map(client.getKubernetesSerialization()::unmarshal);
        LOGGER.debug("Mutating {} for {}", configId, previousConfig
            .map(ignored -> Operation.UPDATE).orElse(Operation.CREATE));
        T configCopy = client.getKubernetesSerialization().clone(config);
        mutatedConfig = mutatingPipeline.mutate(getReview(config, previousConfig), configCopy);
        LOGGER.debug("Validating {} for {}", configId, previousConfig
            .map(ignored -> Operation.UPDATE).orElse(Operation.CREATE));
        validatingPipeline.validate(getReview(mutatedConfig, previousConfig));
        updateValidatedResource(mutatedConfig);
      } else {
        mutatedConfig = config;
      }
      onPreReconciliation(mutatedConfig);
      LOGGER.debug("Checking reconciliation status of {}", configId);
      ReconciliationResult result = conciliator.evalReconciliationState(mutatedConfig);
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
                var created = handlerDelegator.create(mutatedConfig, resource);
                deployedResourcesCache.put(resource, created);
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
                LOGGER.info("Patching {} {}.{}",
                    resource.v2.getKind(),
                    resource.v2.getMetadata().getNamespace(),
                    resource.v2.getMetadata().getName());
                var patched = handlerDelegator.patch(mutatedConfig, resource.v1, resource.v2);
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
                handlerDelegator.delete(mutatedConfig, resource);
              } catch (Exception ex) {
                exceptions.add(ex);
              }
            });
        if (result.getDeletions().isEmpty() && result.getPatches().isEmpty()) {
          onConfigCreated(mutatedConfig, result);
        } else {
          onConfigUpdated(mutatedConfig, result);
        }
      } else {
        LOGGER.debug("{} it's up to date", configId);
      }

      onPostReconciliation(mutatedConfig);
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

  private boolean resourceHashIsValid(final T config) {
    Optional<String> currentValidResourceHash = Optional.of(config)
        .map(CustomResource::getMetadata)
        .map(ObjectMeta::getAnnotations)
        .map(annotations -> annotations.get(StackGresContext.VALID_RESOURCE_HASH));
    return currentValidResourceHash
        .map(hash -> StackGresUtil.getValidResourceHash(config, objectMapper).equals(hash))
        .orElse(false);
  }

  private void updateValidatedResource(final T mutatedConfig) {
    String validResource = StackGresUtil.getValidResourceAsJson(mutatedConfig, objectMapper);
    String validResourceHash = StackGresUtil.getValidResourceHash(mutatedConfig, objectMapper);
    mutatedConfig.getMetadata().setAnnotations(
        Seq.seq(
            Optional.ofNullable(mutatedConfig.getMetadata().getAnnotations())
            .orElse(Map.of()))
        .filter(annotation -> Objects.equals(annotation.v1(), StackGresContext.VALID_RESOURCE))
        .filter(annotation -> Objects.equals(annotation.v1(), StackGresContext.VALID_RESOURCE_HASH))
        .append(Tuple.tuple(
            StackGresContext.VALID_RESOURCE,
            validResource))
        .append(Tuple.tuple(
            StackGresContext.VALID_RESOURCE_HASH,
            validResourceHash))
        .toMap(Tuple2::v1, Tuple2::v2));
    client.resource(mutatedConfig)
        .lockResourceVersion(mutatedConfig.getMetadata().getResourceVersion())
        .update();
  }

  private R getReview(T config, Optional<T> previousConfig) {
    R review = createReview();
    var request = new AdmissionRequest<T>();
    review.setRequest(request);
    request.setDryRun(false);
    request.setUid(UUID.fromString(config.getMetadata().getUid()));
    request.setKind(new GroupVersionKind());
    request.getKind().setGroup(HasMetadata.getGroup(config.getClass()));
    request.getKind().setVersion(HasMetadata.getVersion(config.getClass()));
    request.getKind().setKind(HasMetadata.getKind(config.getClass()));
    request.setResource(new GroupVersionResource());
    request.getResource().setGroup(HasMetadata.getGroup(config.getClass()));
    request.getResource().setResource(HasMetadata.getPlural(config.getClass()));
    request.getResource().setVersion(HasMetadata.getVersion(config.getClass()));
    request.setName(config.getMetadata().getName());
    request.setNamespace(config.getMetadata().getNamespace());
    request.setOperation(previousConfig.isEmpty() ? Operation.CREATE : Operation.UPDATE);
    request.setObject(config);
    request.setOldObject(previousConfig.orElse(null));
    request.setUserInfo(new UserInfo());
    request.getUserInfo().setGroups(List.of(
        "system:serviceaccounts",
        "system:serviceaccounts:" + operatorName,
        "system:authenticated"));
    request.getUserInfo().setUsername(operatorName);
    return review;
  }

  protected abstract R createReview();

  protected abstract void onPreReconciliation(T config);

  protected abstract void onPostReconciliation(T config);

  protected abstract void onConfigCreated(T context, ReconciliationResult result);

  protected abstract void onConfigUpdated(T context, ReconciliationResult result);

  protected abstract void onError(Exception e, T context);

  public KubernetesClient getClient() {
    return client;
  }

}
