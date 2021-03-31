/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
      podRestart.restartPod(pod).await().indefinitely();
      fail("Exception on delete should be raised");
    } catch (Exception e){
      verify(podWriter).delete(pod);
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
    } catch (Exception e){
      verify(podWriter).delete(pod);
      verify(podWatcher).waitUntilIsReplaced(any());
      verify(podWatcher).waitUntilIsReady(anyString(), anyString());
    }
  }
}