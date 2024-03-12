/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodConditionBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.stackgres.testutil.StringUtils;
import io.vertx.junit5.Timeout;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@WithKubernetesTestServer
class PodWatcherTest {

  @Inject
  PodWatcher podWatcher;

  @Inject
  KubernetesClient client;

  String namespace;
  String clusterName;
  String podName;

  ExecutorService testExecutor;

  @BeforeEach
  void setUp() {
    namespace = StringUtils.getRandomNamespace();
    clusterName = StringUtils.getRandomResourceName();
    podName = clusterName + "-" + new Random().nextInt(128);
    client.namespaces()
        .resource(new NamespaceBuilder()
            .withNewMetadata()
            .withName(namespace)
            .endMetadata()
            .build())
        .create();
    testExecutor = Executors.newSingleThreadExecutor();
  }

  @AfterEach
  void tearDown() throws Exception {
    testExecutor.shutdown();
    testExecutor.awaitTermination(1, TimeUnit.SECONDS);
  }

  @Test
  @Timeout(3)
  void givenNoPodCreated_waitUntilIsCreatedShouldFail() throws Exception {
    UniAssertSubscriber<Pod> subscriber = podWatcher.waitUntilIsCreated(podName, namespace)
        .runSubscriptionOn(testExecutor)
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    Thread.sleep(100);
    subscriber.assertNotTerminated();
  }

  @Test
  @Timeout(3)
  void givenAPodCreated_waitUntilIsCreatedShouldPass() {
    client.pods().inNamespace(namespace)
        .resource(new PodBuilder().withNewMetadata().withName(podName).endMetadata().build())
        .create();

    var pod = podWatcher.waitUntilIsCreated(podName, namespace)
        .runSubscriptionOn(testExecutor)
        .subscribe().withSubscriber(UniAssertSubscriber.create())
        .awaitItem()
        .assertCompleted().getItem();

    assertEquals(podName, pod.getMetadata().getName());
  }

  @Test
  void givenADelayedPodCreation_waitUntilIsCreatedShouldWaitForTheCreation() throws Exception {
    UniAssertSubscriber<Pod> subscriber = podWatcher.waitUntilIsCreated(podName, namespace)
        .runSubscriptionOn(testExecutor)
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    Thread.sleep(100);
    subscriber.assertNotTerminated();

    client.pods().inNamespace(namespace)
        .resource(new PodBuilder()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(podName)
            .endMetadata()
            .build())
        .create();

    Pod pod = subscriber.awaitItem().assertCompleted().getItem();

    assertEquals(podName, pod.getMetadata().getName());
  }

  @Test
  @Timeout(3)
  void givenNoPodCreated_waitUntilIsRemovedShouldPass() {
    var podDeleted = new PodBuilder()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(podName)
            .endMetadata()
            .build();
    podWatcher.waitUntilIsRemoved(podDeleted)
        .runSubscriptionOn(testExecutor)
        .subscribe().withSubscriber(UniAssertSubscriber.create())
        .awaitItem()
        .assertCompleted();
  }

  @Test
  void givenPodCreated_waitUntilIsRemovedShouldWaitForThePodToBeRemoved() throws Exception {
    var podCreated = client.pods().inNamespace(namespace)
        .resource(new PodBuilder()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(podName)
            .endMetadata()
            .build())
        .create();

    UniAssertSubscriber<Void> subscriber = podWatcher.waitUntilIsRemoved(podCreated)
        .runSubscriptionOn(testExecutor)
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    Thread.sleep(100);
    subscriber.assertNotTerminated();

    client.pods().inNamespace(namespace).withName(podName).delete();

    subscriber.awaitItem().assertCompleted();
  }

  @Test
  @Timeout(3)
  void givenAPodReplaced_waitUntilIsReplacedShouldPass() {
    Pod pod = client.pods().inNamespace(namespace)
        .resource(new PodBuilder().withNewMetadata()
            .withName(podName).endMetadata().build())
        .create();
    client.pods().inNamespace(namespace).withName(podName).delete();
    client.pods().inNamespace(namespace)
        .resource(new PodBuilder().withNewMetadata()
            .withName(podName).endMetadata().build())
        .create();

    Pod returnedPod = podWatcher.waitUntilIsReplaced(pod)
        .runSubscriptionOn(testExecutor)
        .subscribe().withSubscriber(UniAssertSubscriber.create())
        .awaitItem()
        .assertCompleted().getItem();

    assertEquals(podName, returnedPod.getMetadata().getName());
    assertNotEquals(pod.getMetadata().getCreationTimestamp(),
        returnedPod.getMetadata().getCreationTimestamp());
  }

  @Test
  void givenADelayedPodReplacement_waitUntilIsReplacedShouldWaitForTheReplacement()
      throws Exception {
    Pod pod = client.pods().inNamespace(namespace)
        .resource(new PodBuilder().withNewMetadata()
            .withName(podName).endMetadata().build())
        .create();

    UniAssertSubscriber<Pod> subscriber = podWatcher.waitUntilIsReplaced(pod)
        .runSubscriptionOn(testExecutor)
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    Thread.sleep(100);
    subscriber.assertNotTerminated();

    client.pods().inNamespace(namespace).withName(podName).delete();

    Thread.sleep(100);
    subscriber.assertNotTerminated();

    client.pods().inNamespace(namespace)
        .resource(new PodBuilder().withNewMetadata()
            .withName(podName).endMetadata().build())
        .create();

    Pod returnedPod = subscriber.awaitItem().assertCompleted().getItem();

    assertEquals(podName, pod.getMetadata().getName());
    assertNotEquals(pod.getMetadata().getCreationTimestamp(),
        returnedPod.getMetadata().getCreationTimestamp());
  }

