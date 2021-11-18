/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static io.stackgres.jobs.dbops.clusterrestart.PodWatcher.RUNNING_POD_STATUS_PHASE;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodStatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.smallrye.mutiny.TimeoutException;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockitoAnnotations;

@QuarkusTest
@WithKubernetesTestServer
class PodWatcherTest {

  @Inject
  PodWatcher podWatcher;

  @Inject
  KubernetesClient client;

  String namespace;
  String podName;
  String podUid;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    namespace = StringUtils.getRandomNamespace();
    podName = StringUtils.getRandomClusterName();
    podUid = StringUtils.getRandomClusterName();
    client.namespaces().withName(namespace).create(
        new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build());
  }

  @Test
  void givenNoPodCreated_waitUntilIsCreatedShouldFail() {

    assertThrows(TimeoutException.class, () -> podWatcher.waitUntilIsCreated(podName, namespace)
        .await().atMost(Duration.ofSeconds(3)));

  }

  @Test
  void givenAPodCreated_waitUntilIsCreatedShouldPass() {

    client.pods().inNamespace(namespace).withName(podName)
        .create(new PodBuilder().withNewMetadata().withName(podName).endMetadata().build());

    var pod = podWatcher.waitUntilIsCreated(podName, namespace)
        .await().atMost(Duration.ofMillis(3));

    assertEquals(podName, pod.getMetadata().getName());

  }

  @Test
  void givenADelayedPodCreation_waitUntilIsCreatedShouldWaitForTheCreation() throws Exception {

    CompletableFuture<Pod> futurePod = new CompletableFuture<>();

    podWatcher.waitUntilIsCreated(podName, namespace)
        .subscribe().with(futurePod::complete);

    Thread.sleep(1000);
    client.pods().inNamespace(namespace).withName(podName)
        .create(new PodBuilder()
            .withNewMetadata()
            .withName(podName)
            .endMetadata()
            .build());

    Pod pod = futurePod.get();

    assertEquals(podName, pod.getMetadata().getName());

  }

  @Test
  void givenPodRestartedSuccessfully_statusShouldFinishWithStatusCompleted() {

    var ready = new PodStatusBuilder().withPhase(RUNNING_POD_STATUS_PHASE).build();
    var mockedPod = new PodBuilder().withStatus(ready).withNewMetadata().withName(podName)
        .endMetadata().build();
    client.pods().inNamespace(namespace).withName(podName)
        .create(mockedPod);

    Pod pod = podWatcher.waitUntilIsReady(podName, namespace).await().atMost(Duration.ofMillis(3));

    assertEquals(podName, pod.getMetadata().getName());
  }

  @ParameterizedTest
  @ValueSource(strings = {"Unknown", "Failed"})
  void givenPodRestartedFailed_statusShouldFinishWithFailureOrUnknown(String status) {

    var ready = new PodStatusBuilder().withPhase(status).build();
    var mockedPod = new PodBuilder().withStatus(ready).withNewMetadata().withName(podName)
        .endMetadata().build();
    client.pods().inNamespace(namespace).withName(podName).create(mockedPod);

    Pod invalidPod = podWatcher.waitUntilIsReady(podName, namespace).await()
        .atMost(Duration.ofMillis(360 * 1000));

    assertNull(invalidPod);
  }

}
