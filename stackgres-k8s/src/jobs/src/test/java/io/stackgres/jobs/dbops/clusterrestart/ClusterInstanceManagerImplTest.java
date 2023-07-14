/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.kubernetes.client.KubernetesTestServer;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.StackGresKubernetesMockServerSetup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.jobs.dbops.lock.FakeClusterScheduler;
import io.stackgres.jobs.dbops.lock.MockKubeDb;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@QuarkusTest
@WithKubernetesTestServer(https = true, setup = StackGresKubernetesMockServerSetup.class)
class ClusterInstanceManagerImplTest {

  @Inject
  MockKubeDb kubeDb;

  @KubernetesTestServer
  KubernetesServer mockServer;

  @InjectMock
  PodWatcherImpl podWatcher;

  @Inject
  ClusterInstanceManagerImpl clusterInstanceManager;

  @Inject
  PodTestUtil podTestUtil;

  @InjectSpy
  FakeClusterScheduler clusterScheduler;

  String namespace;

  String clusterName;

  StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    namespace = StringUtils.getRandomNamespace();
    clusterName = StringUtils.getRandomClusterName();

    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().setName(clusterName);
    cluster.getMetadata().setNamespace(namespace);
    cluster.getSpec().setInstances(3);
    cluster = kubeDb.addOrReplaceCluster(cluster);
  }

  @Test
  void givenACleanCluster_increaseInstancesShouldWaitUntilTheNewPodIsCreated() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    final Pod newPod = podTestUtil.buildReplicaPod(cluster, 3);
    final String newPodName = newPod.getMetadata().getName();

    configureCreationPodWatchers();

    configureNewPodCreatedOnClusterEvent(newPod);

    final int initialInstances = cluster.getSpec().getInstances();

    Pod createdPod = clusterInstanceManager.increaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    PodTestUtil.assertPodEquals(newPod, createdPod);

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances + 1, actualInstances);

    verify(podWatcher).waitUntilIsReady(clusterName, newPodName, namespace, false);

  }

  @Test
  void givenAClusterWithANonDisruptablePod_increaseInstancesShouldNotFail() {
    podTestUtil.preparePods(cluster, 0, 1, 2);
    configureNonDisruptablePod(0);
    final Pod newPod = podTestUtil.buildReplicaPod(cluster, 3);
    final int initialInstances = cluster.getSpec().getInstances();
    final String newPodName = newPod.getMetadata().getName();

    configureCreationPodWatchers();
    configureNewPodCreatedOnClusterEvent(newPod);

    Pod createdPod = clusterInstanceManager.increaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    PodTestUtil.assertPodEquals(newPod, createdPod);

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances + 1, actualInstances);

    InOrder order = inOrder(podWatcher);

    order.verify(podWatcher).waitUntilIsReady(clusterName, newPodName, namespace, false);
  }

  @Test
  void givenAClusterWithASingleNonDisruptablePod_increaseInstancesShouldNotFail() {
    cluster.getSpec().setInstances(1);
    podTestUtil.preparePods(cluster, 1);
    configureNonDisruptablePod(1);
    final Pod newPod = podTestUtil.buildReplicaPod(cluster, 0);
    final int initialInstances = cluster.getSpec().getInstances();
    final String newPodName = newPod.getMetadata().getName();

    configureCreationPodWatchers();
    configureNewPodCreatedOnClusterEvent(newPod);

    Pod createdPod = clusterInstanceManager.increaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    PodTestUtil.assertPodEquals(newPod, createdPod);

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances + 1, actualInstances);

    InOrder order = inOrder(podWatcher);

    order.verify(podWatcher).waitUntilIsReady(clusterName, newPodName, namespace, false);
  }

  @Test
  void givenAClusterWithAFarNonDisruptablePod_itShouldWaitForTheRightPodToBeCreated() {
    podTestUtil.preparePods(cluster, 5, 0, 1);
    configureNonDisruptablePod(5);
    final Pod newPod = podTestUtil.buildReplicaPod(cluster, 2);
    final int initialInstances = cluster.getSpec().getInstances();
    final String newPodName = newPod.getMetadata().getName();

    configureCreationPodWatchers();
    configureNewPodCreatedOnClusterEvent(newPod);

    Pod createdPod = clusterInstanceManager.increaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    PodTestUtil.assertPodEquals(newPod, createdPod);

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances + 1, actualInstances);

    verify(podWatcher).waitUntilIsReady(clusterName, newPodName, namespace, false);
  }

  @Test
  void givenACleanCluster_itShouldWaitForTheRightPodToBeDeleted() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    Pod replicaToDelete = podTestUtil.buildReplicaPod(cluster, 2);

    final int initialInstances = cluster.getSpec().getInstances();

    String podName = replicaToDelete.getMetadata().getName();

    when(podWatcher.waitUntilIsRemoved(podName, namespace))
        .thenReturn(Uni.createFrom().voidItem());

    configurePodDeletedOnClusterEvent(replicaToDelete);

    clusterInstanceManager.decreaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    var pods = podTestUtil.getClusterPods(cluster);

    assertEquals(2, pods.size());

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances - 1, actualInstances);

    verify(podWatcher).waitUntilIsRemoved(podName, namespace);
  }

  @Test
  void givenAClusterWithANonDisruptablePod_decreaseInstancesShouldNotFail() {

    podTestUtil.preparePods(cluster, 0, 1, 2);
    Pod replicaToDelete = podTestUtil.buildReplicaPod(cluster, 2);

    configureNonDisruptablePod(0);

    final int initialInstances = cluster.getSpec().getInstances();

    String podName = replicaToDelete.getMetadata().getName();

    when(podWatcher.waitUntilIsRemoved(podName, namespace))
        .thenReturn(Uni.createFrom().voidItem());

    configurePodDeletedOnClusterEvent(replicaToDelete);

    clusterInstanceManager.decreaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    var pods = podTestUtil.getClusterPods(cluster);

    assertEquals(2, pods.size());

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances - 1, actualInstances);

    verify(podWatcher).waitUntilIsRemoved(podName, namespace);
  }

  @Test
  void givenAClusterWithAFarNonDisruptablePod_decreaseInstancesShouldNotFail() {
    podTestUtil.preparePods(cluster, 5, 1, 2);
    Pod replicaToDelete = podTestUtil.buildReplicaPod(cluster, 2);

    configureNonDisruptablePod(5);

    final int initialInstances = cluster.getSpec().getInstances();
    String podName = replicaToDelete.getMetadata().getName();

    when(podWatcher.waitUntilIsRemoved(podName, namespace))
        .thenReturn(Uni.createFrom().voidItem());

    configurePodDeletedOnClusterEvent(replicaToDelete);

    clusterInstanceManager.decreaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    var pods = podTestUtil.getClusterPods(cluster);

    assertEquals(2, pods.size());

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances - 1, actualInstances);

    verify(podWatcher).waitUntilIsRemoved(podName, namespace);

  }

  @Test
  void givenAClusterWithASingleNode_decreaseInstancesShouldNotFail() {
    cluster.getSpec().setInstances(1);
    cluster = kubeDb.addOrReplaceCluster(cluster);

    podTestUtil.preparePods(cluster, 0);
    Pod podToDelete = podTestUtil.buildPrimaryPod(cluster, 0);

    final int initialInstances = cluster.getSpec().getInstances();
    String podName = podToDelete.getMetadata().getName();

    when(podWatcher.waitUntilIsRemoved(podName, namespace))
        .thenReturn(Uni.createFrom().voidItem());

    configurePodDeletedOnClusterEvent(podToDelete);

    clusterInstanceManager.decreaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    var pods = podTestUtil.getClusterPods(cluster);

    assertEquals(0, pods.size());

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances - 1, actualInstances);

    verify(podWatcher).waitUntilIsRemoved(podName, namespace);
  }

  @Test
  void givenAClusterWithASingleNonDisruptable_decreaseInstancesShouldNotFail() {
    cluster.getSpec().setInstances(1);
    cluster = kubeDb.addOrReplaceCluster(cluster);

    podTestUtil.preparePods(cluster, 5);
    configureNonDisruptablePod(5);
    Pod podToDelete = podTestUtil.buildPrimaryPod(cluster, 5);

    final int initialInstances = cluster.getSpec().getInstances();
    String podName = podToDelete.getMetadata().getName();

    when(podWatcher.waitUntilIsRemoved(podName, namespace))
        .thenReturn(Uni.createFrom().voidItem());

    configurePodDeletedOnClusterEvent(podToDelete);

    clusterInstanceManager.decreaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    var pods = podTestUtil.getClusterPods(cluster);

    assertEquals(0, pods.size());

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances - 1, actualInstances);

    verify(podWatcher).waitUntilIsRemoved(podName, namespace);
  }

  @Test
  void givenAIncreasingInstanceFailure_operationShouldBeRetried() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    final Pod newPod = podTestUtil.buildReplicaPod(cluster, 3);

    configureCreationPodWatchers();

    configureNewPodCreatedOnClusterEvent(newPod);

    final int initialInstances = cluster.getSpec().getInstances();

    kubeDb.introduceReplaceFailures(1, cluster);

    clusterInstanceManager.increaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances + 1, actualInstances);

    verify(clusterScheduler, times(2)).update(any());
  }

  @Test
  void givenADecreasingInstanceFailure_operationShouldBeRetried() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    final Pod newPod = podTestUtil.buildReplicaPod(cluster, 3);

    configureCreationPodWatchers();

    configureNewPodCreatedOnClusterEvent(newPod);

    final int initialInstances = cluster.getSpec().getInstances();

    kubeDb.introduceReplaceFailures(1, cluster);

    clusterInstanceManager.decreaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances - 1, actualInstances);

    verify(clusterScheduler, times(2)).update(any());
  }

  private void configureNonDisruptablePod(int index) {
    Pod primaryPod = podTestUtil.buildNonDisruptablePrimaryPod(cluster, index);
    mockServer.getClient().pods().inNamespace(namespace)
        .resource(primaryPod)
        .update();
  }

  private void configureNewPodCreatedOnClusterEvent(Pod newPod) {
    kubeDb.watchCluster(clusterName, namespace, cluster -> mockServer.getClient().pods()
        .inNamespace(namespace)
        .resource(newPod)
        .create());
  }

  private void configurePodDeletedOnClusterEvent(Pod podToDelete) {
    kubeDb.watchCluster(clusterName, namespace, cluster -> mockServer.getClient().pods()
        .inNamespace(namespace)
        .resource(podToDelete)
        .delete());
  }

  private void configureCreationPodWatchers() {
    when(podWatcher.waitUntilIsReady(eq(clusterName), anyString(), eq(namespace), anyBoolean()))
        .thenAnswer(invocation -> {
          final String podName = invocation.getArgument(1);
          String namespace = invocation.getArgument(2);
          Pod pod = null;
          int retries = 0;
          while (pod == null) {
            if (retries > 10) {
              fail("Pod " + podName + " not created. Available pods "
                  + mockServer.getClient().pods().inNamespace(namespace)
                      .list().getItems()
                      .stream()
                      .map(Pod::getMetadata)
                      .map(ObjectMeta::getName)
                      .collect(Collectors.toUnmodifiableList()));
            }
            try {
              pod = mockServer.getClient().pods().inNamespace(namespace)
                  .withName(podName).get();
            } catch (KubernetesClientException ex) {
              pod = null;
            }
            retries++;
            try {
              Thread.sleep(100);
            } catch (InterruptedException ex) {
              continue;
            }
          }
          return Uni.createFrom().item(pod);
        });
  }

}
