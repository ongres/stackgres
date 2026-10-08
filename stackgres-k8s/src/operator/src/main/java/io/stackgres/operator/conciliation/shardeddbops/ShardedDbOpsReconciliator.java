/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.ShardedDbOpsUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgshardeddbops.ShardedDbOpsEventReason;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.labels.LabelFactoryForShardedDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceScheduler;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.common.PatchResumer;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.ReconciliatorWorkerThreadPool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ShardedDbOpsReconciliator
    extends AbstractReconciliator<StackGresShardedDbOps> {

  /**
   * The finalizer set by Kubernetes when a resource is deleted with the Orphan propagation policy.
   */
  static final String ORPHAN_FINALIZER = "orphan";

  /**
   * The time a Pod is waited for after its termination grace period has elapsed.
   */
  static final Duration POD_TERMINATION_TIMEOUT_MARGIN = Duration.ofMinutes(2);

  @Dependent
  static class Parameters {
    @Inject CustomResourceScanner<StackGresShardedDbOps> scanner;
    @Inject CustomResourceFinder<StackGresShardedDbOps> finder;
    @Inject AbstractConciliator<StackGresShardedDbOps> conciliator;
    @Inject DeployedResourcesCache deployedResourcesCache;
    @Inject HandlerDelegator<StackGresShardedDbOps> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject EventEmitter<StackGresShardedDbOps> eventController;
    @Inject ShardedDbOpsStatusManager statusManager;
    @Inject CustomResourceScheduler<StackGresShardedDbOps> dbOpsScheduler;
    @Inject ObjectMapper objectMapper;
    @Inject OperatorLockHolder operatorLockReconciliator;
    @Inject ReconciliatorWorkerThreadPool reconciliatorWorkerThreadPool;
    @Inject Metrics metrics;
    @Inject LabelFactoryForShardedDbOps labelFactory;
    @Inject ResourceFinder<Job> jobFinder;
    @Inject ResourceWriter<Job> jobWriter;
    @Inject ResourceScanner<Pod> podScanner;
    @Inject ResourceWriter<Pod> podWriter;
  }

  private final EventEmitter<StackGresShardedDbOps> eventController;
  private final PatchResumer<StackGresShardedDbOps> patchResumer;
  private final ShardedDbOpsStatusManager statusManager;
  private final CustomResourceScheduler<StackGresShardedDbOps> dbOpsScheduler;
  private final LabelFactoryForShardedDbOps labelFactory;
  private final ResourceFinder<Job> jobFinder;
  private final ResourceWriter<Job> jobWriter;
  private final ResourceScanner<Pod> podScanner;
  private final ResourceWriter<Pod> podWriter;
  Clock clock = Clock.systemUTC();

  @Inject
  public ShardedDbOpsReconciliator(Parameters parameters) {
    super(parameters.scanner, parameters.finder,
        parameters.conciliator, parameters.deployedResourcesCache,
        parameters.handlerDelegator, parameters.client,
        parameters.operatorLockReconciliator,
        parameters.reconciliatorWorkerThreadPool,
        parameters.metrics,
        StackGresShardedDbOps.KIND);
    this.eventController = parameters.eventController;
    this.patchResumer = new PatchResumer<>(parameters.objectMapper);
    this.statusManager = parameters.statusManager;
    this.dbOpsScheduler = parameters.dbOpsScheduler;
    this.labelFactory = parameters.labelFactory;
    this.jobFinder = parameters.jobFinder;
    this.jobWriter = parameters.jobWriter;
    this.podScanner = parameters.podScanner;
    this.podWriter = parameters.podWriter;
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresShardedDbOps configKey, int retry, boolean load) {
    super.reconciliationCycle(configKey, retry, load);
  }

  @Override
  protected StackGresShardedDbOps updateResource(
      StackGresShardedDbOps config, Consumer<StackGresShardedDbOps> setter) {
    return dbOpsScheduler.update(config, setter);
  }

  @Override
  protected List<String> getFinalizers() {
    return List.of(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER);
  }

  @Override
  protected boolean onFinalizer(StackGresShardedDbOps dbOps, String finalizer) {
    if (!StackGresContext.WAIT_PODS_TERMINATION_FINALIZER.equals(finalizer)) {
      throw new RuntimeException("Unknown finalizer " + finalizer);
    }
    final String namespace = dbOps.getMetadata().getNamespace();
    final String name = dbOps.getMetadata().getName();
    // Deleting an SGShardedDbOps does not terminate the Pod of its Job: the Kubernetes garbage
    // collector removes the Job only once the SGShardedDbOps is gone, and the Pod keeps running for
    // the whole termination grace period. While it runs it still operates on the SGShardedCluster
    // and reports the outcome of the operation on the SGShardedDbOps that holds its name, that may
    // well be another SGShardedDbOps created with the same name in the meanwhile, marking as failed
    // an operation that has just started. Delete the Job and wait for its Pods to be gone before
    // the deletion of the SGShardedDbOps is allowed to complete.
    //
    // Only what the garbage collector would delete anyway is removed: when the SGShardedDbOps is
    // deleted orphaning its dependents (propagation policy Orphan) the Job and its Pods are left
    // untouched.
    if (Optional.ofNullable(dbOps.getMetadata().getFinalizers())
        .map(finalizers -> finalizers.contains(ORPHAN_FINALIZER))
        .orElse(false)) {
      LOGGER.debug("SGShardedDbOps {}.{} is being deleted orphaning its dependents,"
          + " not waiting for its Pods to terminate", namespace, name);
      return true;
    }
    jobFinder.findByNameAndNamespace(ShardedDbOpsUtil.jobName(dbOps), namespace)
        // The garbage collector may have already processed the orphan finalizer, removing the owner
        // reference of the Job.
        .filter(job -> isOwnedBy(job, dbOps))
        .filter(job -> job.getMetadata().getDeletionTimestamp() == null)
        .ifPresent(job -> {
          LOGGER.debug("Deleting Job {}.{} before deleting SGShardedDbOps {}.{}",
              namespace, job.getMetadata().getName(), namespace, name);
          jobWriter.delete(job);
        });
    // The Pods are looked for by the labels of the SGShardedDbOps, that hold its uid, so that the
    // Pods of the Job of another SGShardedDbOps created with the same name are never waited for.
    var pods = podScanner.getResourcesInNamespaceWithLabels(
        namespace, labelFactory.shardedDbOpsPodLabels(dbOps));
    if (pods.isEmpty()) {
      return true;
    }
    // Deleting the Job removes its Pods, but a Pod released by the Job, or left behind by a Job
    // that has already been removed, is not. Delete any leftover Pod.
    pods.stream()
        .filter(pod -> pod.getMetadata().getDeletionTimestamp() == null)
        .forEach(pod -> {
          LOGGER.debug("Deleting Pod {}.{} before deleting SGShardedDbOps {}.{}",
              namespace, pod.getMetadata().getName(), namespace, name);
          podWriter.delete(pod);
        });
    // A Pod whose termination grace period has elapsed long ago is not being terminated by its
    // kubelet (e.g. its node is unreachable). Pods are never force deleted, instead the finalizer
    // is removed so that the deletion of the SGShardedDbOps does not block forever.
    final Instant now = Instant.now(clock);
    var stuckPods = pods.stream()
        .filter(pod -> Optional.ofNullable(pod.getMetadata().getDeletionTimestamp())
            .map(Instant::parse)
            .map(deletionTimestamp -> deletionTimestamp
                .plus(POD_TERMINATION_TIMEOUT_MARGIN).isBefore(now))
            .orElse(false))
        .map(pod -> pod.getMetadata().getName())
        .toList();
    if (stuckPods.size() == pods.size()) {
      eventController.sendEvent(ShardedDbOpsEventReason.SHARDED_DBOPS_PODS_TERMINATION_TIMEOUT,
          "Pods " + String.join(", ", stuckPods) + " of SGShardedDbOps " + namespace + "." + name
          + " did not terminate within their termination grace period, the deletion of the"
          + " SGShardedDbOps is completed without waiting for them", dbOps);
      return true;
    }
    LOGGER.debug("Waiting for {} Pods of SGShardedDbOps {}.{} to terminate",
        pods.size(), namespace, name);
    return false;
  }

  private boolean isOwnedBy(Job job, StackGresShardedDbOps dbOps) {
    return Optional.ofNullable(job.getMetadata().getOwnerReferences())
        .stream()
        .flatMap(List::stream)
        .anyMatch(ownerReference -> Objects.equals(
            ownerReference.getUid(), dbOps.getMetadata().getUid()));
  }

  @Override
  protected void onPreReconciliation(StackGresShardedDbOps config) {
  }

  @Override
  protected void onPostReconciliation(StackGresShardedDbOps config) {
    dbOpsScheduler.update(config, statusManager::refreshCondition);
  }

  @Override
  protected void onConfigCreated(StackGresShardedDbOps dbOps, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(dbOps, result);
    eventController.sendEvent(ShardedDbOpsEventReason.SHARDED_DBOPS_CREATED,
        "SGShardedDbOps " + dbOps.getMetadata().getNamespace() + "."
            + dbOps.getMetadata().getName() + " created: " + resourceChanged, dbOps);
  }

  @Override
  protected void onConfigUpdated(StackGresShardedDbOps dbOps, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(dbOps, result);
    eventController.sendEvent(ShardedDbOpsEventReason.SHARDED_DBOPS_UPDATED,
        "SGShardedDbOps " + dbOps.getMetadata().getNamespace() + "."
            + dbOps.getMetadata().getName() + " updated: " + resourceChanged, dbOps);
  }

  @Override
  protected void onError(Exception ex, StackGresShardedDbOps dbOps) {
    String message = MessageFormatter.arrayFormat(
        "SGShardedDbOps reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(ShardedDbOpsEventReason.SHARDED_DBOPS_CONFIG_ERROR,
        message + ": " + ex.getMessage(), dbOps);
  }

}
