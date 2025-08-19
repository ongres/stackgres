/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodStatus;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.patroni.PatroniMember;

public class ClusterRolloutUtil {

  private static final String CONTROLLER_REVISION_HASH_LABEL = "controller-revision-hash";

  public static boolean isRolloutAllowed(StackGresCluster cluster) {
    Map<String, String> annotations = Optional
        .ofNullable(cluster.getMetadata().getAnnotations())
        .orElse(Map.of());
    if (Objects.equals(
        annotations.get(StackGresContext.ROLLOUT_KEY),
        StackGresContext.ROLLOUT_NEVER_VALUE)) {
      return false;
    }
    if (Objects.equals(
        annotations.get(StackGresContext.ROLLOUT_KEY),
        StackGresContext.ROLLOUT_ALWAYS_VALUE)) {
      return true;
    }
    if (annotations.containsKey(StackGresContext.ROLLOUT_DBOPS_KEY)) {
      return true;
    }
    return false;
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

    public ImmutableSet<RestartReason> getReasons() {
      return ImmutableSet.copyOf(reasons);
    }

    public boolean requiresRestart() {
      return !reasons.isEmpty();
    }
  }

  public enum RestartReason {
    STATEFULSET,
    PATRONI,
    POD_STATUS;
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
