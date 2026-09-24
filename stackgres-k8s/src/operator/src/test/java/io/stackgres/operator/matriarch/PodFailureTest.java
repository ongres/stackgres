/*
 * Copyright (C) 2026 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.matriarch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import org.junit.jupiter.api.Test;

/**
 * The pod-failure classifier: a pod stuck past the grace window in a back-off / config / unschedulable
 * state is FAILED (with a reason); a young pod or a transient state (ContainerCreating) is not — so a
 * normal, slow-but-succeeding start is never mislabeled.
 */
class PodFailureTest {

  private static final Instant NOW = Instant.parse("2026-09-23T12:00:00Z");
  private static final Duration GRACE = Duration.ofSeconds(60);
  private static final String OLD = NOW.minusSeconds(120).toString();   // past the grace window
  private static final String YOUNG = NOW.minusSeconds(10).toString();  // still within grace

  private static Pod waiting(String name, String created, String reason, int restarts) {
    return new PodBuilder()
        .withNewMetadata().withName(name).withCreationTimestamp(created).endMetadata()
        .withNewStatus()
          .addNewContainerStatus()
            .withName("patroni").withRestartCount(restarts)
            .withNewState().withNewWaiting().withReason(reason).endWaiting().endState()
          .endContainerStatus()
        .endStatus()
        .build();
  }

  private static Pod unschedulable(String name, String created) {
    return new PodBuilder()
        .withNewMetadata().withName(name).withCreationTimestamp(created).endMetadata()
        .withNewStatus()
          .addNewCondition().withType("PodScheduled").withStatus("False")
              .withReason("Unschedulable").withMessage("0/1 nodes are available").endCondition()
        .endStatus()
        .build();
  }

  @Test
  void oldImagePullBackOffIsFailed() {
    Optional<String> r = PodFailure.detect(List.of(waiting("c-0", OLD, "ImagePullBackOff", 0)), NOW, GRACE);
    assertTrue(r.isPresent());
    assertTrue(r.get().startsWith("c-0"));
    assertTrue(r.get().contains("ImagePullBackOff"));
  }

  @Test
  void oldCrashLoopReportsRestartCount() {
    Optional<String> r = PodFailure.detect(List.of(waiting("c-0", OLD, "CrashLoopBackOff", 5)), NOW, GRACE);
    assertTrue(r.isPresent());
    assertTrue(r.get().contains("CrashLoopBackOff"));
    assertTrue(r.get().contains("restarts=5"));
  }

  @Test
  void oldUnschedulableIsFailed() {
    Optional<String> r = PodFailure.detect(List.of(unschedulable("c-0", OLD)), NOW, GRACE);
    assertTrue(r.isPresent());
    assertTrue(r.get().contains("Unschedulable"));
  }

  @Test
  void youngFailingPodIsNotYetFailed() {
    assertFalse(PodFailure.detect(
        List.of(waiting("c-0", YOUNG, "ImagePullBackOff", 0)), NOW, GRACE).isPresent());
  }

  @Test
  void transientWaitingReasonIsNotFailed() {
    assertFalse(PodFailure.detect(
        List.of(waiting("c-0", OLD, "ContainerCreating", 0)), NOW, GRACE).isPresent());
  }

  @Test
  void noPodsIsNotFailed() {
    assertFalse(PodFailure.detect(List.of(), NOW, GRACE).isPresent());
  }
}
