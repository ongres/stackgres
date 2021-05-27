/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@QuarkusTest
class ClusterRestartImplTest {

  private static final String NAMESPACE = "test";
  private static final String CLUSTER_NAME = "test-cluster";
  private static final String IN_PLACE_METHOD = "InPlace";
  private static final String REDUCED_IMPACT_METHOD = "ReducedImpact";

  @Inject
  ClusterRestartImpl clusterRestart;

  @InjectMock
  PodRestartImpl podRestart;

  @InjectMock
  ClusterSwitchoverHandlerImpl switchoverHandler;

  @InjectMock
  ClusterInstanceManagerImpl instanceManager;

  @InjectMock
  ClusterWatcher clusterWatcher;

  Pod primary = new PodBuilder()
      .withNewMetadata()
      .withName(CLUSTER_NAME + "-1")
      .withNamespace(NAMESPACE)
      .addToLabels("role", "master")
      .endMetadata()
      .build();

  Pod replica1 = new PodBuilder()
      .withNewMetadata()
      .withName(CLUSTER_NAME + "-2")
      .withNamespace(NAMESPACE)
      .addToLabels("role", "replica")
      .endMetadata()
      .build();

  Pod replica2 = new PodBuilder()
      .withNewMetadata()
      .withName(CLUSTER_NAME + "-3")
      .withNamespace(NAMESPACE)
      .addToLabels("role", "replica")
      .endMetadata()
      .build();

  Pod additionalPod = new PodBuilder()
      .withNewMetadata()
      .withName(CLUSTER_NAME + "-4")
      .withNamespace(NAMESPACE)
      .addToLabels("role", "replica")
      .endMetadata()
      .build();

  StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    cluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
    cluster.getMetadata().setName(CLUSTER_NAME);
    cluster.getMetadata().setNamespace(NAMESPACE);
    cluster.getSpec().setInstances(3);

    when(clusterWatcher.waitUntilIsReady(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(cluster));

  }

  @Test
  void givenACleanState_itShouldRestartAllPods() {

    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(IN_PLACE_METHOD)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .isSwitchoverInitiated(false)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should perform a switchover");

    final InOrder order = inOrder(podRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica1);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica2);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithARestartedPod_shouldNotRestartThatPod() {

    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(IN_PLACE_METHOD)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances(replica1)
        .isSwitchoverInitiated(false)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should perform a switchover");

    final InOrder order = inOrder(podRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica2);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);

    verify(podRestart, never()).restartPod(replica1);

    checkFinalSgClusterOnInPlace();

  }

  @Test
  void givenAClusterWithAllReplicasRestarted_shouldNotRestartOnlyThePrimaryNode() {

    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(IN_PLACE_METHOD)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances(replica1, replica2)
        .isSwitchoverInitiated(false)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should perform a switchover");

    final InOrder order = inOrder(podRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);

    verify(podRestart, never()).restartPod(replica1);
    verify(podRestart, never()).restartPod(replica2);

    checkFinalSgClusterOnInPlace();

  }

  @Test
  void givenAClusterWithAllReplicasRestartedAndSwitchoverInitiated_shouldNotPerformSwitchover() {

    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(IN_PLACE_METHOD)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances(replica1, replica2)
        .isSwitchoverInitiated(true)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should not perform a switchover");

    final InOrder order = inOrder(podRestart, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);

    verify(podRestart, never()).restartPod(replica1);
    verify(podRestart, never()).restartPod(replica2);

    verify(switchoverHandler, never()).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();

  }

  private void checkFinalSgClusterOnInPlace() {

    verify(instanceManager, never()).increaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    verify(instanceManager, never()).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);

  }

  @Test
  void givenACleanStateWithReduceImpact_itShouldRestartAllPods() {

    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(REDUCED_IMPACT_METHOD)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .isSwitchoverInitiated(false)
        .build();

    when(instanceManager.increaseClusterInstances(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(additionalPod));

    when(instanceManager.decreaseClusterInstances(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should send an event for every pod created");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should perform a switchover");

    final InOrder order = inOrder(podRestart, switchoverHandler, instanceManager);
    order.verify(instanceManager).increaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica1);
    order.verify(podRestart).restartPod(replica2);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(instanceManager).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);

  }

  @Test
  void givenAClusterWithARestartedPodAndReducedImpact_shouldNotRestartThatPod() {

    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(REDUCED_IMPACT_METHOD)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2, additionalPod)
        .addRestartedInstances(replica1, additionalPod)
        .isSwitchoverInitiated(false)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    when(instanceManager.decreaseClusterInstances(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());


    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should perform a switchover");

    final InOrder order = inOrder(podRestart, switchoverHandler, instanceManager, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica2);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(instanceManager).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);

    verify(podRestart, never()).restartPod(replica1);
    verify(instanceManager, never()).increaseClusterInstances(CLUSTER_NAME, NAMESPACE);

  }

  @Test
  void givenAClusterWithAllReplicasRestartedAndSwitchoverInitiatedAndReducedImpact_shouldNotPerformSwitchover() {

    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(REDUCED_IMPACT_METHOD)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2, additionalPod)
        .addRestartedInstances(additionalPod, replica1, replica2)
        .isSwitchoverInitiated(true)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should not perform a switchover");

    verify(podRestart).restartPod(primary);
    verify(instanceManager).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);

    verify(podRestart, never()).restartPod(replica1);
    verify(podRestart, never()).restartPod(replica2);
    verify(switchoverHandler, never()).performSwitchover(any(), any(), any());
    verify(instanceManager, never()).increaseClusterInstances(CLUSTER_NAME, NAMESPACE);

  }

  @Test
  void givenAClusterWithAInstancedDecreasedAndReducedImpact_shouldNotDecreaseInstances() {

    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .clusterName(CLUSTER_NAME)
        .namespace(NAMESPACE)
        .restartMethod(REDUCED_IMPACT_METHOD)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances(replica1, replica2, primary)
        .isSwitchoverInitiated(true)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTART)
            .count(), "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_CREATED)
            .count(), "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.SWITCHOVER)
            .count(), "it should not perform a switchover");


    verify(podRestart, never()).restartPod(primary);
    verify(podRestart, never()).restartPod(replica1);
    verify(podRestart, never()).restartPod(replica2);
    verify(switchoverHandler, never()).performSwitchover(any(), any(), any());
    verify(instanceManager, never()).increaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    verify(instanceManager, never()).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);
  }

}