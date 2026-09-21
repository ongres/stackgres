/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatus;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus;
import io.stackgres.common.JobUtil;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsOperation;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.DbOpsStatusCondition;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestart;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.common.patroni.PatroniCtl;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceScanner;
import io.stackgres.operator.common.ClusterRolloutUtil;
import io.stackgres.operator.common.DbOpsUtil;
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

  private final ResourceFinder<Endpoints> endpointsFinder;

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
      ResourceFinder<Endpoints> endpointsFinder,
      PatroniCtl patroniCtl) {
    this.jobFinder = jobFinder;
    this.clusterFinder = clusterFinder;
    this.labelFactory = labelFactory;
    this.statefulSetFinder = statefulSetFinder;
    this.podScanner = podScanner;
    this.endpointsFinder = endpointsFinder;
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
    Instant now = Instant.now();
    if (DbOpsUtil.isToRunAfter(source, now)) {
      return;
    }
    if (DbOpsUtil.isTimeoutExpired(source, now)) {
      updateCondition(getFalseRunning(), source);
      updateCondition(getCompleted(), source);
      updateCondition(getFailedDueToTimeoutFailure(), source);
      return;
    }
    Optional<StackGresCluster> foundCluster =
        clusterFinder.findByNameAndNamespace(source.getSpec().getSgCluster(), source.getMetadata().getNamespace());
    if (foundCluster.isEmpty()) {
      return;
    }
    StackGresCluster cluster = foundCluster.get();
    Optional<StatefulSet> statefulSet =
        statefulSetFinder.findByNameAndNamespace(
            source.getSpec().getSgCluster(), source.getMetadata().getNamespace());
    if (statefulSet
        .map(StatefulSet::getStatus)
        .map(StatefulSetStatus::getUpdateRevision)
        .isEmpty()) {
      return;
    }
    List<Pod> pods = podScanner
        .getResourcesInNamespaceWithLabels(
            source.getMetadata().getNamespace(), labelFactory.clusterLabels(cluster));
    List<PatroniMember> patroniMembers = patroniCtl.instanceFor(cluster).list();
    final boolean primaryIsReadyAndUpdated = pods.stream()
        .filter(ClusterRolloutUtil::isPodReady)
        .filter(pod -> !ClusterRolloutUtil.getPodRestartReasons(
            cluster, statefulSet, pod).requiresRestart()
            && !ClusterRolloutUtil.getPostgresRestartReasons(
                pod, patroniMembers).requiresRestart())
        .anyMatch(pod -> patroniMembers.stream()
            .anyMatch(patroniMember -> patroniMember.getMember().equals(pod.getMetadata().getName())
                && patroniMember.isPrimary()));
    final boolean primaryIsExternal = patroniMembers.stream()
        .filter(PatroniMember::isPrimary)
        .anyMatch(patroniMember -> pods.stream()
            .map(HasMetadata::getMetadata)
            .map(ObjectMeta::getName)
            .noneMatch(patroniMember.getMember()::equals));
    List<Pod> podsReadyAndUpdated = pods.stream()
        .filter(ClusterRolloutUtil::isPodReady)
        .filter(pod -> !ClusterRolloutUtil.getPodRestartReasons(
            cluster, statefulSet, pod).requiresRestart()
            && !ClusterRolloutUtil.getPostgresRestartReasons(
                pod, patroniMembers).requiresRestart())
        .toList();
    final boolean securityUpgradeWasApplied;
    if (Objects.equals(
        DbOpsOperation.SECURITY_UPGRADE.toString(),
        source.getSpec().getOp())) {
      securityUpgradeWasApplied = podsReadyAndUpdated
          .stream()
          .allMatch(pod -> Optional.of(pod)
              .map(Pod::getMetadata)
              .map(ObjectMeta::getAnnotations)
              .map(Map::entrySet)
              .stream()
              .flatMap(Set::stream)
              .anyMatch(Map.entry(
                  StackGresContext.VERSION_KEY,
                  StackGresProperty.OPERATOR_VERSION.getString())::equals));
    } else {
      securityUpgradeWasApplied = true;
    }
    final boolean minorVersionUpgradeWasApplied;
    if (Objects.equals(
        DbOpsOperation.MINOR_VERSION_UPGRADE.toString(),
        source.getSpec().getOp())) {
      final String targetPatroniImage = StackGresUtil.getPatroniImageName(
          cluster,
          source.getSpec().getMinorVersionUpgrade().getPostgresVersion());
      minorVersionUpgradeWasApplied = podsReadyAndUpdated
          .stream()
          .allMatch(pod -> Optional.of(pod)
              .map(Pod::getSpec)
              .map(PodSpec::getContainers)
              .stream()
              .flatMap(List::stream)
              .map(Container::getImage)
              .anyMatch(targetPatroniImage::equals));
    } else {
      minorVersionUpgradeWasApplied = true;
    }
    if (source.getStatus() == null) {
      source.setStatus(new StackGresDbOpsStatus());
    }
    final DbOpsRestartStatus restartStatus;
    if ("restart".equals(source.getSpec().getOp())) {
      if (source.getStatus().getRestart() == null) {
        source.getStatus().setRestart(new StackGresDbOpsRestartStatus());
      }
      restartStatus = source.getStatus().getRestart();
    } else if ("securityUpgrade".equals(source.getSpec().getOp())) {
      if (source.getStatus().getSecurityUpgrade() == null) {
        source.getStatus().setSecurityUpgrade(new StackGresDbOpsSecurityUpgradeStatus());
      }
      restartStatus = source.getStatus().getSecurityUpgrade();
    } else if ("minorVersionUpgrade".equals(source.getSpec().getOp())) {
      if (source.getStatus().getMinorVersionUpgrade() == null) {
        source.getStatus().setMinorVersionUpgrade(new StackGresDbOpsMinorVersionUpgradeStatus());
      }
      restartStatus = source.getStatus().getMinorVersionUpgrade();
      source.getStatus().getMinorVersionUpgrade().setTargetPostgresVersion(
          source.getSpec().getMinorVersionUpgrade().getPostgresVersion());
    } else {
      throw new UnsupportedOperationException(
          "Operation " + source.getSpec().getOp() + " is not a rollout operation");
    }

    // The status of patroni and of the StatefulSet is not updated immediately, so the rollout may
    // look completed while some Pod has still to be restarted. To avoid completing the operation
    // too early the completion is postponed until the status of the SGDbOps has not been updated
    // for at least the configured status update delay.
    final Duration statusUpdateDelay = DbOpsUtil.getStatusUpdateDelay(source);
    final String previousLastUpdate = restartStatus.getLastUpdate();
    restartStatus.setLastUpdate(null);
    final String previousStatus = source.getStatus().toString();
    final boolean isStatusUpdateDelayElapsed =
        isStatusUpdateDelayElapsed(previousLastUpdate, statusUpdateDelay, now);
    final boolean wasRolloutCompleted = Optional.of(source)
          .map(StackGresDbOps::getStatus)
          .map(StackGresDbOpsStatus::getConditions)
          .stream()
          .flatMap(List::stream)
          .anyMatch(DbOpsStatusCondition.DBOPS_ROLLOUT_COMPLETED::isCondition);
    final boolean isRolloutCompleted = (primaryIsReadyAndUpdated || primaryIsExternal)
        && securityUpgradeWasApplied
        && minorVersionUpgradeWasApplied
        && pods.size() == podsReadyAndUpdated.size();
    if (isRolloutCompleted && (isStatusUpdateDelayElapsed || wasRolloutCompleted)) {
      updateCondition(getRolloutCompleted(), source);
      if (Optional.ofNullable(cluster.getMetadata().getAnnotations())
          .map(Map::entrySet)
          .stream()
          .flatMap(Set::stream)
          .noneMatch(Map.entry(
            StackGresContext.ROLLOUT_DBOPS_KEY,
            source.getMetadata().getName())::equals)) {
        updateCondition(getFalseRunning(), source);
        updateCondition(getCompleted(), source);
      }
    } else {
      if (isRolloutCompleted && LOGGER.isDebugEnabled()) {
        LOGGER.debug("DbOps {} rollout completed but waiting the status update delay of {} to pass"
            + " since the last status update", getDbOpsId(source), statusUpdateDelay);
      }
      updateCondition(getRunning(), source);
      updateCondition(getFalseRestartCompleted(), source);
      updateCondition(getFalseCompleted(), source);
    }
    if (source.getStatus().getOpStarted() == null) {
      source.getStatus().setOpStarted(now.toString());
      source.getStatus().setOpRetries(0);
    }
    final List<String> initialInstances = Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(clusterDbOpsStatus -> Optional.of(clusterDbOpsStatus))
        .flatMap(clusterDbOpsStatus -> clusterDbOpsStatus
            .<ClusterDbOpsRestartStatus>map(StackGresClusterDbOpsStatus::getRestart)
            .or(() -> clusterDbOpsStatus
                .map(StackGresClusterDbOpsStatus::getSecurityUpgrade))
            .or(() -> clusterDbOpsStatus
                .map(StackGresClusterDbOpsStatus::getMinorVersionUpgrade)))
        .map(ClusterDbOpsRestartStatus::getInitialInstances)
        .or(() -> Optional.ofNullable(source.getStatus())
            .map(dbOpsStatus -> Optional.of(dbOpsStatus))
            .flatMap(dbOpsStatus -> dbOpsStatus
                .<DbOpsRestartStatus>map(StackGresDbOpsStatus::getRestart)
                .or(() -> dbOpsStatus
                    .map(StackGresDbOpsStatus::getSecurityUpgrade))
                .or(() -> dbOpsStatus
                    .map(StackGresDbOpsStatus::getMinorVersionUpgrade)))
            .map(DbOpsRestartStatus::getInitialInstances))
        .orElse(null);
    final String primaryInstance = Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getDbOps)
        .map(clusterDbOpsStatus -> Optional.of(clusterDbOpsStatus))
        .flatMap(clusterDbOpsStatus -> clusterDbOpsStatus
            .<ClusterDbOpsRestartStatus>map(StackGresClusterDbOpsStatus::getRestart)
            .or(() -> clusterDbOpsStatus
                .map(StackGresClusterDbOpsStatus::getSecurityUpgrade))
            .or(() -> clusterDbOpsStatus
                .map(StackGresClusterDbOpsStatus::getMinorVersionUpgrade)))
        .map(ClusterDbOpsRestartStatus::getPrimaryInstance)
        .or(() -> Optional.ofNullable(source.getStatus())
            .map(dbOpsStatus -> Optional.of(dbOpsStatus))
            .flatMap(dbOpsStatus -> dbOpsStatus
                .<DbOpsRestartStatus>map(StackGresDbOpsStatus::getRestart)
                .or(() -> dbOpsStatus
                    .map(StackGresDbOpsStatus::getSecurityUpgrade))
                .or(() -> dbOpsStatus
                    .map(StackGresDbOpsStatus::getMinorVersionUpgrade)))
            .map(DbOpsRestartStatus::getPrimaryInstance))
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
    final Supplier<String> switchoverInitiated = () -> Optional.ofNullable(primaryInstance)
        .flatMap(primary -> endpointsFinder
            .findByNameAndNamespace(PatroniUtil.failoverName(cluster), source.getMetadata().getNamespace())
            .map(HasMetadata::getMetadata)
            .map(ObjectMeta::getAnnotations)
            .map(annotations -> annotations.get("leader"))
            .filter(primary::equals)
            .map(ignored -> now.toString()))
        .orElse(null);
    final Supplier<String> switchoverFinalized = () -> Optional.ofNullable(primaryInstance)
        .flatMap(primary -> endpointsFinder
            .findByNameAndNamespace(PatroniUtil.failoverName(cluster), source.getMetadata().getNamespace())
            .map(HasMetadata::getMetadata)
            .map(ObjectMeta::getAnnotations)
            .map(annotations -> Optional.ofNullable(annotations.get("leader")).orElse("none"))
            .filter(Predicate.not(primary::equals))
            .map(ignored -> now.toString()))
        .orElse(null);

    restartStatus.setInitialInstances(initialInstances);
    restartStatus.setPrimaryInstance(primaryInstance);
    restartStatus.setPendingToRestartInstances(pendingToRestartInstances);
    restartStatus.setRestartedInstances(restartedInstances);
    final boolean isReducedImpact = Optional.of(source.getSpec())
        .map(StackGresDbOpsSpec::getRestart)
        .map(StackGresDbOpsRestart::getMethod)
        .or(() -> Optional.of(source.getSpec())
            .map(StackGresDbOpsSpec::getSecurityUpgrade)
            .map(StackGresDbOpsSecurityUpgrade::getMethod))
        .or(() -> Optional.of(source.getSpec())
            .map(StackGresDbOpsSpec::getMinorVersionUpgrade)
            .map(StackGresDbOpsMinorVersionUpgrade::getMethod))
        .map(DbOpsMethodType::fromString)
        .map(DbOpsMethodType.REDUCED_IMPACT::equals)
        .orElse(false);
    if ((cluster.getSpec().getInstances() > 1 || isReducedImpact)
        && restartStatus.getSwitchoverInitiated() == null) {
      restartStatus.setSwitchoverInitiated(switchoverInitiated.get());
    }
    if ((cluster.getSpec().getInstances() > 1 || isReducedImpact)
        && restartStatus.getSwitchoverInitiated() != null
        && restartStatus.getSwitchoverFinalized() == null) {
      restartStatus.setSwitchoverFinalized(switchoverFinalized.get());
    }
    final boolean isStatusUpdated = !Objects.equals(previousStatus, source.getStatus().toString());
    restartStatus.setLastUpdate(isStatusUpdated || previousLastUpdate == null
        ? now.toString() : previousLastUpdate);
  }

  private boolean isStatusUpdateDelayElapsed(
      String lastUpdate, Duration statusUpdateDelay, Instant now) {
    if (lastUpdate == null) {
      return false;
    }
    try {
      return Instant.parse(lastUpdate).plus(statusUpdateDelay).isBefore(now);
    } catch (DateTimeParseException ex) {
      LOGGER.warn("Last update {} is not valid", lastUpdate, ex);
      return false;
    }
  }

  private void updateJobBasedDbOps(StackGresDbOps source) {
    final Optional<Job> job = jobFinder.findByNameAndNamespace(
        DbOpsUtil.jobName(source),
        source.getMetadata().getNamespace());
    final Optional<Boolean> jobCompleted = JobUtil.isJobCompleteOrFailed(job);
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

    updateMajorVersionUpgradeWaitingRollbackCondition(source);

    if (jobCompleted.isPresent()) {
      updateCondition(getFalseRunning(), source);
      final boolean dbOpsAlreadyFailed = Optional.of(source)
          .map(StackGresDbOps::getStatus)
          .map(StackGresDbOpsStatus::getConditions)
          .stream()
          .flatMap(List::stream)
          .filter(condition -> Objects.equals(condition.getType(),
              DbOpsStatusCondition.Type.FAILED.getType()))
          .anyMatch(condition -> Objects.equals(condition.getStatus(), "True"));
      if (dbOpsAlreadyFailed) {
        // The operation already recorded a failure (e.g. the run script set OperationFailed). Never
        // stamp OperationCompleted over a failed operation.
        updateCondition(getFalseCompleted(), source);
      } else if (jobCompleted.orElse(false)) {
        updateCondition(getCompleted(), source);
      } else {
        LOGGER.warn(
            "DbOps {} failed since the job failed but status condition is neither completed nor failed",
            getDbOpsId(source));
        updateCondition(getFailedDueToUnexpectedFailure(), source);
        updateCondition(getFalseCompleted(), source);
      }
    }
  }

  private void updateMajorVersionUpgradeWaitingRollbackCondition(StackGresDbOps source) {
    if (!source.getSpec().isOpMajorVersionUpgrade()) {
      return;
    }
    final Optional<StackGresDbOpsMajorVersionUpgradeStatus> majorVersionUpgrade =
        Optional.ofNullable(source.getStatus())
        .map(StackGresDbOpsStatus::getMajorVersionUpgrade);
    final String phase = majorVersionUpgrade
        .map(StackGresDbOpsMajorVersionUpgradeStatus::getPhase)
        .orElse(null);
    final Boolean rollback = majorVersionUpgrade
        .map(StackGresDbOpsMajorVersionUpgradeStatus::getRollback)
        .orElse(null);
    if (rollback == null
        && DbOpsUtil.MAJOR_VERSION_UPGRADE_WAIT_POST_FAILED_UPGRADE_DECISION_PHASE.equals(phase)) {
      updateCondition(
          DbOpsStatusCondition.DBOPS_WAITING_ROLLBACK_AFTER_FAILED.getCondition(), source);
    } else if (rollback == null
        && DbOpsUtil.MAJOR_VERSION_UPGRADE_WAIT_POST_UPGRADE_DECISION_PHASE.equals(phase)) {
      updateCondition(
          DbOpsStatusCondition.DBOPS_WAITING_ROLLBACK_AFTER_SUCCEEDED.getCondition(), source);
    } else {
      updateCondition(DbOpsStatusCondition.DBOPS_FALSE_WAITING_ROLLBACK.getCondition(), source);
    }
  }

  protected Condition getRunning() {
    return DbOpsStatusCondition.DBOPS_RUNNING.getCondition();
  }

  protected Condition getFalseRunning() {
    return DbOpsStatusCondition.DBOPS_FALSE_RUNNING.getCondition();
  }

  protected Condition getRolloutCompleted() {
    return DbOpsStatusCondition.DBOPS_ROLLOUT_COMPLETED.getCondition();
  }

  protected Condition getFalseRestartCompleted() {
    return DbOpsStatusCondition.DBOPS_FALSE_ROLLOUT_COMPLETED.getCondition();
  }

  protected Condition getCompleted() {
    return DbOpsStatusCondition.DBOPS_COMPLETED.getCondition();
  }

  protected Condition getFalseCompleted() {
    return DbOpsStatusCondition.DBOPS_FALSE_COMPLETED.getCondition();
  }

  protected Condition getFailedDueToUnexpectedFailure() {
    var failed = DbOpsStatusCondition.DBOPS_FAILED.getCondition();
    failed.setMessage("Unexpected failure");
    return failed;
  }

  protected Condition getFailedDueToTimeoutFailure() {
    var failed = DbOpsStatusCondition.DBOPS_TIMED_OUT.getCondition();
    failed.setMessage("Timeout");
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
