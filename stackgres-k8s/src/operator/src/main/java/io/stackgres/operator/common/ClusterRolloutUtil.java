/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatus;
import io.stackgres.common.CustomPersistentVolumeUtil;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterUpdateStrategy;
import io.stackgres.common.crd.sgcluster.StackGresClusterUpdateStrategyMethod;
import io.stackgres.common.crd.sgcluster.StackGresClusterUpdateStrategyScheduleBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterUpdateStrategyType;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.patroni.PatroniMember;
import org.jooq.lambda.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterRolloutUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRolloutUtil.class);

  private static final String CONTROLLER_REVISION_HASH_LABEL = "controller-revision-hash";
  private static final CronParser CRON_PARSER =
      new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

  public static final String DBOPS_NOT_FOUND_NAME = "__DBOPS_NOT_FOUND__";

  public static boolean isRolloutAllowed(StackGresCluster cluster) {
    final Map<String, String> annotations = Optional
        .ofNullable(cluster.getMetadata().getAnnotations())
        .orElse(Map.of());
    final StackGresClusterUpdateStrategyType updateStrategyType = Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getUpdateStrategy)
        .map(StackGresClusterUpdateStrategy::getType)
        .map(StackGresClusterUpdateStrategyType::fromString)
        .orElse(StackGresClusterUpdateStrategyType.ONLY_DB_OPS);
    final boolean hasRolloutAnnotation = annotations.containsKey(StackGresContext.ROLLOUT_KEY);
    if (Objects.equals(
        annotations.get(StackGresContext.ROLLOUT_KEY),
        StackGresContext.ROLLOUT_NEVER_VALUE)
        || updateStrategyType == StackGresClusterUpdateStrategyType.NEVER) {
      return false;
    }
    if (Objects.equals(
        annotations.get(StackGresContext.ROLLOUT_KEY),
        StackGresContext.ROLLOUT_ALWAYS_VALUE)
        || (!hasRolloutAnnotation && updateStrategyType == StackGresClusterUpdateStrategyType.ALWAYS)) {
      return true;
    }
    if (annotations.containsKey(StackGresContext.ROLLOUT_DBOPS_KEY)) {
      return true;
    }
    if ((Objects.equals(
        annotations.get(StackGresContext.ROLLOUT_KEY),
        StackGresContext.ROLLOUT_SCHEDULE_VALUE)
        && annotations.containsKey(StackGresContext.ROLLOUT_SCHEDULE_KEY))
        || (!hasRolloutAnnotation && updateStrategyType == StackGresClusterUpdateStrategyType.SCHEDULE
        && Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getUpdateStrategy)
        .map(StackGresClusterUpdateStrategy::getSchedule)
        .isPresent())) {
      ZonedDateTime now = ZonedDateTime.now();
      return Optional.ofNullable(annotations.get(StackGresContext.ROLLOUT_SCHEDULE_KEY))
          .map(schedule -> schedule.split("\\|"))
          .map(schedule -> Arrays.stream(schedule)
              .map(s -> s.split("@"))
              .filter(s -> s.length >= 2)
              .map(s -> new StackGresClusterUpdateStrategyScheduleBuilder()
                  .withCron(s[0])
                  .withDuration(s[1])
                  .build())
              .toList())
          .or(() -> Optional.of(cluster)
              .map(StackGresCluster::getSpec)
              .map(StackGresClusterSpec::getPods)
              .map(StackGresClusterPods::getUpdateStrategy)
              .map(StackGresClusterUpdateStrategy::getSchedule))
          .stream()
          .flatMap(List::stream)
          .map(s -> Tuple.tuple(parseCron(s.getCron()), parseDuration(s.getDuration())))
          .filter(s -> s.v1.isPresent() && s.v2.isPresent())
          .map(s -> s.map1(Optional::get).map2(Optional::get))
          .anyMatch(s -> ExecutionTime.forCron(s.v1)
              .lastExecution(now)
              .map(lastExecution -> lastExecution
                  .plus(s.v2)
                  .isAfter(now))
              .orElse(false));
    }
    return false;
  }

  private static Optional<Cron> parseCron(String cron) {
    try {
      return Optional.of(CRON_PARSER.parse(cron).validate());
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Cron expression {} is not valid", cron, ex);
      return Optional.empty();
    }
  }

  private static Optional<Duration> parseDuration(String duration) {
    try {
      return Optional.of(Duration.parse(duration));
    } catch (DateTimeParseException ex) {
      LOGGER.warn("Duration {} is not valid", duration, ex);
      return Optional.empty();
    }
  }

  public static boolean isRolloutReducedImpact(StackGresCluster cluster) {
    Map<String, String> annotations = Optional
        .ofNullable(cluster.getMetadata().getAnnotations())
        .orElse(Map.of());
    if (annotations.containsKey(StackGresContext.ROLLOUT_DBOPS_METHOD_KEY)) {
      if (Objects.equals(
          annotations.get(StackGresContext.ROLLOUT_DBOPS_METHOD_KEY),
          DbOpsMethodType.REDUCED_IMPACT.annotationValue())) {
        return true;
      } else {
        return false;
      }
    }
    if (annotations.containsKey(StackGresContext.ROLLOUT_METHOD_KEY)) {
      if (Objects.equals(
          annotations.get(StackGresContext.ROLLOUT_METHOD_KEY),
          DbOpsMethodType.REDUCED_IMPACT.annotationValue())) {
        return true;
      } else {
        return false;
      }
    }
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPods)
        .map(StackGresClusterPods::getUpdateStrategy)
        .map(StackGresClusterUpdateStrategy::getMethod)
        .map(StackGresClusterUpdateStrategyMethod::fromString)
        .map(StackGresClusterUpdateStrategyMethod.REDUCED_IPACT::equals)
        .orElse(false);
  }

  public static class ClusterRestartReasons {
    final Set<ClusterRestartReason> reasons = EnumSet.noneOf(ClusterRestartReason.class);

    public static ClusterRestartReasons of(ClusterRestartReason...reasons) {
      ClusterRestartReasons restartReasons = new ClusterRestartReasons();
      for (ClusterRestartReason reason : reasons) {
        restartReasons.addReason(reason);
      }
      return restartReasons;
    }

    void addReason(ClusterRestartReason reason) {
      reasons.add(reason);
    }

    public Set<ClusterRestartReason> getReasons() {
      return Set.of(reasons.toArray(ClusterRestartReason[]::new));
    }

    public boolean requiresRestart() {
      return !reasons.isEmpty();
    }

    public boolean requiresUpgrade() {
      return reasons.contains(ClusterRestartReason.UPGRADE);
    }
  }

  public static class PodRestartReasons {
    final Set<PodRestartReason> reasons = EnumSet.noneOf(PodRestartReason.class);

    public static PodRestartReasons of(PodRestartReason...reasons) {
      PodRestartReasons restartReasons = new PodRestartReasons();
      for (PodRestartReason reason : reasons) {
        restartReasons.addReason(reason);
      }
      return restartReasons;
    }

    void addReason(PodRestartReason reason) {
      reasons.add(reason);
    }

    public Set<PodRestartReason> getReasons() {
      return Set.of(reasons.toArray(PodRestartReason[]::new));
    }

    public boolean requiresRestart() {
      return !reasons.isEmpty();
    }

  }

  public static class PostgresRestartReasons {
    final Set<PostgresRestartReason> reasons = EnumSet.noneOf(PostgresRestartReason.class);

    public static PostgresRestartReasons of(PostgresRestartReason...reasons) {
      PostgresRestartReasons restartReasons = new PostgresRestartReasons();
      for (PostgresRestartReason reason : reasons) {
        restartReasons.addReason(reason);
      }
      return restartReasons;
    }

    void addReason(PostgresRestartReason reason) {
      reasons.add(reason);
    }

    public Set<PostgresRestartReason> getReasons() {
      return Set.of(reasons.toArray(PostgresRestartReason[]::new));
    }

    public boolean requiresRestart() {
      return !reasons.isEmpty();
    }

  }

  public enum ClusterRestartReason {
    STATEFULSET,
    PATRONI,
    POD_STATUS,
    WAL_PATH,
    UPGRADE;
  }

  public enum PodRestartReason {
    STATEFULSET,
    POD_STATUS,
    WAL_PATH;
  }

  public enum PostgresRestartReason {
    PATRONI;
  }

  public static ClusterRestartReasons getClusterRestartReasons(
      StackGresCluster cluster,
      Optional<StatefulSet> statefulSet,
      List<Pod> pods,
      List<PatroniMember> patroniMembers) {
    final ClusterRestartReasons reasons = new ClusterRestartReasons();

    if (isStatefulSetPendingRestart(statefulSet, pods)) {
      reasons.addReason(ClusterRestartReason.STATEFULSET);
    }

    if (isPatroniPendingRestart(pods, patroniMembers)) {
      reasons.addReason(ClusterRestartReason.PATRONI);
    }

    if (isAnyPodPendingRestart(cluster, pods)) {
      reasons.addReason(ClusterRestartReason.POD_STATUS);
    }

    if (isAnyPodWalPathPendingRestart(cluster, pods)) {
      reasons.addReason(ClusterRestartReason.WAL_PATH);
    }

    if (isPendingUpgrade(cluster)) {
      reasons.addReason(ClusterRestartReason.UPGRADE);
    }

    return reasons;
  }

  public static PodRestartReasons getPodsRestartReasons(
      StackGresCluster cluster,
      Optional<StatefulSet> statefulSet,
      List<Pod> pods) {
    final PodRestartReasons reasons = new PodRestartReasons();

    if (isStatefulSetPendingRestart(statefulSet, pods)) {
      reasons.addReason(PodRestartReason.STATEFULSET);
    }

    if (isAnyPodPendingRestart(cluster, pods)) {
      reasons.addReason(PodRestartReason.POD_STATUS);
    }

    if (isAnyPodWalPathPendingRestart(cluster, pods)) {
      reasons.addReason(PodRestartReason.WAL_PATH);
    }

    return reasons;
  }

  public static PodRestartReasons getPodRestartReasons(
      StackGresCluster cluster,
      Optional<StatefulSet> clusterStatefulSet,
      Pod pod) {
    final PodRestartReasons reasons = new PodRestartReasons();

    if (isStatefulSetPodPendingRestart(clusterStatefulSet, pod)) {
      reasons.addReason(PodRestartReason.STATEFULSET);
    }

    if (isPodPendingRestart(cluster, pod)) {
      reasons.addReason(PodRestartReason.POD_STATUS);
    }

    if (isPodWalPathPendingRestart(cluster, pod)) {
      reasons.addReason(PodRestartReason.WAL_PATH);
    }

    return reasons;
  }

  public static PostgresRestartReasons getPostgresRestartReasons(
      List<Pod> pods,
      List<PatroniMember> patroniMembers) {
    final PostgresRestartReasons reasons = new PostgresRestartReasons();

    if (isPatroniPendingRestart(pods, patroniMembers)) {
      reasons.addReason(PostgresRestartReason.PATRONI);
    }

    return reasons;
  }

  public static PostgresRestartReasons getPostgresRestartReasons(
      Pod pod,
      List<PatroniMember> patroniMembers) {
    final PostgresRestartReasons reasons = new PostgresRestartReasons();

    if (isPatroniPendingRestart(pod, patroniMembers)) {
      reasons.addReason(PostgresRestartReason.PATRONI);
    }

    return reasons;
  }

  private static boolean isStatefulSetPendingRestart(
      Optional<StatefulSet> statefulSet, List<Pod> pods) {
    return statefulSet
        .map(StatefulSet::getStatus)
        .map(StatefulSetStatus::getUpdateRevision)
        .map(statefulSetUpdateRevision -> pods.stream()
            .anyMatch(pod -> isStatefulSetPodPendingRestart(statefulSetUpdateRevision, pod)))
        .orElse(false);
  }

  public static boolean isStatefulSetPodPendingRestart(
      Optional<StatefulSet> statefulSet, Pod pod) {
    return statefulSet
        .map(StatefulSet::getStatus)
        .map(StatefulSetStatus::getUpdateRevision)
        .map(statefulSetUpdateRevision -> isStatefulSetPodPendingRestart(statefulSetUpdateRevision, pod))
        .orElse(false);
  }

  private static boolean isStatefulSetPodPendingRestart(
      String statefulSetUpdateRevision, Pod pod) {
    return Optional.ofNullable(pod.getMetadata().getLabels())
        .map(labels -> labels.get(CONTROLLER_REVISION_HASH_LABEL))
        .map(statefulSetUpdateRevision::equals)
        .map(revisionNotChanged -> !revisionNotChanged)
        .orElse(true);
  }

  private static boolean isPatroniPendingRestart(List<Pod> pods, List<PatroniMember> patroniMembers) {
    return pods.stream()
        .anyMatch(pod -> isPatroniPendingRestart(pod, patroniMembers));
  }

  private static boolean isPatroniPendingRestart(Pod pod, List<PatroniMember> patroniMembers) {
    return patroniMembers.stream()
        .anyMatch(patroniMember -> patroniMember.getMember().equals(pod.getMetadata().getName())
            && patroniMember.getPendingRestart() != null);
  }

  private static boolean isAnyPodPendingRestart(
      StackGresCluster cluster, List<Pod> pods) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getPodStatuses)
        .stream()
        .flatMap(List::stream)
        .anyMatch(clusterPodStatus -> pods.stream()
            .anyMatch(pod -> isPodPendingRestart(clusterPodStatus, pod)));
  }

  private static boolean isPodPendingRestart(StackGresCluster cluster, Pod pod) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getPodStatuses)
        .stream()
        .flatMap(List::stream)
        .anyMatch(clusterPodStatus -> isPodPendingRestart(clusterPodStatus, pod));
  }

  private static boolean isPodPendingRestart(StackGresClusterPodStatus clusterPodStatus, Pod pod) {
    return pod.getMetadata().getName().equals(clusterPodStatus.getName())
        && Objects.equals(clusterPodStatus.getPendingRestart(), Boolean.TRUE);
  }

  private static boolean isAnyPodWalPathPendingRestart(
      StackGresCluster cluster, List<Pod> pods) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getPodStatuses)
        .stream()
        .flatMap(List::stream)
        .anyMatch(clusterPodStatus -> pods.stream()
            .anyMatch(pod -> isPodWalPathPendingRestart(cluster, clusterPodStatus, pod)));
  }

  private static boolean isPodWalPathPendingRestart(StackGresCluster cluster, Pod pod) {
    return Optional.ofNullable(cluster.getStatus())
        .map(StackGresClusterStatus::getPodStatuses)
        .stream()
        .flatMap(List::stream)
        .anyMatch(clusterPodStatus -> isPodWalPathPendingRestart(cluster, clusterPodStatus, pod));
  }

  private static boolean isPodWalPathPendingRestart(
      StackGresCluster cluster, StackGresClusterPodStatus clusterPodStatus, Pod pod) {
    // A pod status without an applied WAL path means either that the WAL directory is placed,
    // as by default, under the PostgreSQL data directory or that the cluster controller has
    // not reported the applied WAL path yet: in both cases the pod is not flagged (when
    // setting walPath the change of the pod template already flags the pod for restart).
    return pod.getMetadata().getName().equals(clusterPodStatus.getName())
        && clusterPodStatus.getWalPath() != null
        && !Objects.equals(
            CustomPersistentVolumeUtil.getWalPath(cluster).orElse(null),
            clusterPodStatus.getWalPath());
  }

  /**
   * Check pending upgrade status condition.
   */
  private static boolean isPendingUpgrade(StackGresCluster cluster) {
    return StackGresVersion.getStackGresVersion(cluster) != StackGresVersion.LATEST;
  }

  public static boolean isPodInFailedPhase(Pod pod) {
    return Optional.ofNullable(pod.getStatus())
        .map(status -> "Failed".equals(status.getPhase()))
        .orElse(false);
  }

  public static boolean isPodReady(
      Pod pod) {
    return Optional.ofNullable(pod.getStatus())
        .map(PodStatus::getConditions)
        .stream()
        .flatMap(List::stream)
        .anyMatch(condition -> "Ready".equals(condition.getType()) && "True".equals(condition.getStatus()));
  }

}
