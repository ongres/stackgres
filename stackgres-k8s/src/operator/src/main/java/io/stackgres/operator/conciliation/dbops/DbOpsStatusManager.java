/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.stackgres.common.ClusterRolloutUtil;
import io.stackgres.common.DbOpsUtil;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.conciliation.StatusManager;
import io.stackgres.operatorframework.resource.ConditionUpdater;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DbOpsStatusManager
    extends ConditionUpdater<StackGresDbOps, Condition>
    implements StatusManager<StackGresDbOps, Condition> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DbOpsStatusManager.class);

  private final ResourceFinder<Job> jobFinder;

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final LabelFactoryForCluster labelFactory;

  private final ResourceFinder<StatefulSet> statefulSetFinder;

  private final ResourceScanner<Pod> podScanner;

  private final PatroniCtl patroniCtl;

  private static String getDbOpsId(StackGresDbOps dbOps) {
    return dbOps.getMetadata().getNamespace() + "/" + dbOps.getMetadata().getName();
  }

  @Inject
  public DbOpsStatusManager(
      ResourceFinder<Job> jobFinder,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      LabelFactoryForCluster labelFactory,
      ResourceFinder<StatefulSet> statefulSetFinder,
      ResourceScanner<Pod> podScanner,
      PatroniCtl patroniCtl) {
    this.jobFinder = jobFinder;
    this.clusterFinder = clusterFinder;
    this.labelFactory = labelFactory;
    this.statefulSetFinder = statefulSetFinder;
    this.podScanner = podScanner;
    this.patroniCtl = patroniCtl;
  }

  @Override
  public StackGresDbOps refreshCondition(StackGresDbOps source) {
    if (DbOpsUtil.isAlreadySuccessfullyCompleted(source)) {
      return source;
    }
    if (DbOpsUtil.ROLLOUT_OPS.contains(source.getSpec().getOp())) {
      updateRolloutBasedDbOps(source);
    } else {
      updateJobBasedDbOps(source);
    }
    return source;
  }

  private void updateRolloutBasedDbOps(StackGresDbOps source) {
    if (DbOpsUtil.isAlreadyCompleted(source)) {
      return;
    }
    Optional<StackGresCluster> foundCluster =
        clusterFinder.findByNameAndNamespace(source.getSpec().getSgCluster(), source.getMetadata().getNamespace());
    Instant now = Instant.now();
    if (foundCluster.isPresent()
        && !DbOpsUtil.isToRunAfter(source, now)) {
      StackGresCluster cluster = foundCluster.get();
      Optional<StatefulSet> statefulSet =
          statefulSetFinder.findByNameAndNamespace(
              source.getSpec().getSgCluster(), source.getMetadata().getNamespace());
      List<Pod> pods = podScanner
          .getResourcesInNamespaceWithLabels(
              source.getMetadata().getNamespace(), labelFactory.patroniClusterLabels(cluster));
      List<PatroniMember> patroniMembers = patroniCtl.instanceFor(cluster).list();
      boolean primaryIsReadyAndUpdated = pods.stream()
          .filter(ClusterRolloutUtil::isPodReady)
          .filter(pod -> !ClusterRolloutUtil.getRestartReasons(
              cluster, statefulSet, pod, patroniMembers).requiresRestart())
          .anyMatch(pod -> patroniMembers.stream()
              .anyMatch(patroniMember -> patroniMember.getMember().equals(pod.getMetadata().getName())
                  && patroniMember.isPrimary()));
      List<Pod> podsReadyAndUpdated = pods.stream()
          .filter(ClusterRolloutUtil::isPodReady)
          .filter(pod -> !ClusterRolloutUtil.getRestartReasons(
              cluster, statefulSet, pod, patroniMembers).requiresRestart())
          .toList();
      if (source.getStatus() == null) {
        source.setStatus(new StackGresDbOpsStatus());
      }
      if (primaryIsReadyAndUpdated
          && cluster.getSpec().getInstances() == podsReadyAndUpdated.size()) {
        updateCondition(getFalseRunning(), source);
        updateCondition(getCompleted(), source);
      } else {
        updateCondition(getRunning(), source);
        updateCondition(getFalseCompleted(), source);
      }
      if (source.getStatus().getOpStarted() == null) {
        source.getStatus().setOpStarted(now.toString());
      }
      final List<String> initialInstances = Optional.ofNullable(cluster.getStatus())
          .map(StackGresClusterStatus::getDbOps)
          .map(StackGresClusterDbOpsStatus::getRestart)
          .map(StackGresClusterDbOpsRestartStatus::getInitialInstances)
          .orElse(null);
      final String primaryInstance = Optional.ofNullable(cluster.getStatus())
          .map(StackGresClusterStatus::getDbOps)
          .map(StackGresClusterDbOpsStatus::getRestart)
          .map(StackGresClusterDbOpsRestartStatus::getPrimaryInstance)
          .orElse(null);
      final List<String> pendingToRestartInstances = pods
          .stream()
          .filter(pod -> !podsReadyAndUpdated.contains(pod))
          .map(pod -> pod.getMetadata().getName())
          .toList();
      final List<String> restartedInstances = podsReadyAndUpdated
          .stream()
          .map(pod -> pod.getMetadata().getName())
          .toList();
      final Supplier<String> switchoverInitiated = () -> primaryInstance != null
          && patroniMembers.stream().anyMatch(patroniMember -> patroniMember.getMember().equals(primaryInstance)
              && !patroniMember.isPrimary()) ? now.toString() : null;
      final Supplier<String> switchoverFinalized = () -> primaryInstance != null
          && primaryIsReadyAndUpdated
          && patroniMembers.stream().anyMatch(patroniMember -> patroniMember.getMember().equals(primaryInstance)
              && !patroniMember.isPrimary()) ? now.toString() : null;
      if ("restart".equals(source.getSpec().getOp())) {
        if (source.getStatus().getRestart() == null) {
          source.getStatus().setRestart(new StackGresDbOpsRestartStatus());
        }
        source.getStatus().getRestart().setInitialInstances(initialInstances);
        source.getStatus().getRestart().setPrimaryInstance(primaryInstance);
        source.getStatus().getRestart().setPendingToRestartInstances(pendingToRestartInstances);
        source.getStatus().getRestart().setRestartedInstances(restartedInstances);
        if (source.getStatus().getRestart().getSwitchoverInitiated() == null) {
          source.getStatus().getRestart().setSwitchoverInitiated(switchoverInitiated.get());
        }
        if (source.getStatus().getRestart().getSwitchoverFinalized() == null) {
          source.getStatus().getRestart().setSwitchoverFinalized(switchoverFinalized.get());
        }
      } else if ("securityUpgrade".equals(source.getSpec().getOp())) {
        if (source.getStatus().getSecurityUpgrade() == null) {
          source.getStatus().setSecurityUpgrade(new StackGresDbOpsSecurityUpgradeStatus());
        }
        source.getStatus().getSecurityUpgrade().setInitialInstances(initialInstances);
        source.getStatus().getSecurityUpgrade().setPrimaryInstance(primaryInstance);
        source.getStatus().getSecurityUpgrade().setPendingToRestartInstances(pendingToRestartInstances);
        source.getStatus().getSecurityUpgrade().setRestartedInstances(restartedInstances);
        if (source.getStatus().getSecurityUpgrade().getSwitchoverInitiated() == null) {
          source.getStatus().getSecurityUpgrade().setSwitchoverInitiated(switchoverInitiated.get());
        }
        if (source.getStatus().getSecurityUpgrade().getSwitchoverFinalized() == null) {
          source.getStatus().getSecurityUpgrade().setSwitchoverFinalized(switchoverFinalized.get());
        }
      } else if ("minorVersionUpgrade".equals(source.getSpec().getOp())) {
        if (source.getStatus().getMinorVersionUpgrade() == null) {
          source.getStatus().setMinorVersionUpgrade(new StackGresDbOpsMinorVersionUpgradeStatus());
        }
        source.getStatus().getMinorVersionUpgrade().setTargetPostgresVersion(
            source.getSpec().getMinorVersionUpgrade().getPostgresVersion());
        source.getStatus().getMinorVersionUpgrade().setInitialInstances(initialInstances);
        source.getStatus().getMinorVersionUpgrade().setPrimaryInstance(primaryInstance);
        source.getStatus().getMinorVersionUpgrade().setPendingToRestartInstances(pendingToRestartInstances);
        source.getStatus().getMinorVersionUpgrade().setRestartedInstances(restartedInstances);
        if (source.getStatus().getMinorVersionUpgrade().getSwitchoverInitiated() == null) {
          source.getStatus().getMinorVersionUpgrade().setSwitchoverInitiated(switchoverInitiated.get());
        }
        if (source.getStatus().getMinorVersionUpgrade().getSwitchoverFinalized() == null) {
          source.getStatus().getMinorVersionUpgrade().setSwitchoverFinalized(switchoverFinalized.get());
        }
      }
    }
  }

  private void updateJobBasedDbOps(StackGresDbOps source) {
    final boolean isJobFinishedAndStatusNotUpdated;
    final Optional<Job> job = jobFinder.findByNameAndNamespace(
        DbOpsUtil.jobName(source),
        source.getMetadata().getNamespace());
    isJobFinishedAndStatusNotUpdated = job
        .map(Job::getStatus)
        .map(JobStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .filter(condition -> Objects.equals(condition.getType(), "Failed")
            || Objects.equals(condition.getType(), "Completed"))
        .anyMatch(condition -> Objects.equals(condition.getStatus(), "True"));
    if (source.getStatus() == null) {
      source.setStatus(new StackGresDbOpsStatus());
    }
    final int active = job
        .map(Job::getStatus)
        .map(JobStatus::getActive)
        .orElse(0);
    final int failed = job
        .map(Job::getStatus)
        .map(JobStatus::getFailed)
        .orElse(0);
    source.getStatus().setOpRetries(
        Math.max(0, failed - 1) + (failed > 0 ? active : 0));

    if (isJobFinishedAndStatusNotUpdated) {
      if (source.getStatus() == null) {
        source.setStatus(new StackGresDbOpsStatus());
      }
      updateCondition(getFalseRunning(), source);
      updateCondition(getCompleted(), source);
      if (Optional.of(source)
          .map(StackGresDbOps::getStatus)
          .map(StackGresDbOpsStatus::getConditions)
          .stream()
          .flatMap(List::stream)
          .filter(condition -> Objects.equals(condition.getType(),
              DbOpsStatusCondition.Type.FAILED.getType()))
          .noneMatch(condition -> Objects.equals(condition.getStatus(), "True"))) {
        LOGGER.warn(
            "DbOps {} failed since the job completed but status condition is neither completed or failed",
            getDbOpsId(source));
        updateCondition(getFailedDueToUnexpectedFailure(), source);
      }
    }
  }

  protected Condition getRunning() {
    return DbOpsStatusCondition.DBOPS_RUNNING.getCondition();
  }

  protected Condition getFalseCompleted() {
    return DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition();
  }

  protected Condition getFalseRunning() {
    return DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition();
  }

  protected Condition getCompleted() {
    return DbOpsStatusCondition.DBOPS_COMPLETED.getCondition();
  }

  protected Condition getFailedDueToUnexpectedFailure() {
    var failed = DbOpsStatusCondition.DBOPS_FAILED.getCondition();
    failed.setMessage("Unexpected failure");
    return failed;
  }

  @Override
  protected List<Condition> getConditions(StackGresDbOps context) {
    return Optional.ofNullable(context.getStatus())
        .map(StackGresDbOpsStatus::getConditions)
        .orElse(List.of());
  }

  @Override
  protected void setConditions(StackGresDbOps context, List<Condition> conditions) {
    if (context.getStatus() == null) {
      context.setStatus(new StackGresDbOpsStatus());
    }
    context.getStatus().setConditions(conditions);
  }

}
