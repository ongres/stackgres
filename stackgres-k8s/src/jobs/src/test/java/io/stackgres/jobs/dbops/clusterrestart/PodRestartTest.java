/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.stackgres.common.resource.PodWriter;
import io.stackgres.jobs.dbops.mock.MockKubeDbTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

@WithKubernetesTestServer
@QuarkusTest
class PodRestartTest extends MockKubeDbTest {

  private static final int MAX_RETRY_ATTEMPTS = 11;

  @Inject
  PodRestart podRestart;

  @InjectMock
  PodWatcher podWatcher;

  @InjectMock
  PodWriter podWriter;

  private String clusterName;
  private Pod pod;

  @BeforeEach
  void setUp() {
    clusterName = "pod";
    pod = new PodBuilder()
        .withNewMetadata()
        .withCreationTimestamp("1")
        .withName(clusterName + "-0")
        .withNamespace("pod-namespace")
        .endMetadata()
        .build();
  }

  @Test
  void podRestart_shouldDeleteThePodFirstThenWaitForCreationThenWaitForReadiness() {
    String podName = pod.getMetadata().getName();
    String podNamespace = pod.getMetadata().getNamespace();

    when(podWatcher.waitUntilIsCreated(anyString(), anyString()))
        .thenReturn(Uni.createFrom().item(() -> pod));

    doNothing().when(podWriter)
        .delete(pod);

    when(podWatcher.waitUntilIsReplaced(pod))
        .thenReturn(Uni.createFrom().item(new PodBuilder()
            .withNewMetadata()
            .withName(podName)
            .withNamespace(podNamespace)
            .endMetadata()
            .build()));

    when(podWatcher.waitUntilIsReady(clusterName, podName, podNamespace, true))
        .thenReturn(Uni.createFrom().item(new PodBuilder()
            .withNewMetadata()
            .withName(podName)
            .withNamespace(podNamespace)
            .endMetadata()
            .build()));

    podRestart.restartPod(clusterName, pod).subscribe()
      .withSubscriber(UniAssertSubscriber.create())
      .awaitItem()
      .assertCompleted();
    InOrder inOrder = Mockito.inOrder(podWriter, podWatcher);
    inOrder.verify(podWatcher).waitUntilIsCreated(podName, podNamespace);
    inOrder.verify(podWriter).delete(pod);
    inOrder.verify(podWatcher).waitUntilIsReplaced(pod);
    inOrder.verify(podWatcher).waitUntilIsReady(clusterName, podName, podNamespace, true);
  }

  @Test
  void returnedPod_shouldBeTheNewlyCreatedOne() {
    String podName = pod.getMetadata().getName();
    String podNamespace = pod.getMetadata().getNamespace();

    when(podWatcher.waitUntilIsCreated(anyString(), anyString()))
        .thenReturn(Uni.createFrom().item(() -> pod));

    doNothing().when(podWriter)
        .delete(pod);

    when(podWatcher.waitUntilIsReplaced(pod))
        .thenReturn(Uni.createFrom().item(new PodBuilder()
            .withNewMetadata()
            .withName(podName)
            .withNamespace(podNamespace)
            .endMetadata()
            .build()));

    when(podWatcher.waitUntilIsReady(clusterName, podName, podNamespace, true))
        .thenReturn(Uni.createFrom().item(new PodBuilder()
            .withNewMetadata()
            .withName(podName)
            .withNamespace(podNamespace)
            .withCreationTimestamp("3")
            .endMetadata()
            .build()));

    UniAssertSubscriber<Pod> subscriber = podRestart.restartPod(clusterName, pod).subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .awaitItem()
        .assertCompleted();
    assertEquals("3", subscriber.getItem().getMetadata().getCreationTimestamp());
  }

