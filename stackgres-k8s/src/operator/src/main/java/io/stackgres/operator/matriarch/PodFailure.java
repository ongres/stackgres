/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;

/**
 * Classifies whether a cluster's pods are in a <em>persisted</em> terminal-ish failure the create watch
 * and the read model should surface as FAILED — image-pull errors, crash loops, or unschedulable pods
 * stuck longer than a grace window (so a normal, slow-but-succeeding start is never mislabeled). Pure and
 * stateless: it reflects the pods' current state + age, so a recovered pod stops being reported.
 */
final class PodFailure {

  private PodFailure() {
  }

  // Waiting reasons that mean "this container won't start on its own" — not the transient
  // ContainerCreating / PodInitializing. CrashLoopBackOff/ImagePullBackOff are already back-off states.
  private static final List<String> TERMINAL_WAITING_REASONS = List.of(
      "CrashLoopBackOff", "ImagePullBackOff", "ErrImagePull", "InvalidImageName",
      "CreateContainerConfigError", "CreateContainerError");

  /**
   * A reason string when some pod is persistently failing (older than {@code grace}), else empty.
   */
  static Optional<String> detect(List<Pod> pods, Instant now, Duration grace) {
    if (pods == null) {
      return Optional.empty();
    }
    for (Pod pod : pods) {
      if (youngerThanGrace(pod, now, grace)) {
        continue;   // too new to judge — a normal start is still converging
      }
      Optional<String> reason = failureReason(pod);
      if (reason.isPresent()) {
        String name = pod.getMetadata() != null ? pod.getMetadata().getName() : "pod";
        return Optional.of(name + ": " + reason.get());
      }
    }
    return Optional.empty();
  }

  private static boolean youngerThanGrace(Pod pod, Instant now, Duration grace) {
    if (pod.getMetadata() == null || pod.getMetadata().getCreationTimestamp() == null) {
      return true;   // no age info → don't judge
    }
    try {
      return Instant.parse(pod.getMetadata().getCreationTimestamp()).isAfter(now.minus(grace));
    } catch (RuntimeException e) {
      return true;   // unparseable timestamp → don't judge
    }
  }

  private static Optional<String> failureReason(Pod pod) {
    if (pod.getStatus() == null) {
      return Optional.empty();
    }
    // A back-off / config error on any (init) container — reflects the current waiting state, so a
    // container that recovers to Running clears it.
    Optional<String> containerReason = containerFailure(pod.getStatus().getInitContainerStatuses())
        .or(() -> containerFailure(pod.getStatus().getContainerStatuses()));
    if (containerReason.isPresent()) {
      return containerReason;
    }
    // Unschedulable: the scheduler couldn't place the pod (anti-affinity, resources, unbound PVC).
    if (pod.getStatus().getConditions() != null) {
      for (PodCondition c : pod.getStatus().getConditions()) {
        if ("PodScheduled".equals(c.getType()) && "False".equals(c.getStatus())) {
          String msg = c.getMessage() != null && !c.getMessage().isBlank() ? ": " + c.getMessage() : "";
          return Optional.of("Unschedulable" + msg);
        }
      }
    }
    return Optional.empty();
  }

  private static Optional<String> containerFailure(List<ContainerStatus> statuses) {
    if (statuses == null) {
      return Optional.empty();
    }
    for (ContainerStatus cs : statuses) {
      if (cs.getState() != null && cs.getState().getWaiting() != null) {
        String reason = cs.getState().getWaiting().getReason();
        if (reason != null && TERMINAL_WAITING_REASONS.contains(reason)) {
          int restarts = cs.getRestartCount() != null ? cs.getRestartCount() : 0;
          return Optional.of(restarts > 0 ? reason + " (restarts=" + restarts + ")" : reason);
        }
      }
    }
    return Optional.empty();
  }
}
