/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.resource.PodWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

@QuarkusTest
class PodRestartImplTest {

  private static final int MAX_RETRY_ATTEMPTS = 10;

  @Inject
  PodRestartImpl podRestart;

  @InjectMock
  PodWatcher podWatcher;

  @InjectMock
  PodWriter podWriter;

  private Pod pod;

  @BeforeEach
  void setUp() {
    pod = new PodBuilder()
        .withNewMetadata()
        .withCreationTimestamp("1")
        .withName("pod")
        .withNamespace("pod-namespace")
        .endMetadata()
        .build();
  }

  @Test
  void podRestartShouldDeleteThePodFirstThenWaitForCreationThenWaitForReadiness() {

    String podName = pod.getMetadata().getName();
    String podNamespace = pod.getMetadata().getNamespace();

    doNothing().when(podWriter)
        .delete(pod);

    when(podWatcher.waitUntilIsReplaced(pod))
        .thenReturn(Uni.createFrom().item(new PodBuilder()
            .withNewMetadata()
            .withName(podName)
            .withNamespace(podNamespace)
            .endMetadata()
            .build()));

    when(podWatcher.waitUntilIsReady(podName, podNamespace))
        .thenReturn(Uni.createFrom().item(new PodBuilder()
            .withNewMetadata()
            .withName(podName)
            .withNamespace(podNamespace)
            .endMetadata()
            .build()));

    podRestart.restartPod(pod).await().indefinitely();

    InOrder inOrder = Mockito.inOrder(podWriter, podWatcher);
    inOrder.verify(podWriter).delete(pod);
    inOrder.verify(podWatcher).waitUntilIsReplaced(pod);
    inOrder.verify(podWatcher).waitUntilIsReady(podName, podNamespace);

  }

  @Test
  void returnedPod_shouldBeTheNewlyCreatedOne() {
    String podName = pod.getMetadata().getName();
    String podNamespace = pod.getMetadata().getNamespace();

    doNothing().when(podWriter)
        .delete(pod);

    when(podWatcher.waitUntilIsReplaced(pod))
        .thenReturn(Uni.createFrom().item(new PodBuilder()
            .withNewMetadata()
            .withName(podName)
            .withNamespace(podNamespace)
            .endMetadata()
            .build()));

    when(podWatcher.waitUntilIsReady(podName, podNamespace))
        .thenReturn(Uni.createFrom().item(new PodBuilder()
            .withNewMetadata()
            .withName(podName)
            .withNamespace(podNamespace)
            .withCreationTimestamp("3")
            .endMetadata()
            .build()));

    Pod createdPod = podRestart.restartPod(pod).await().indefinitely();
    assertEquals(podNamespace, createdPod.getMetadata().getNamespace());
    assertEquals(podName, createdPod.getMetadata().getName());
    assertEquals("3", createdPod.getMetadata().getCreationTimestamp());
  }

  @Test
  void ifPodDeletionsFails_waitUntilIsCreatedShouldNotBeCalled() {
    doThrow(new RuntimeException())
        .when(podWriter).delete(pod);

    try {
      podRestart.restartPod(pod).await().atMost(Duration.ofSeconds(3));
      fail("Exception on delete should be raised");
    } catch (Exception e) {
      verify(podWriter, atLeast(1)).delete(pod);
      verify(podWatcher, never()).waitUntilIsReplaced(any());
      verify(podWatcher, never()).waitUntilIsReady(anyString(), anyString());
    }
  }

  @Test
  void ifReadinessWaitFailed_idShouldThrownAnException() {

    String podName = pod.getMetadata().getName();
    String podNamespace = pod.getMetadata().getNamespace();

    doNothing().when(podWriter)
        .delete(pod);

    when(podWatcher.waitUntilIsRemoved(podName, podNamespace))
        .thenReturn(Uni.createFrom().voidItem());

    when(podWatcher.waitUntilIsReady(podName, podNamespace))
        .thenReturn(Uni.createFrom().failure(() -> {
          throw new RuntimeException();
        }));

    try {
      podRestart.restartPod(pod).await().indefinitely();
      fail("Exceptions raised during wait until create should be raised");
    } catch (Exception e) {
      verify(podWriter, atLeast(MAX_RETRY_ATTEMPTS)).delete(pod);
      verify(podWatcher, atLeast(MAX_RETRY_ATTEMPTS)).waitUntilIsReplaced(any());
      verify(podWatcher, atLeast(MAX_RETRY_ATTEMPTS)).waitUntilIsReady(anyString(), anyString());
    }
  }
}
