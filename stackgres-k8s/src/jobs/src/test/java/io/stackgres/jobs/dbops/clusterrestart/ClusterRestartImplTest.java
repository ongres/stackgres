/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReason;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@QuarkusTest
class ClusterRestartImplTest {

  private static final String NAMESPACE = "test";
  private static final String DBOPS_NAME = "test-dbops";
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

  @InjectMock
  PostgresRestart postgresRestart;

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
  StackGresDbOps dbOps;

  @BeforeEach
  void setUp() {
    cluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
    cluster.getMetadata().setName(CLUSTER_NAME);
    cluster.getMetadata().setNamespace(NAMESPACE);
    cluster.getSpec().setInstances(3);

    dbOps = JsonUtil
        .readFromJson("stackgres_dbops/dbops_restart.json", StackGresDbOps.class);
    dbOps.getMetadata().setName(DBOPS_NAME);
    dbOps.getMetadata().setNamespace(NAMESPACE);
    dbOps.getSpec().setSgCluster(CLUSTER_NAME);

    when(clusterWatcher.waitUntilIsReady(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(cluster));
  }

  @Test
  void givenACleanState_itShouldRestartAllPods() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(dbOps.getSpec().getOp())
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(IN_PLACE_METHOD)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(),
            replica1, RestartReasons.of(),
            replica2, RestartReasons.of()))
        .isSwitchoverInitiated(false)
        .isSwitchoverFinalized(false)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(postgresRestart.restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(true));

    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.RESTARTING_POD)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(clusterState.getTotalInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTARTED)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INCREASING_INSTANCES)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_INCREASED)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.DECREASING_INSTANCES)
            .count(),
        "it should not delete a pod in InPlace restart");
    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_DECREASED)
            .count(),
        "it should not delete a pod in InPlace restart");

    assertEquals(1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_INITIATED)
            .count(),
        "it should initiate a switchover");

    assertEquals(1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_FINALIZED)
            .count(),
        "it should finalize a switchover");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.RESTARTING_POSTGRES)
            .count(),
        "it should notify that the primary postgres its been restarted");
    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTARTED)
            .count(),
        "it should restart the primary postgres");

    final InOrder order = inOrder(podRestart, switchoverHandler, clusterWatcher, postgresRestart);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(postgresRestart).restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica1);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica2);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(6)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(1)).restartPostgres(any(), any(), any());
    verify(podRestart, times(3)).restartPod(any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithARestartedPod_shouldNotRestartThatPod() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(dbOps.getSpec().getOp())
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(IN_PLACE_METHOD)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances(replica1)
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(),
            replica1, RestartReasons.of(),
            replica2, RestartReasons.of()))
        .isSwitchoverInitiated(false)
        .isSwitchoverFinalized(false)
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

    assertEquals(
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTARTED)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.RESTARTING_POD)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INCREASING_INSTANCES)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_INCREASED)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.DECREASING_INSTANCES)
            .count(),
        "it should not delete a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_DECREASED)
            .count(),
        "it should not delete a pod in InPlace restart");

    assertEquals(1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_INITIATED)
            .count(),
        "it should initiate a switchover");

    assertEquals(1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_FINALIZED)
            .count(),
        "it should finalize a switchover");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.RESTARTING_POSTGRES)
            .count(),
        "it shouldn't restart the primary postgres");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTARTED)
            .count(),
        "it shouldn't restart the primary postgres");

    final InOrder order = inOrder(podRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica2);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(4)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(2)).restartPod(any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithAPodInPendingRestartWithOnlyPendingRestart_shouldOnlyRestartThatPod() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(dbOps.getSpec().getOp())
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(IN_PLACE_METHOD)
        .isOnlyPendingRestart(true)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances()
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(),
            replica1, RestartReasons.of(RestartReason.PATRONI),
            replica2, RestartReasons.of()))
        .isSwitchoverInitiated(false)
        .isSwitchoverFinalized(false)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTARTED)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.RESTARTING_POD)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INCREASING_INSTANCES)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_INCREASED)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.DECREASING_INSTANCES)
            .count(),
        "it should not delete a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_DECREASED)
            .count(),
        "it should not delete a pod in InPlace restart");

    assertEquals(0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_INITIATED)
            .count(),
        "it should not initiate a switchover");

    assertEquals(0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_FINALIZED)
            .count(),
        "it should not finalize a switchover");

    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica1);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(2)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any());
    verify(switchoverHandler, times(0)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithPrimaryInPendingRestartWithOnlyPendingRestart_shouldOnlyRestartThatPod() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(dbOps.getSpec().getOp())
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(IN_PLACE_METHOD)
        .isOnlyPendingRestart(true)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances()
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(RestartReason.PATRONI),
            replica1, RestartReasons.of(),
            replica2, RestartReasons.of()))
        .isSwitchoverInitiated(false)
        .isSwitchoverFinalized(false)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(postgresRestart.restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(true));

    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.RESTARTING_POD)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTARTED)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INCREASING_INSTANCES)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_INCREASED)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.DECREASING_INSTANCES)
            .count(),
        "it should not delete a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_DECREASED)
            .count(),
        "it should not delete a pod in InPlace restart");

    assertEquals(1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_INITIATED)
            .count(),
        "it should initiate a switchover");

    assertEquals(1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_FINALIZED)
            .count(),
        "it should finalize a switchover");

    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(postgresRestart).restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(4)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(1)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithAllReplicasRestarted_shouldRestartOnlyThePrimaryNode() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(dbOps.getSpec().getOp())
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(IN_PLACE_METHOD)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances(replica1, replica2)
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(),
            replica1, RestartReasons.of(),
            replica2, RestartReasons.of()))
        .isSwitchoverInitiated(false)
        .isSwitchoverFinalized(false)
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

    assertEquals(
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTARTED)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.RESTARTING_POD)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INCREASING_INSTANCES)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_INCREASED)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_DECREASED)
            .count(),
        "it should not delete a pod in InPlace restart");

    assertEquals(1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_INITIATED)
            .count(),
        "it should initiate a switchover");

    assertEquals(1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_FINALIZED)
            .count(),
        "it should finalize a switchover");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.RESTARTING_POSTGRES)
            .count(),
        "it shouldn't restart the primary postgres");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTARTED)
            .count(),
        "it shouldn't restart the primary postgres");

    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(3)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithAllReplicasRestartedAndSwitchoverInitiated_shouldNotPerformSwitchover() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(dbOps.getSpec().getOp())
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(IN_PLACE_METHOD)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances(replica1, replica2)
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(),
            replica1, RestartReasons.of(),
            replica2, RestartReasons.of()))
        .isSwitchoverInitiated(true)
        .isSwitchoverFinalized(true)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.RESTARTING_POD)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTARTED)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INCREASING_INSTANCES)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_INCREASED)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.DECREASING_INSTANCES)
            .count(),
        "it should not delete a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_DECREASED)
            .count(),
        "it should not delete a pod in InPlace restart");

    assertEquals(0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_INITIATED)
            .count(),
        "it should not initiate a switchover");

    assertEquals(0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_FINALIZED)
            .count(),
        "it should not finalize a switchover");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.RESTARTING_POSTGRES)
            .count(),
        "it shouldn't restart the primary postgres");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTARTED)
            .count(),
        "it shouldn't restart the primary postgres");

    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(2)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any());
    verify(switchoverHandler, times(0)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  private void checkFinalSgClusterOnInPlace() {
    verify(instanceManager, never()).increaseClusterInstances(any(), any());
    verify(instanceManager, never()).decreaseClusterInstances(any(), any());
  }

  @Test
  void givenACleanStateWithReduceImpact_itShouldRestartAllPods() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(dbOps.getSpec().getOp())
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(REDUCED_IMPACT_METHOD)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(),
            replica1, RestartReasons.of(),
            replica2, RestartReasons.of()))
        .isSwitchoverInitiated(false)
        .isSwitchoverFinalized(false)
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

    when(postgresRestart.restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(true));

    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(clusterState.getTotalInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.RESTARTING_POD)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(clusterState.getTotalInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTARTED)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INCREASING_INSTANCES)
            .count(),
        "it should send an event for every pod created");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_INCREASED)
            .count(),
        "it should send an event for every pod created");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.DECREASING_INSTANCES)
            .count(),
        "it should send an event for every pod deleted");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_DECREASED)
            .count(),
        "it should send an event for every pod deleted");

    assertEquals(1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_INITIATED)
            .count(),
        "it should initiate a switchover");

    assertEquals(1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_FINALIZED)
            .count(),
        "it should finalize a switchover");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.RESTARTING_POSTGRES)
            .count(),
        "it should restart the primary postgres");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTARTED)
            .count(),
        "it should restart the primary postgres");

    final InOrder order =
        inOrder(podRestart, switchoverHandler, instanceManager, postgresRestart, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(postgresRestart).restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(instanceManager).increaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica1);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica2);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(instanceManager).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(7)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(1)).restartPostgres(any(), any(), any());
    verify(podRestart, times(3)).restartPod(any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());
    verify(instanceManager, times(1)).increaseClusterInstances(any(), any());
    verify(instanceManager, times(1)).decreaseClusterInstances(any(), any());
  }

  @Test
  void givenAClusterWithARestartedPodAndReducedImpact_shouldNotRestartThatPod() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(dbOps.getSpec().getOp())
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(REDUCED_IMPACT_METHOD)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2, additionalPod)
        .addRestartedInstances(replica1, additionalPod)
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(),
            replica1, RestartReasons.of(),
            replica2, RestartReasons.of(),
            additionalPod, RestartReasons.of()))
        .isSwitchoverInitiated(false)
        .isSwitchoverFinalized(false)
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

    assertEquals(
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.RESTARTING_POD)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTARTED)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INCREASING_INSTANCES)
            .count(),
        "it should not create a pod if the pod is already created");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_INCREASED)
            .count(),
        "it should not create a pod if the pod is already created");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.DECREASING_INSTANCES)
            .count(),
        "it should send an event for every pod deletion");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_DECREASED)
            .count(),
        "it should send an event for every pod deletion");

    assertEquals(1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_INITIATED)
            .count(),
        "it should initiate a switchover");

    assertEquals(1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_FINALIZED)
            .count(),
        "it should finalize a switchover");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.RESTARTING_POSTGRES)
            .count(),
        "it shouldn't restart the primary postgres");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTARTED)
            .count(),
        "it shouldn't restart the primary postgres");

    final InOrder order =
        inOrder(podRestart, postgresRestart, switchoverHandler, instanceManager, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(replica2);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(instanceManager).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(4)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(2)).restartPod(any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());
    verify(instanceManager, times(0)).increaseClusterInstances(any(), any());
    verify(instanceManager, times(1)).decreaseClusterInstances(any(), any());
  }

  @Test
  @DisplayName("Given Cluster With All Replicas Restarted And Switchover Initiated And Reduced "
      + "Impact Should Not Perform Switchover")
  void givenClusterReplicasRestartedAndSwitchoverReducedImpact_shouldNotPerformSwitchover() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(dbOps.getSpec().getOp())
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(REDUCED_IMPACT_METHOD)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2, additionalPod)
        .addRestartedInstances(additionalPod, replica1, replica2)
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(),
            replica1, RestartReasons.of(),
            replica2, RestartReasons.of(),
            additionalPod, RestartReasons.of()))
        .isSwitchoverInitiated(true)
        .isSwitchoverFinalized(true)
        .build();

    when(podRestart.restartPod(any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(0);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertEquals(
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.RESTARTING_POD)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size(),
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTARTED)
            .count(),
        "it should send an event for every pod restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INCREASING_INSTANCES)
            .count(),
        "it should send an event for every pod creation");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_INCREASED)
            .count(),
        "it should send an event for every pod creation");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.DECREASING_INSTANCES)
            .count(),
        "it should send an event for every pod deleted");

    assertEquals(1,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_DECREASED)
            .count(),
        "it should send an event for every pod deleted");

    assertEquals(0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_INITIATED)
            .count(),
        "it should not initiate a switchover");

    assertEquals(0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_FINALIZED)
            .count(),
        "it should not finalize a switchover");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.RESTARTING_POSTGRES)
            .count(),
        "it shouldn't restart the primary postgres");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTARTED)
            .count(),
        "it shouldn't restart the primary postgres");

    final InOrder order =
        inOrder(podRestart, postgresRestart, switchoverHandler, instanceManager, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(primary);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(instanceManager).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(2)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any());
    verify(switchoverHandler, times(0)).performSwitchover(any(), any(), any());
    verify(instanceManager, times(0)).increaseClusterInstances(any(), any());
    verify(instanceManager, times(1)).decreaseClusterInstances(any(), any());
  }

  @Test
  void givenAClusterWithAInstancedDecreasedAndReducedImpact_shouldNotDecreaseInstances() {
    ClusterRestartState clusterState = ImmutableClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(dbOps.getSpec().getOp())
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(REDUCED_IMPACT_METHOD)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary)
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .addRestartedInstances(replica1, replica2, primary)
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(),
            replica1, RestartReasons.of(),
            replica2, RestartReasons.of()))
        .isSwitchoverInitiated(true)
        .isSwitchoverFinalized(true)
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
        events.stream().filter(event -> event.getEventType() == RestartEventType.RESTARTING_POD)
            .count(),
        "it should not send an event if all pods has been restarted");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTARTED)
            .count(),
        "it should not send an event if all pods has been restarted");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INCREASING_INSTANCES)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.INSTANCES_INCREASED)
            .count(),
        "it should not create a pod in InPlace restart");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTARTED)
            .count(),
        "it should not send an pod deleted event if instances has already been decreased");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.RESTARTING_POD)
            .count(),
        "it should not send an pod deleted event if instances has already been decreased");

    assertEquals(0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_INITIATED)
            .count(),
        "it should not initiate a switchover");

    assertEquals(0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_FINALIZED)
            .count(),
        "it should not finalize a switchover");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType()
                == RestartEventType.RESTARTING_POSTGRES)
            .count(),
        "it shouldn't restart the primary postgres");

    assertEquals(0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTARTED)
            .count(),
        "it shouldn't restart the primary postgres");

    final InOrder order =
        inOrder(podRestart, postgresRestart, switchoverHandler, instanceManager, clusterWatcher);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(1)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(0)).restartPod(any());
    verify(switchoverHandler, times(0)).performSwitchover(any(), any(), any());
    verify(instanceManager, times(0)).increaseClusterInstances(any(), any());
    verify(instanceManager, times(0)).decreaseClusterInstances(any(), any());
  }

}