  @Test
  void ifPodDeletionsFails_itShouldRetryAndIsReplacedShouldNotBeCalled() {
    when(podWatcher.waitUntilIsCreated(anyString(), anyString()))
        .thenReturn(Uni.createFrom().item(() -> pod));

    doThrow(new RuntimeException())
        .when(podWriter).delete(pod);

    podRestart.restartPod(clusterName, pod).subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .awaitFailure()
        .assertFailed();
    verify(podWatcher, times(1)).waitUntilIsCreated(anyString(), anyString());
    verify(podWriter, times(MAX_RETRY_ATTEMPTS)).delete(any());
    verify(podWatcher, never()).waitUntilIsReplaced(any());
    verify(podWatcher, never()).waitUntilIsReady(any(), anyString(), anyString(), anyBoolean());
  }

  @Test
  void ifRemovedWaitFails_itShouldRetryAndIsReplacedShouldNotBeCalled() {
    when(podWatcher.waitUntilIsCreated(anyString(), anyString()))
        .thenReturn(Uni.createFrom().item(() -> pod));

    doNothing().when(podWriter)
        .delete(pod);

    when(podWatcher.waitUntilIsReplaced(pod))
        .thenReturn(Uni.createFrom().failure(() -> new RuntimeException()));

    podRestart.restartPod(clusterName, pod).subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .awaitFailure()
        .assertFailed();
    verify(podWatcher, times(1)).waitUntilIsCreated(anyString(), anyString());
    verify(podWriter, times(MAX_RETRY_ATTEMPTS)).delete(pod);
    verify(podWatcher, times(MAX_RETRY_ATTEMPTS)).waitUntilIsReplaced(any());
    verify(podWatcher, never()).waitUntilIsReady(
        any(), anyString(), anyString(), anyBoolean());
  }

  @Test
  void ifReadinessWaitFails_itShouldRetryAndFinallyThrownAnException() {
    String podName = pod.getMetadata().getName();
    String podNamespace = pod.getMetadata().getNamespace();

    when(podWatcher.waitUntilIsCreated(anyString(), anyString()))
        .thenReturn(Uni.createFrom().item(() -> pod));

    doNothing().when(podWriter)
        .delete(pod);

    when(podWatcher.waitUntilIsReplaced(pod))
        .thenReturn(Uni.createFrom().item(pod));

    when(podWatcher.waitUntilIsReady(clusterName, podName, podNamespace, true))
        .thenReturn(Uni.createFrom().failure(() -> new RuntimeException()));

    podRestart.restartPod(clusterName, pod).subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .awaitFailure()
        .assertFailed();
    verify(podWatcher, times(1)).waitUntilIsCreated(anyString(), anyString());
    verify(podWriter, times(MAX_RETRY_ATTEMPTS)).delete(pod);
    verify(podWatcher, times(MAX_RETRY_ATTEMPTS)).waitUntilIsReplaced(any());
    verify(podWatcher, times(MAX_RETRY_ATTEMPTS)).waitUntilIsReady(
        any(), anyString(), anyString(), anyBoolean());
  }

  @Test
  void ifReadinessWaitDetectsStatefulSetChange_itShouldRetry() {
    String podName = pod.getMetadata().getName();
    String podNamespace = pod.getMetadata().getNamespace();

    when(podWatcher.waitUntilIsCreated(anyString(), anyString()))
        .thenReturn(Uni.createFrom().item(() -> pod));

    doNothing().when(podWriter)
        .delete(pod);

    when(podWatcher.waitUntilIsReplaced(pod))
        .thenReturn(Uni.createFrom().item(pod));

    when(podWatcher.waitUntilIsReady(clusterName, podName, podNamespace, true))
        .thenReturn(Uni.createFrom()
            .failure(() -> new StatefulSetChangedException("test")))
        .thenReturn(Uni.createFrom().item(pod));

    podRestart.restartPod(clusterName, pod).subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .awaitItem()
        .assertCompleted();
    verify(podWatcher, times(1)).waitUntilIsCreated(anyString(), anyString());
    verify(podWriter, times(2)).delete(pod);
    verify(podWatcher, times(2)).waitUntilIsReplaced(any());
    verify(podWatcher, times(2)).waitUntilIsReady(
        any(), anyString(), anyString(), anyBoolean());
  }
}
