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
import io.stackgres.common.patroni.PatroniMember.PendingRestartReason;
import org.jooq.lambda.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterRolloutUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRolloutUtil.class);

  private static final String CONTROLLER_REVISION_HASH_LABEL = "controller-revision-hash";
  private static final CronParser CRON_PARSER =
      new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

  public static final String DBOPS_NOT_FOUND_NAME = "__DBOPS_NOT_FOUND__";

  /**
   * Parameters that a hot standby requires to be set to a value greater than or equal to the value
   * set on the primary.
   *
   * @see <a href="https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-ADMIN">
   *      Hot Standby Parameter Reference</a>
   */
  public static final Set<String> HOT_STANDBY_SENSITIVE_PARAMETERS = Set.of(
      "max_connections",
      "max_prepared_transactions",
      "max_locks_per_transaction",
      "max_wal_senders",
      "max_worker_processes");

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
    UPGRADE;
  }

  public enum PodRestartReason {
    STATEFULSET,
    POD_STATUS;
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

  /**
   * A hot standby refuses to continue the recovery if any of the
   * {@link #HOT_STANDBY_SENSITIVE_PARAMETERS} is set to a value lower than the value set on the
   * primary. When any of those parameters is decreased the primary has to apply the new value
   * before the replicas do, therefore the Postgres instance of the primary has to be restarted in
   * place instead of performing a switchover.
   *
   * <p>
   * Patroni only reports the reason of a pending restart since version 4. When it is not reported
   * it is not possible to tell if a hot standby sensitive parameter is being decreased, therefore
   * a switchover is performed.
   * </p>
   */
  public static boolean requiresPostgresRestartWithoutSwitchover(
      Pod pod,
      List<PatroniMember> patroniMembers) {
    return patroniMembers.stream()
        .filter(patroniMember -> patroniMember.getMember().equals(pod.getMetadata().getName()))
        .anyMatch(ClusterRolloutUtil::requiresPostgresRestartWithoutSwitchover);
  }

  private static boolean requiresPostgresRestartWithoutSwitchover(PatroniMember patroniMember) {
    if (patroniMember.getPendingRestart() == null) {
      return false;
    }
    var pendingRestartReasons = patroniMember.getPendingRestartReasons();
    if (pendingRestartReasons.isEmpty()) {
      LOGGER.debug("Patroni did not report the reason of the pending restart of member {},"
          + " skip restarting the Postgres instance in place", patroniMember.getMember());
      return false;
    }
    return pendingRestartReasons.entrySet()
        .stream()
        .filter(pendingRestartReason ->
            HOT_STANDBY_SENSITIVE_PARAMETERS.contains(pendingRestartReason.getKey()))
        .anyMatch(pendingRestartReason -> isDecreased(
            pendingRestartReason.getKey(), pendingRestartReason.getValue()));
  }

  private static boolean isDecreased(String parameter, PendingRestartReason pendingRestartReason) {
    try {
      return Long.parseLong(pendingRestartReason.newValue())
          < Long.parseLong(pendingRestartReason.oldValue());
    } catch (NumberFormatException ex) {
      LOGGER.warn("Unable to compare values {} and {} of parameter {}",
          pendingRestartReason.oldValue(), pendingRestartReason.newValue(), parameter, ex);
      return false;
    }
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
