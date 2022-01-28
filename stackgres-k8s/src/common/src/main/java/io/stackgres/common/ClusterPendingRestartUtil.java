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
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;

public class ClusterPendingRestartUtil {

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
      List<StackGresClusterPodStatus> clusterPodStatuses,
      Optional<StatefulSet> clusterStatefulSet, List<Pod> clusterPods) {
    final RestartReasons reasons = new RestartReasons();

    if (isStatefulSetPendingRestart(clusterStatefulSet, clusterPods)) {
      reasons.addReason(RestartReason.STATEFULSET);
    }

    if (isPatroniPendingRestart(clusterPods)) {
      reasons.addReason(RestartReason.PATRONI);
    }

    if (isAnyPodPendingRestart(clusterPodStatuses, clusterPods)) {
      reasons.addReason(RestartReason.POD_STATUS);
    }

    return reasons;
  }

  private static boolean isStatefulSetPendingRestart(
      Optional<StatefulSet> clusterStatefulSet, List<Pod> clusterPods) {
    return clusterStatefulSet
        .filter(sts -> Optional.ofNullable(sts.getStatus())
            .map(StatefulSetStatus::getUpdateRevision).isPresent())
        .map(sts -> {
          String statefulSetUpdateRevision = sts.getStatus().getUpdateRevision();

          return clusterPods.stream()
              .map(pod -> pod.getMetadata().getLabels().get("controller-revision-hash"))
              .anyMatch(controllerRevisionHash ->
                  !Objects.equals(statefulSetUpdateRevision, controllerRevisionHash));
        })
        .orElse(false);
  }

  private static boolean isPatroniPendingRestart(List<Pod> clusterPods) {
    return Optional.of(clusterPods)
        .map(pods -> pods.stream()
            .map(Pod::getMetadata).filter(Objects::nonNull)
            .map(ObjectMeta::getAnnotations).filter(Objects::nonNull)
            .map(Map::entrySet)
            .anyMatch(p -> p.stream()
                .map(Map.Entry::getValue).filter(Objects::nonNull)
                .anyMatch(r -> r.contains("\"pending_restart\":true")))
        )
        .orElse(false);
  }

  private static boolean isAnyPodPendingRestart(List<StackGresClusterPodStatus> clusterPodStatuses,
      List<Pod> clusterPods) {
    return clusterPodStatuses
        .stream()
        .filter(podStatus -> clusterPods.stream().anyMatch(pod -> pod.getMetadata().getName()
            .equals(podStatus.getName())))
        .map(StackGresClusterPodStatus::getPendingRestart)
        .map(Optional::ofNullable)
        .map(pensingRestart -> pensingRestart.orElse(false))
        .filter(pensingRestart -> pensingRestart)
        .findAny()
        .orElse(false);
  }
}
