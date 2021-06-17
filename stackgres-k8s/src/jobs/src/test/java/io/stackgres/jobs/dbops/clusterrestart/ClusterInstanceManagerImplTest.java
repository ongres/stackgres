/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.stream.Collectors;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.jobs.app.KubernetesClientProvider;
import io.stackgres.jobs.dbops.lock.FakeClusterScheduler;
import io.stackgres.jobs.dbops.lock.MockKubeDb;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@WithKubernetesTestServer
@QuarkusTest
class ClusterInstanceManagerImplTest {

  @Inject
  MockKubeDb kubeDb;

  @Inject
  KubernetesClientProvider clientFactory;

  @InjectMock
  PodWatcher podWatcher;

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

    cluster = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);
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

    configureNewPodCreated(newPod);

    final int initialInstances = cluster.getSpec().getInstances();

    Pod createdPod = clusterInstanceManager.increaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    PodTestUtil.assertPodEquals(newPod, createdPod);

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances + 1, actualInstances);

    verify(podWatcher).waitUntilIsReady(newPodName, namespace);

  }

  @Test
  void givenAClusterWithANonDisruptablePod_increaseInstancesShouldNotFail() {
    podTestUtil.preparePods(cluster, 0, 1, 2);
    configureNonDisruptablePod(0);
    final Pod newPod = podTestUtil.buildReplicaPod(cluster, 3);
    final int initialInstances = cluster.getSpec().getInstances();
    final String newPodName = newPod.getMetadata().getName();

    configureCreationPodWatchers();
    configureNewPodCreated(newPod);

    Pod createdPod = clusterInstanceManager.increaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    PodTestUtil.assertPodEquals(newPod, createdPod);

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances + 1, actualInstances);

    InOrder order = inOrder(podWatcher);

    order.verify(podWatcher).waitUntilIsReady(newPodName, namespace);

  }

  @Test
  void givenAClusterWithAFarDisruptablePod_itShouldWaitForTheRightPodToBeCreated() {

    podTestUtil.preparePods(cluster, 5, 0, 1);
    configureNonDisruptablePod(5);
    final Pod newPod = podTestUtil.buildReplicaPod(cluster, 2);
    final int initialInstances = cluster.getSpec().getInstances();
    final String newPodName = newPod.getMetadata().getName();

    configureCreationPodWatchers();
    configureNewPodCreated(newPod);

    Pod createdPod = clusterInstanceManager.increaseClusterInstances(clusterName, namespace)
        .await().indefinitely();

    PodTestUtil.assertPodEquals(newPod, createdPod);

    final int actualInstances = kubeDb.getCluster(clusterName, namespace).getSpec().getInstances();
    assertEquals(
        initialInstances + 1, actualInstances);

    verify(podWatcher).waitUntilIsReady(newPodName, namespace);
  }

  @Test
  void givenACleanCluster_itShouldWaitForTheRightPodToBeDeleted() {

    podTestUtil.preparePods(cluster, 0, 1, 2);

    Pod replicaToDelete = podTestUtil.buildReplicaPod(cluster, 2);

    final int initialInstances = cluster.getSpec().getInstances();

    String podName = replicaToDelete.getMetadata().getName();

    when(podWatcher.waitUntilIsRemoved(podName, namespace))
        .thenReturn(Uni.createFrom().voidItem());

    configurePodDeleted(replicaToDelete);

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

    configurePodDeleted(replicaToDelete);

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

    configurePodDeleted(replicaToDelete);

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

    configurePodDeleted(podToDelete);

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

    configurePodDeleted(podToDelete);

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

    configureNewPodCreated(newPod);

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

    configureNewPodCreated(newPod);

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
    clientFactory.withNewClient(client -> client.pods().inNamespace(namespace)
        .replace(primaryPod));
  }

  private void configureNewPodCreated(Pod newPod) {
    kubeDb.watchCluster(clusterName, namespace, cluster -> clientFactory
        .withNewClient(client -> client.pods()
            .inNamespace(namespace)
            .createOrReplace(newPod)));

  }

  private void configurePodDeleted(Pod podToDelete) {
    kubeDb.watchCluster(clusterName, namespace, cluster -> clientFactory
        .withNewClient(client -> client.pods()
            .inNamespace(namespace)
            .delete(podToDelete)));

  }

  private void configureCreationPodWatchers() {

    when(podWatcher.waitUntilIsReady(anyString(), eq(namespace))).thenAnswer(invocation -> {
      final String podName = invocation.getArgument(0);
      String namespace = invocation.getArgument(1);
      Pod pod = clientFactory.withNewClient(client -> client.pods().inNamespace(namespace)
          .withName(podName).get());
      int retries = 0;
      while (pod == null) {
        Thread.sleep(100);
        pod = clientFactory.withNewClient(client -> client.pods().inNamespace(namespace)
            .withName(podName).get());
        retries++;
        if (retries > 10) {
          fail("Pod " + podName + " not created available pods " + clientFactory
              .withNewClient(client -> client.pods().inNamespace(namespace)
                  .list().getItems()
                  .stream()
                  .map(Pod::getMetadata)
                  .map(ObjectMeta::getName)).collect(Collectors.toUnmodifiableList()));
        }
      }
      return Uni.createFrom().item(pod);

    });

  }

}