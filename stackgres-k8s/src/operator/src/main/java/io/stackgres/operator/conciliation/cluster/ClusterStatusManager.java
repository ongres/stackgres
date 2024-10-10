/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.stackgres.common.ManagedSqlUtil;
import io.stackgres.common.StackGresUtil;
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
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.common.ClusterRolloutUtil;
import io.stackgres.operator.common.ClusterRolloutUtil.ClusterRestartReason;
import io.stackgres.operator.common.ClusterRolloutUtil.ClusterRestartReasons;
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

  private final ResourceFinder<StatefulSet> statefulSetFinder;

  private final ResourceScanner<Pod> podScanner;

  private final PatroniCtl patroniCtl;

  private static String getClusterId(StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace() + "/" + cluster.getMetadata().getName();
  }

  @Inject
  public ClusterStatusManager(
      LabelFactoryForCluster labelFactory,
      CustomResourceFinder<StackGresScript> scriptFinder,
      ResourceFinder<StatefulSet> statefulSetFinder,
      ResourceScanner<Pod> podScanner,
      PatroniCtl patroniCtl) {
    this.labelFactory = labelFactory;
    this.scriptFinder = scriptFinder;
    this.statefulSetFinder = statefulSetFinder;
    this.podScanner = podScanner;
    this.patroniCtl = patroniCtl;
  }

  @Override
  public StackGresCluster refreshCondition(StackGresCluster source) {
    if (source.getStatus() == null) {
      source.setStatus(new StackGresClusterStatus());
    }
    source.getStatus().setBinding(new StackGresClusterServiceBindingStatus());
    source.getStatus().getBinding().setName(ServiceBindingSecret.name(source));
    StatusContext context = getStatusContext(source);
    ClusterRestartReasons restartReasons = getRestartReasons(source, context);
    if (restartReasons.requiresRestart()) {
      updateCondition(getPodRequiresRestart(), source);
    } else {
      updateCondition(getFalsePendingRestart(), source);
    }
    if (restartReasons.requiresUpgrade()) {
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
    if (!StackGresUtil.isRegistryEnabled(source)) {
      if (source.getStatus() != null
          && source.getStatus().getArch() != null
          && source.getStatus().getOs() != null
          && source.getStatus().getPodStatuses() != null
          && source.getStatus().getExtensions() != null) {
        source.getStatus().getPodStatuses()
            .stream()
            .filter(StackGresClusterPodStatus::getPrimary)
            .flatMap(podStatus -> source.getStatus().getExtensions().stream()
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
    }
    refreshComponentsUpdated(source);
    source.getStatus().setInstances(context.pods().size());
    source.getStatus().setLabelSelector(labelFactory.clusterLabels(source)
        .entrySet()
        .stream()
        .map(e -> e.getKey() + "=" + e.getValue())
        .collect(Collectors.joining(",")));
    return source;
  }

  /**
   * Advertise the availability of newer PostgreSQL minor/major versions and extension upgrades in
   * the {@code ComponentsUpdated} condition.
   *
   * <p>The condition status is {@code True} only when the latest minor version of the PostgreSQL
   * major in use is used, independently of the major; when a newer minor is available it is
   * {@code False}. A newer major appends {@code -NotLatestMajor} and extension upgrades append
   * {@code -AvailableExtensionsUpgrade} to the reason without changing the status. The reason is a
   * fixed {@link ClusterStatusCondition} entry per combination ({@code UpToDate} when the latest
   * minor is used with no newer major nor extension upgrades); the actual versions and the number of
   * extensions to upgrade are advertised in the message (limited to
   * {@value io.stackgres.operatorframework.resource.ConditionUpdater#MAX_MESSAGE_LENGTH} chars).
   */
  private void refreshComponentsUpdated(StackGresCluster source) {
    final String usedVersion = Optional.of(source)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getPostgresVersion)
        .orElse(null);
    if (usedVersion == null) {
      return;
    }
    final String latestMinor = source.getStatus().getLatestPostgresMinor();
    final String latestMajor = source.getStatus().getLatestPostgresMajor();
    final List<ExtensionUpgrade> extensionUpgrades = getExtensionUpgrades(source);

    final boolean isLatestMinor = latestMinor == null;
    final boolean newMajorAvailable = latestMajor != null;

    final StringBuilder message = new StringBuilder();
    if (isLatestMinor) {
      message.append("PostgreSQL ").append(usedVersion)
          .append(" is the latest minor version for major ").append(majorOf(usedVersion))
          .append('.');
    } else {
      message.append("PostgreSQL ").append(usedVersion)
          .append(" is not the latest minor version for major ").append(majorOf(usedVersion))
          .append(" (latest is ").append(latestMinor).append(").");
    }
    if (newMajorAvailable) {
      message.append(" A newer major version ").append(latestMajor).append(" is available.");
    }
    if (!extensionUpgrades.isEmpty()) {
      appendExtensionUpgradesMessage(message, extensionUpgrades);
    }

    Condition condition = getComponentsUpdatedCondition(
        isLatestMinor, newMajorAvailable, !extensionUpgrades.isEmpty()).getCondition();
    condition.setMessage(message.toString());
    updateCondition(condition, source);
  }

  private static String majorOf(String version) {
    int indexOfSeparator = version.indexOf('.');
    return indexOfSeparator > 0 ? version.substring(0, indexOfSeparator) : version;
  }

  /**
   * Select the fixed {@link ClusterStatusCondition} reason for every combination of the latest minor
   * in use, a newer major available and extension upgrades available.
   */
  private static ClusterStatusCondition getComponentsUpdatedCondition(
      boolean isLatestMinor, boolean newMajorAvailable, boolean extensionUpgrades) {
    if (isLatestMinor) {
      if (newMajorAvailable && extensionUpgrades) {
        return ClusterStatusCondition
            .COMPONENTS_LATEST_MINOR_NOT_LATEST_MAJOR_AVAILABLE_EXTENSIONS_UPGRADE;
      }
      if (newMajorAvailable) {
        return ClusterStatusCondition.COMPONENTS_LATEST_MINOR_NOT_LATEST_MAJOR;
      }
      if (extensionUpgrades) {
        return ClusterStatusCondition.COMPONENTS_LATEST_MINOR_AVAILABLE_EXTENSIONS_UPGRADE;
      }
      return ClusterStatusCondition.COMPONENTS_UP_TO_DATE;
    }
    if (newMajorAvailable && extensionUpgrades) {
      return ClusterStatusCondition
          .COMPONENTS_NOT_LATEST_MINOR_NOT_LATEST_MAJOR_AVAILABLE_EXTENSIONS_UPGRADE;
    }
    if (newMajorAvailable) {
      return ClusterStatusCondition.COMPONENTS_NOT_LATEST_MINOR_NOT_LATEST_MAJOR;
    }
    if (extensionUpgrades) {
      return ClusterStatusCondition.COMPONENTS_NOT_LATEST_MINOR_AVAILABLE_EXTENSIONS_UPGRADE;
    }
    return ClusterStatusCondition.COMPONENTS_NOT_LATEST_MINOR;
  }

  /**
   * Collect the extensions that have a newer version available, as advertised by
   * {@code .status.extensions[].latest} (computed by {@code ClusterExtensionsContextAppender} by
   * comparing against the version requested in {@code .spec.postgres.extensions[]}).
   */
  private List<ExtensionUpgrade> getExtensionUpgrades(StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getExtensions)
        .orElse(List.of())
        .stream()
        .filter(installedExtension -> installedExtension.getLatest() != null)
        .map(installedExtension -> new ExtensionUpgrade(
            installedExtension.getName(),
            installedExtension.getVersion(),
            installedExtension.getLatest()))
        .sorted(Comparator.comparing(ExtensionUpgrade::name))
        .toList();
  }

  /**
   * Append the number and list of extensions to upgrade to the message, stopping before the message
   * would exceed {@value io.stackgres.operatorframework.resource.ConditionUpdater#MAX_MESSAGE_LENGTH}
   * characters and indicating how many were omitted.
   */
  private void appendExtensionUpgradesMessage(
      StringBuilder message, List<ExtensionUpgrade> upgrades) {
    message.append(" The following").append(upgrades.size())
        .append(upgrades.size() == 1 ? " extension can be upgraded: " : " extensions can be"
            + " upgraded: ");
    int shown = 0;
    for (ExtensionUpgrade upgrade : upgrades) {
      final String item = (shown == 0 ? "" : ", ")
          + upgrade.name() + " (" + upgrade.from() + "->" + upgrade.to() + ")";
      final String remainingSuffix = " and " + (upgrades.size() - shown) + " more.";
      if (message.length() + item.length() + remainingSuffix.length() > MAX_MESSAGE_LENGTH) {
        message.append(remainingSuffix);
        return;
      }
      message.append(item);
      shown++;
    }
    message.append('.');
  }

  /**
   * Check pending restart status condition.
   */
  public ClusterRestartReasons getRestartReasons(StackGresCluster cluster, StatusContext context) {
    ClusterRestartReasons reasons = ClusterRolloutUtil.getClusterRestartReasons(
        context.cluster(), context.statefulSet(), context.pods(), context.patroniMembers());
    for (ClusterRestartReason reason : reasons.getReasons()) {
      switch (reason) {
        case PATRONI:
          LOGGER.debug("Cluster {} requires restart due to patroni's indication",
              getClusterId(cluster));
          break;
        case POD_STATUS:
          LOGGER.debug("Cluster {} requires restart due to controller indication",
              getClusterId(cluster));
          break;
        case STATEFULSET:
          LOGGER.debug("Cluster {} requires restart due to pod template changes",
              getClusterId(cluster));
          break;
        case UPGRADE:
          LOGGER.debug("Cluster {} requires upgrade due to cluster using old version",
              getClusterId(cluster));
          break;
        default:
          break;
      }
    }
    return reasons;
  }

  private StatusContext getStatusContext(StackGresCluster cluster) {
    final Optional<StatefulSet> statefulSet = statefulSetFinder
        .findByNameAndNamespace(cluster.getMetadata().getName(), cluster.getMetadata().getNamespace());
    final List<Pod> pods = podScanner
        .getResourcesInNamespaceWithLabels(cluster.getMetadata().getNamespace(), labelFactory.clusterLabels(cluster));
    final List<PatroniMember> patroniMembers = patroniCtl.instanceFor(cluster).list();
    StatusContext context = new StatusContext(cluster, statefulSet, pods, patroniMembers);
    return context;
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
      StackGresCluster cluster,
      Optional<StatefulSet> statefulSet,
      List<Pod> pods,
      List<PatroniMember> patroniMembers) {
  }

  record ExtensionUpgrade(String name, String from, String to) {
  }

}
