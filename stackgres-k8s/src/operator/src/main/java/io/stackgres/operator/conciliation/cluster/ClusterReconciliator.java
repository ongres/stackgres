/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    // The StatefulSet is scaled directly instead of setting .spec.instances to 0 and reconciling
    // the SGCluster: generating the required resources may fail when a resource referenced by the
    // SGCluster has been removed together with it, and updating the spec is rejected while an
    // SGDbOps holds the lock of the cluster. Both would block the deletion forever.
    statefulSetFinder.findByNameAndNamespace(name, namespace)
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
        namespace, labelFactory.clusterLabels(cluster));
    if (pods.isEmpty()) {
      return true;
    }
    // Scaling down the StatefulSet does not remove the Pods that have been marked as non
    // disruptable, since those do not match its selector anymore. Delete any leftover Pod.
    pods.stream()
        .filter(pod -> pod.getMetadata().getDeletionTimestamp() == null)
        .forEach(pod -> {
          LOGGER.debug("Deleting Pod {}.{} before deleting SGCluster {}.{}",
              namespace, pod.getMetadata().getName(), namespace, name);
          podWriter.delete(pod);
        });
    LOGGER.debug("Waiting for {} Pods of SGCluster {}.{} to terminate",
        pods.size(), namespace, name);
    return false;
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