  @Test
  @Timeout(3)
  void givenAPodReady_waitUntilIsReadyShouldPass() {
    client.pods().inNamespace(namespace)
        .resource(new PodBuilder().withNewMetadata()
            .withName(podName).endMetadata()
            .withNewStatus()
            .withConditions(ImmutableList.of(
                new PodConditionBuilder()
                .withType("Ready")
                .withStatus("true")
                .build()))
            .endStatus().build())
        .create();

    var returnedPod = podWatcher.waitUntilIsReady(clusterName, podName, namespace, false)
        .runSubscriptionOn(testExecutor)
        .subscribe().withSubscriber(UniAssertSubscriber.create())
        .awaitItem()
        .assertCompleted().getItem();

    assertEquals(podName, returnedPod.getMetadata().getName());
  }

  @Test
  void givenADelayedPodReady_waitUntilIsReadyShouldWaitForTheReadiness()
      throws Exception {
    Pod pod = client.pods().inNamespace(namespace)
        .resource(new PodBuilder().withNewMetadata()
            .withName(podName).endMetadata()
            .withNewStatus()
            .withConditions(ImmutableList.of(
                new PodConditionBuilder()
                .withType("Ready")
                .withStatus("false")
                .build()))
            .endStatus().build())
        .create();

    UniAssertSubscriber<Pod> subscriber = podWatcher
        .waitUntilIsReady(clusterName, podName, namespace, false)
        .runSubscriptionOn(testExecutor)
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    Thread.sleep(100);
    subscriber.assertNotTerminated();

    client.pods().inNamespace(namespace)
        .resource(new PodBuilder(pod)
            .editStatus()
            .editCondition(0)
            .withStatus("true")
            .endCondition()
            .endStatus().build())
        .update();

    Pod returnedPod = subscriber.awaitItem().assertCompleted().getItem();

    assertEquals(podName, returnedPod.getMetadata().getName());
  }

  @Test
  void givenAnUnchangedStatefulSet_waitUntilIsReadyShouldNotThrowAnExceptionWhenCheckEnabled()
      throws Exception {
    Pod pod = client.pods().inNamespace(namespace)
        .resource(new PodBuilder().withNewMetadata()
            .withName(podName)
            .withLabels(ImmutableMap.of("controller-revision-hash", "test"))
            .endMetadata()
            .withNewStatus()
            .withConditions(ImmutableList.of(
                new PodConditionBuilder()
                .withType("Ready")
                .withStatus("false")
                .build()))
            .endStatus().build())
        .create();
    client.apps().statefulSets()
        .inNamespace(namespace)
        .resource(new StatefulSetBuilder().withNewMetadata()
            .withName(clusterName).endMetadata()
            .withNewStatus()
            .withUpdateRevision("test")
            .endStatus().build())
        .create();

    UniAssertSubscriber<Pod> subscriber = podWatcher
        .waitUntilIsReady(clusterName, podName, namespace, true)
        .runSubscriptionOn(testExecutor)
        .subscribe().withSubscriber(UniAssertSubscriber.create());

    Thread.sleep(100);
    subscriber.assertNotTerminated();

    client.pods().inNamespace(namespace)
        .resource(new PodBuilder(pod)
            .editStatus()
            .editCondition(0)
            .withStatus("true")
            .endCondition()
            .endStatus().build())
        .update();

    Pod returnedPod = subscriber.awaitItem().assertCompleted().getItem();

    assertEquals(podName, returnedPod.getMetadata().getName());
  }

  @Test
  void givenAChangedStatefulSet_waitUntilIsReadyShouldThrowAnExceptionWhenCheckEnabled()
      throws Exception {
    client.pods().inNamespace(namespace)
        .resource(new PodBuilder().withNewMetadata()
            .withName(podName)
            .withLabels(ImmutableMap.of("controller-revision-hash", "wrong"))
            .endMetadata()
            .withNewStatus()
            .withConditions(ImmutableList.of(
                new PodConditionBuilder()
                .withType("Ready")
                .withStatus("false")
                .build()))
            .endStatus().build())
        .create();
    client.apps().statefulSets()
        .inNamespace(namespace)
        .resource(new StatefulSetBuilder().withNewMetadata()
            .withName(clusterName).endMetadata()
            .withNewStatus()
            .withUpdateRevision("test")
            .endStatus().build())
        .create();

    podWatcher.waitUntilIsReady(clusterName, podName, namespace, true)
        .runSubscriptionOn(testExecutor)
        .subscribe().withSubscriber(UniAssertSubscriber.create())
        .awaitFailure()
        .assertFailedWith(StatefulSetChangedException.class, null);
  }

}

