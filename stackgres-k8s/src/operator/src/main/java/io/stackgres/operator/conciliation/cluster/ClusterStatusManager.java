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
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.ClusterPendingRestartUtil;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReason;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.ClusterStatusCondition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterServiceBindingStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptSpec;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operator.conciliation.factory.cluster.ServiceBindingSecret;
import io.stackgres.operatorframework.resource.ConditionUpdater;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterStatusManager
    extends ConditionUpdater<StackGresCluster, Condition>
    implements StatusManager<StackGresCluster, Condition> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStatusManager.class);

  private final LabelFactoryForCluster labelFactory;

  private final CustomResourceFinder<StackGresScript> scriptFinder;

  private final KubernetesClient client;

  @Inject
  public ClusterStatusManager(
      LabelFactoryForCluster labelFactory,
      CustomResourceFinder<StackGresScript> scriptFinder,
      KubernetesClient client) {
    this.labelFactory = labelFactory;
    this.scriptFinder = scriptFinder;
    this.client = client;
  }

  private static String getClusterId(StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace() + "/" + cluster.getMetadata().getName();
  }

  @Override
  public StackGresCluster refreshCondition(StackGresCluster source) {
    if (source.getStatus() == null) {
      source.setStatus(new StackGresClusterStatus());
    }
    source.getStatus().setBinding(new StackGresClusterServiceBindingStatus());
    source.getStatus().getBinding().setName(ServiceBindingSecret.name(source));
    StatusContext context = getStatusContext(source);
    if (isPendingRestart(source, context)) {
      updateCondition(getPodRequiresRestart(), source);
    } else {
      updateCondition(getFalsePendingRestart(), source);
    }
    if (isPendingUpgrade(source)) {
      updateCondition(getClusterRequiresUpgrade(), source);
    } else {
      updateCondition(getFalsePendingUpgrade(), source);
    }
    if (Optional.of(source)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .noneMatch(ClusterStatusCondition.CLUSTER_BOOTSTRAPPED::isCondition)) {
      boolean isPlatformSet = source.getStatus() != null
          && source.getStatus().getArch() != null
          && source.getStatus().getOs() != null;
      if (isPlatformSet) {
        updateCondition(getClusterBootstrapped(), source);
      }
    }
    if (Optional.of(source)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .noneMatch(ClusterStatusCondition.CLUSTER_INITIAL_SCRIPTS_APPLIED::isCondition)) {
      boolean isInitialScriptApplied = Optional.of(source)
          .map(StackGresCluster::getSpec)
          .map(StackGresClusterSpec::getManagedSql)
          .map(StackGresClusterManagedSql::getScripts)
          .stream()
          .flatMap(List::stream)
          .map(script -> Tuple.tuple(
              script,
              scriptFinder.findByNameAndNamespace(
                  script.getSgScript(), source.getMetadata().getNamespace()),
              Optional.of(source)
              .map(StackGresCluster::getStatus)
              .map(StackGresClusterStatus::getManagedSql)
              .map(StackGresClusterManagedSqlStatus::getScripts)
              .stream()
              .flatMap(List::stream)
              .filter(scriptStatus -> Objects.equals(
                  script.getId(),
                  scriptStatus.getId()))
              .findFirst()))
          .allMatch(script -> script.v2.isPresent()
              && script.v3.isPresent()
              && script.v2
              .map(StackGresScript::getSpec)
              .map(StackGresScriptSpec::getScripts)
              .stream()
              .flatMap(List::stream)
              .allMatch(scriptEntry -> script.v3
                  .map(StackGresClusterManagedScriptEntryStatus::getScripts)
                  .stream()
                  .flatMap(List::stream)
                  .filter(scriptEntryStatus -> Objects.equals(
                      scriptEntry.getId(),
                      scriptEntryStatus.getId()))
                  .filter(scriptEntryStatus -> ManagedSqlUtil
                      .isScriptEntryUpToDate(scriptEntry, scriptEntryStatus))
                  .findFirst()
                  .isPresent()));
      if (isInitialScriptApplied) {
        updateCondition(getClusterInitialScriptApplied(), source);
      }
    }
    if (source.getStatus() != null
        && source.getStatus().getArch() != null
        && source.getStatus().getOs() != null
        && source.getStatus().getPodStatuses() != null
        && source.getSpec().getToInstallPostgresExtensions() != null) {
      source.getStatus().getPodStatuses()
          .stream()
          .filter(StackGresClusterPodStatus::getPrimary)
          .flatMap(podStatus -> source.getSpec().getToInstallPostgresExtensions().stream()
              .filter(toInstallExtension -> podStatus
                  .getInstalledPostgresExtensions().stream()
                  .noneMatch(toInstallExtension::equals))
              .map(toInstallExtension -> Tuple.tuple(
                  toInstallExtension,
                  podStatus.getInstalledPostgresExtensions().stream()
                  .filter(installedExtension -> Objects.equals(
                      installedExtension.getName(),
                      toInstallExtension.getName()))
                  .findFirst())))
          .filter(t -> t.v2.isPresent())
          .map(t -> t.map2(Optional::get))
          .forEach(t -> t.v1.setBuild(t.v2.getBuild()));
    }
    source.getStatus().setInstances(context.clusterPods().size());
    source.getStatus().setLabelSelector(labelFactory.clusterLabels(source)
        .entrySet()
        .stream()
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining(",")));
    return source;
  }

  /**
   * Check pending restart status condition.
   */
  public boolean isPendingRestart(StackGresCluster cluster, StatusContext context) {
    RestartReasons reasons = ClusterPendingRestartUtil.getRestartReasons(
        context.clusterPodStatuses(), context.clusterStatefulSet(), context.clusterPods());
    for (RestartReason reason : reasons.getReasons()) {
      switch (reason) {
        case PATRONI:
          LOGGER.debug("Cluster {} requires restart due to patroni's indication",
              getClusterId(cluster));
          break;
        case POD_STATUS:
          LOGGER.debug("Cluster {} requires restart due to pod status indication",
              getClusterId(cluster));
          break;
        case STATEFULSET:
          LOGGER.debug("Cluster {} requires restart due to pod template changes",
              getClusterId(cluster));
          break;
        default:
          break;
      }
    }
    return reasons.requiresRestart();
  }

  private StatusContext getStatusContext(StackGresCluster cluster) {
    List<StackGresClusterPodStatus> clusterPodStatuses = Optional
        .ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getPodStatuses)
        .orElse(List.of());
    Optional<StatefulSet> clusterStatefulSet = getClusterStatefulSet(cluster);
    List<Pod> clusterPods = getClusterPods(cluster);
    StatusContext context = new StatusContext(clusterPodStatuses, clusterStatefulSet, clusterPods);
    return context;
  }

  /**
   * Check pending upgrade status condition.
   */
  public boolean isPendingUpgrade(StackGresCluster cluster) {
    if (Optional.of(cluster.getMetadata())
        .map(ObjectMeta::getAnnotations)
        .stream()
        .map(Map::entrySet)
        .flatMap(Set::stream)
        .anyMatch(e -> e.getKey().equals(StackGresContext.VERSION_KEY)
            && !e.getValue().equals(StackGresProperty.OPERATOR_VERSION.getString()))) {
      LOGGER.debug("Cluster {} requires upgrade since it is using an old operator version",
          getClusterId(cluster));
      return true;
    }
    return false;
  }

  private Optional<StatefulSet> getClusterStatefulSet(StackGresCluster cluster) {
    return Optional.ofNullable(client.apps().statefulSets()
        .inNamespace(cluster.getMetadata().getNamespace())
        .withName(cluster.getMetadata().getName())
        .get())
        .stream()
        .filter(sts -> sts.getMetadata().getOwnerReferences()
            .stream().anyMatch(ownerReference -> ownerReference.getKind()
                .equals(StackGresCluster.KIND)
                && ownerReference.getName().equals(cluster.getMetadata().getName())
                && ownerReference.getUid().equals(cluster.getMetadata().getUid())))
        .findFirst();
  }

  private List<Pod> getClusterPods(StackGresCluster cluster) {
    final Map<String, String> podClusterLabels =
        labelFactory.clusterLabels(cluster);

    return client.pods().inNamespace(cluster.getMetadata().getNamespace())
        .withLabels(podClusterLabels)
        .list()
        .getItems()
        .stream()
        .toList();
  }

  @Override
  protected List<Condition> getConditions(
      StackGresCluster source) {
    return Optional.ofNullable(source.getStatus())
        .map(StackGresClusterStatus::getConditions)
        .orElse(List.of());
  }

  @Override
  protected void setConditions(
      StackGresCluster source,
      List<Condition> conditions) {
    if (source.getStatus() == null) {
      source.setStatus(new StackGresClusterStatus());
    }
    source.getStatus().setConditions(conditions);
  }

  protected Condition getFalsePendingRestart() {
    return ClusterStatusCondition.FALSE_PENDING_RESTART.getCondition();
  }

  protected Condition getPodRequiresRestart() {
    return ClusterStatusCondition.POD_REQUIRES_RESTART.getCondition();
  }

  protected Condition getFalsePendingUpgrade() {
    return ClusterStatusCondition.FALSE_PENDING_UPGRADE.getCondition();
  }

  protected Condition getClusterRequiresUpgrade() {
    return ClusterStatusCondition.CLUSTER_REQUIRES_UPGRADE.getCondition();
  }

  protected Condition getClusterBootstrapped() {
    return ClusterStatusCondition.CLUSTER_BOOTSTRAPPED.getCondition();
  }

  protected Condition getClusterInitialScriptApplied() {
    return ClusterStatusCondition.CLUSTER_INITIAL_SCRIPTS_APPLIED.getCondition();
  }

  record StatusContext(
      List<StackGresClusterPodStatus> clusterPodStatuses,
      Optional<StatefulSet> clusterStatefulSet,
      List<Pod> clusterPods) {
  }

}
