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
import org.jooq.lambda.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterRolloutUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRolloutUtil.class);

  private static final String CONTROLLER_REVISION_HASH_LABEL = "controller-revision-hash";
  private static final CronParser CRON_PARSER =
      new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

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
        || (hasRolloutAnnotation && updateStrategyType == StackGresClusterUpdateStrategyType.ALWAYS)) {
      return true;
    }
    if (annotations.containsKey(StackGresContext.ROLLOUT_DBOPS_KEY)) {
      return true;
    }
    if ((Objects.equals(
        annotations.get(StackGresContext.ROLLOUT_KEY),
        StackGresContext.ROLLOUT_SCHEDULE_VALUE)
        && annotations.containsKey(StackGresContext.ROLLOUT_SCHEDULE_KEY))
        || (hasRolloutAnnotation && updateStrategyType == StackGresClusterUpdateStrategyType.SCHEDULE
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

  public static class RestartReasons {
    final Set<RestartReason> reasons = EnumSet.noneOf(RestartReason.class);

    public static RestartReasons of(RestartReason...reasons) {
      RestartReasons restartReasons = new RestartReasons();
      for (RestartReason reason : reasons) {
        restartReasons.addReason(reason);
      }
      return restartReasons;
    }

    void addReason(RestartReason reason) {
      reasons.add(reason);
    }

    public Set<RestartReason> getReasons() {
      return Set.of(reasons.toArray(RestartReason[]::new));
    }

    public boolean requiresRestart() {
      return !reasons.isEmpty();
    }

    public boolean requiresUpgrade() {
      return reasons.contains(RestartReason.UPGRADE);
    }
  }

  public enum RestartReason {
    STATEFULSET,
    PATRONI,
    POD_STATUS,
    UPGRADE;
  }

  public static RestartReasons getRestartReasons(
      StackGresCluster cluster,
      Optional<StatefulSet> statefulSet,
      List<Pod> pods,
      List<PatroniMember> patroniMembers) {
    final RestartReasons reasons = new RestartReasons();

    if (isStatefulSetPendingRestart(statefulSet, pods)) {
      reasons.addReason(RestartReason.STATEFULSET);
    }

    if (isPatroniPendingRestart(pods, patroniMembers)) {
      reasons.addReason(RestartReason.PATRONI);
    }

    if (isAnyPodPendingRestart(cluster, pods)) {
      reasons.addReason(RestartReason.POD_STATUS);
    }

    if (isPendingUpgrade(cluster)) {
      reasons.addReason(RestartReason.UPGRADE);
    }

    return reasons;
  }

  public static RestartReasons getRestartReasons(
      StackGresCluster cluster,
      Optional<StatefulSet> clusterStatefulSet,
      Pod pod,
      List<PatroniMember> patroniMembers) {
    final RestartReasons reasons = new RestartReasons();

    if (isStatefulSetPodPendingRestart(clusterStatefulSet, pod)) {
      reasons.addReason(RestartReason.STATEFULSET);
    }

    if (isPatroniPendingRestart(pod, patroniMembers)) {
      reasons.addReason(RestartReason.PATRONI);
    }

    if (isPodPendingRestart(cluster, pod)) {
      reasons.addReason(RestartReason.POD_STATUS);
    }

    if (isPendingUpgrade(cluster)) {
      reasons.addReason(RestartReason.UPGRADE);
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
