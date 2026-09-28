/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.ClusterEventReason;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.event.EventEmitter;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.CustomResourceWriter;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operator.app.OperatorLockHolder;
import io.stackgres.operator.common.ClusterPatchResumer;
import io.stackgres.operator.common.ClusterRolloutUtil;
import io.stackgres.operator.common.Metrics;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractReconciliator;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.HandlerDelegator;
import io.stackgres.operator.conciliation.ReconciliationResult;
import io.stackgres.operator.conciliation.ReconciliatorWorkerThreadPool;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operator.conciliation.cluster.context.ClusterPostgresVersionContextAppender;
import io.stackgres.operator.conciliation.factory.dbops.DbOpsClusterRollout;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operatorframework.admissionwebhook.mutating.MutationPipeline;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.slf4j.helpers.MessageFormatter;

@ApplicationScoped
public class ClusterReconciliator
    extends AbstractReconciliator<StackGresCluster, StackGresClusterReview> {

  /**
   * The finalizer set by Kubernetes when a resource is deleted with the Orphan propagation policy.
   */
  static final String ORPHAN_FINALIZER = "orphan";

  /**
   * The time a Pod is waited for after its termination grace period has elapsed.
   */
  static final Duration POD_TERMINATION_TIMEOUT_MARGIN = Duration.ofMinutes(2);

  private static final String STATEFUL_SET_KIND = HasMetadata.getKind(StatefulSet.class);

  @Dependent
  static class Parameters {
    @Inject OperatorPropertyContext operatorPropertyContext;
    @Inject CustomResourceScanner<StackGresCluster> scanner;
    @Inject CustomResourceFinder<StackGresCluster> finder;
    @Inject MutationPipeline<StackGresCluster, StackGresClusterReview> mutationPipeline;
    @Inject ValidationPipeline<StackGresClusterReview> validationPipeline;
    @Inject CustomResourceWriter<StackGresCluster> writer;
    @Inject AbstractConciliator<StackGresCluster> conciliator;
    @Inject DeployedResourcesCache deployedResourcesCache;
    @Inject HandlerDelegator<StackGresCluster> handlerDelegator;
    @Inject KubernetesClient client;
    @Inject StatusManager<StackGresCluster, Condition> statusManager;
    @Inject EventEmitter<StackGresCluster> eventController;
    @Inject CustomResourceWriter<StackGresCluster> clusterWriter;
    @Inject ObjectMapper objectMapper;
    @Inject OperatorLockHolder operatorLockReconciliator;
    @Inject ReconciliatorWorkerThreadPool reconciliatorWorkerThreadPool;
    @Inject Metrics metrics;
    @Inject LabelFactoryForCluster labelFactory;
    @Inject ResourceFinder<StatefulSet> statefulSetFinder;
    @Inject ResourceWriter<StatefulSet> statefulSetWriter;
    @Inject ResourceScanner<Pod> podScanner;
    @Inject ResourceWriter<Pod> podWriter;
  }

  private final StatusManager<StackGresCluster, Condition> statusManager;
  private final EventEmitter<StackGresCluster> eventController;
  private final CustomResourceWriter<StackGresCluster> clusterWriter;
  private final ClusterPatchResumer patchResumer;
  private final LabelFactoryForCluster labelFactory;
  private final ResourceFinder<StatefulSet> statefulSetFinder;
  private final ResourceWriter<StatefulSet> statefulSetWriter;
  private final ResourceScanner<Pod> podScanner;
  private final ResourceWriter<Pod> podWriter;
  Clock clock = Clock.systemUTC();

  @Inject
  public ClusterReconciliator(Parameters parameters) {
    super(
        parameters.operatorPropertyContext,
        parameters.scanner,
        parameters.finder,
        parameters.objectMapper,
        parameters.mutationPipeline,
        parameters.validationPipeline,
        parameters.writer,
        parameters.conciliator,
        parameters.deployedResourcesCache,
        parameters.handlerDelegator,
        parameters.client,
        parameters.operatorLockReconciliator,
        parameters.reconciliatorWorkerThreadPool,
        parameters.metrics,
        StackGresCluster.KIND);
    this.statusManager = parameters.statusManager;
    this.eventController = parameters.eventController;
    this.clusterWriter = parameters.clusterWriter;
    this.patchResumer = new ClusterPatchResumer(parameters.objectMapper);
    this.labelFactory = parameters.labelFactory;
    this.statefulSetFinder = parameters.statefulSetFinder;
    this.statefulSetWriter = parameters.statefulSetWriter;
    this.podScanner = parameters.podScanner;
    this.podWriter = parameters.podWriter;
  }

  @Override
  protected void setSpecAndStatus(StackGresCluster currentConfig, StackGresCluster mutatedAndValidatedConfig) {
    currentConfig.setSpec(mutatedAndValidatedConfig.getSpec());
    currentConfig.setStatus(mutatedAndValidatedConfig.getStatus());
  }

  @Override
  protected StackGresClusterReview createAdmissionReview() {
    return new StackGresClusterReview();
  }

  @Override
  protected Class<StackGresCluster> getResourceClass() {
    return StackGresCluster.class;
  }

  void onStart(@Observes StartupEvent ev) {
    start();
  }

  void onStop(@Observes ShutdownEvent ev) {
    stop();
  }

  @Override
  protected void reconciliationCycle(StackGresCluster configKey, int retry, boolean load) {
    super.reconciliationCycle(configKey, retry, load);
  }

  @Override
  protected List<String> getFinalizers() {
    return List.of(StackGresContext.WAIT_PODS_TERMINATION_FINALIZER);
  }

  @Override
  protected boolean onFinalizer(StackGresCluster cluster, String finalizer) {
    if (!StackGresContext.WAIT_PODS_TERMINATION_FINALIZER.equals(finalizer)) {
      throw new RuntimeException("Unknown finalizer " + finalizer);
    }
    final String namespace = cluster.getMetadata().getNamespace();
    final String name = cluster.getMetadata().getName();
    // Deleting an SGCluster does not terminate the Pods of its StatefulSet: they are removed
    // asynchronously by the Kubernetes garbage collector once the SGCluster is gone and keep
    // running for the whole termination grace period. While they run their Patroni is still able to
    // write to the DCS endpoints of a cluster created with the same name, taking the leader lock
    // and leaving the initialize key set on a cluster that will then never be bootstrapped
    // (see https://gitlab.com/ongresinc/stackgres/-/issues/3240). Scale the cluster to 0 instances
    // and wait for its Pods to be gone before the deletion of the SGCluster is allowed to complete.
    //
    // Only what the garbage collector would delete anyway is removed: when the SGCluster is deleted
    // orphaning its dependents (propagation policy Orphan) the StatefulSet and its Pods are left
    // untouched.
    if (Optional.ofNullable(cluster.getMetadata().getFinalizers())
        .map(finalizers -> finalizers.contains(ORPHAN_FINALIZER))
        .orElse(false)) {
      LOGGER.debug("SGCluster {}.{} is being deleted orphaning its dependents,"
          + " not waiting for its Pods to terminate", namespace, name);
      return true;
    }
    final Optional<StatefulSet> foundStatefulSet =
        statefulSetFinder.findByNameAndNamespace(name, namespace);
    // The garbage collector may have already processed the orphan finalizer, removing the owner
    // reference of the StatefulSet.
    if (foundStatefulSet.isPresent() && !isOwnedBy(foundStatefulSet.get(), cluster)) {
      LOGGER.debug("StatefulSet {}.{} is not owned by SGCluster {}.{},"
          + " not waiting for its Pods to terminate", namespace, name, namespace, name);
      return true;
    }
    // The StatefulSet is scaled directly instead of setting .spec.instances to 0 and reconciling
    // the SGCluster: generating the required resources may fail when a resource referenced by the
    // SGCluster has been removed together with it, and updating the spec is rejected while an
    // SGDbOps holds the lock of the cluster. Both would block the deletion forever.
    foundStatefulSet
        .filter(statefulSet -> Optional.of(statefulSet.getSpec())
            .map(StatefulSetSpec::getReplicas)
            .map(replicas -> replicas > 0)
            .orElse(false))
        .ifPresent(statefulSet -> {
          LOGGER.debug("Scaling StatefulSet {}.{} to 0 instances before deleting SGCluster",
              namespace, name);
          statefulSetWriter.update(new StatefulSetBuilder(statefulSet)
              .editSpec()
              .withReplicas(0)
              .endSpec()
              .build());
        });
    var pods = podScanner.getResourcesInNamespaceWithLabels(
        namespace, labelFactory.clusterLabels(cluster))
        .stream()
        .filter(pod -> isClusterPod(pod, cluster))
        .toList();
    if (pods.isEmpty()) {
      return true;
    }
    // Scaling down the StatefulSet does not remove the Pods that have been marked as non
    // disruptable, since those do not match its selector anymore and are released by it. Delete
    // any leftover Pod.
    pods.stream()
        .filter(pod -> pod.getMetadata().getDeletionTimestamp() == null)
        .forEach(pod -> {
          LOGGER.debug("Deleting Pod {}.{} before deleting SGCluster {}.{}",
              namespace, pod.getMetadata().getName(), namespace, name);
          podWriter.delete(pod);
        });
    // A Pod whose termination grace period has elapsed long ago is not being terminated by its
    // kubelet (e.g. its node is unreachable). Pods are never force deleted, instead the finalizer
    // is removed so that the deletion of the SGCluster does not block forever.
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
      eventController.sendEvent(ClusterEventReason.CLUSTER_PODS_TERMINATION_TIMEOUT,
          "Pods " + String.join(", ", stuckPods) + " of SGCluster " + namespace + "." + name
          + " did not terminate within their termination grace period, the deletion of the"
          + " SGCluster is completed without waiting for them", cluster);
      return true;
    }
    LOGGER.debug("Waiting for {} Pods of SGCluster {}.{} to terminate",
        pods.size(), namespace, name);
    return false;
  }

  private boolean isOwnedBy(StatefulSet statefulSet, StackGresCluster cluster) {
    return Optional.ofNullable(statefulSet.getMetadata().getOwnerReferences())
        .stream()
        .flatMap(List::stream)
        .anyMatch(ownerReference -> Objects.equals(
            ownerReference.getUid(), cluster.getMetadata().getUid()));
  }

  /**
   * A Pod of the cluster is owned by its StatefulSet or by the SGCluster itself, or has no owner
   * when it has been released by the StatefulSet after being marked as non disruptable.
   */
  private boolean isClusterPod(Pod pod, StackGresCluster cluster) {
    return Optional.ofNullable(pod.getMetadata().getOwnerReferences())
        .filter(Predicate.not(List::isEmpty))
        .map(ownerReferences -> ownerReferences.stream()
            .anyMatch(ownerReference -> Objects.equals(
                ownerReference.getUid(), cluster.getMetadata().getUid())
                || (Objects.equals(ownerReference.getKind(), STATEFUL_SET_KIND)
                && Objects.equals(ownerReference.getName(), cluster.getMetadata().getName()))))
        .orElse(true);
  }

  @Override
  protected void onPreReconciliation(StackGresCluster config) {
    if (Optional.of(config)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPostgresVersion)
        .map(ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.keySet()::contains)
        .orElse(false)) {
      eventController.sendEvent(ClusterEventReason.CLUSTER_SECURITY_WARNING,
          "Cluster " + config.getMetadata().getNamespace() + "."
              + config.getMetadata().getName() + " is using PostgreSQL "
              + config.getSpec().getPostgres().getVersion() + ". "
              + ClusterPostgresVersionContextAppender.BUGGY_PG_VERSIONS.get(
                  config.getSpec().getPostgres().getVersion()), config);
    }
  }

  @Override
  protected void onPostReconciliation(StackGresCluster config) {
    statusManager.refreshCondition(config);

    clusterWriter.update(config,
        (currentCluster) -> {
          currentCluster.getMetadata().setAnnotations(
              Seq.seq(
                  Optional.ofNullable(currentCluster.getMetadata().getAnnotations())
                  .map(Map::entrySet)
                  .stream()
                  .flatMap(Set::stream)
                  .filter(annotation -> !Objects.equals(annotation.getKey(), StackGresContext.VERSION_KEY))
                  .filter(annotation -> !DbOpsClusterRollout.ROLLOUT_DBOPS_KEYS.contains(annotation.getKey())
                      || Optional.ofNullable(config.getStatus())
                      .map(StackGresClusterStatus::getDbOps)
                      .map(StackGresClusterDbOpsStatus::getName)
                      .map(name -> !ClusterRolloutUtil.DBOPS_NOT_FOUND_NAME.equals(name))
                      .orElse(true)))
              .append(Optional.ofNullable(config.getMetadata().getAnnotations())
                  .map(Map::entrySet)
                  .stream()
                  .flatMap(Set::stream)
                  .filter(annotation -> Objects.equals(annotation.getKey(), StackGresContext.VERSION_KEY)))
              .toMap(Map.Entry::getKey, Map.Entry::getValue));
          var targetOs = Optional.ofNullable(currentCluster.getStatus())
              .map(StackGresClusterStatus::getOs)
              .orElse(null);
          var targetArch = Optional.ofNullable(currentCluster.getStatus())
              .map(StackGresClusterStatus::getArch)
              .orElse(null);
          var targetPodStatuses = Optional.ofNullable(currentCluster.getStatus())
              .map(StackGresClusterStatus::getPodStatuses)
              .orElse(null);
          var targetDbOps = Optional.ofNullable(currentCluster.getStatus())
              .map(StackGresClusterStatus::getDbOps)
              .orElse(null);
          var targetManagedSql = Optional.ofNullable(currentCluster.getStatus())
              .map(StackGresClusterStatus::getManagedSql)
              .orElse(null);
          if (config.getStatus() != null) {
            config.getStatus().setOs(targetOs);
            config.getStatus().setArch(targetArch);
            config.getStatus().setPodStatuses(targetPodStatuses);
            config.getStatus().setDbOps(targetDbOps);
            config.getStatus().setManagedSql(targetManagedSql);
            currentCluster.setStatus(config.getStatus());
          }
        });
  }

  @Override
  protected void onConfigCreated(StackGresCluster cluster, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(cluster, result);
    eventController.sendEvent(ClusterEventReason.CLUSTER_CREATED,
        "SGCluster " + cluster.getMetadata().getNamespace() + "."
            + cluster.getMetadata().getName() + " created: " + resourceChanged, cluster);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), cluster);
  }

  @Override
  protected void onConfigUpdated(StackGresCluster cluster, ReconciliationResult result) {
    final String resourceChanged = patchResumer.resourceChanged(cluster, result);
    eventController.sendEvent(ClusterEventReason.CLUSTER_UPDATED,
        "SGCluster " + cluster.getMetadata().getNamespace() + "."
            + cluster.getMetadata().getName() + " updated: " + resourceChanged, cluster);
    statusManager.updateCondition(
        ClusterStatusCondition.FALSE_FAILED.getCondition(), cluster);
  }

  @Override
  protected void onError(Exception ex, StackGresCluster cluster) {
    String message = MessageFormatter.arrayFormat(
        "SGCluster reconciliation cycle failed",
        new String[]{
        }).getMessage();
    eventController.sendEvent(ClusterEventReason.CLUSTER_CONFIG_ERROR,
        message + ": " + ex.getMessage(), cluster);
  }

}
