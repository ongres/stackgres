/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReason;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsOperation;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.fixture.Fixtures;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@QuarkusTest
class ClusterRestartTest {

  private static final String NAMESPACE = "test";
  private static final String DBOPS_NAME = "test-dbops";
  private static final String CLUSTER_NAME = "test-cluster";
  private static final String PRIMARY_POD_NAME = "test-cluster-0";
  private static final String REPLICA_1_POD_NAME = "test-cluster-1";
  private static final String REPLICA_2_POD_NAME = "test-cluster-2";
  private static final String REPLICA_3_POD_NAME = "test-cluster-3";

  @Inject
  ClusterRestart clusterRestart;

  @InjectMock
  PodRestart podRestart;

  @InjectMock
  ClusterSwitchoverHandler switchoverHandler;

  @InjectMock
  ClusterInstanceManager instanceManager;

  @InjectMock
  ClusterWatcher clusterWatcher;

  @InjectMock
  PostgresRestart postgresRestart;

  Pod primary;
  Pod replica1;
  Pod replica2;
  Pod additionalPod;

  StackGresCluster cluster;
  StackGresDbOps dbOps;

  private Pod buildPod(String name, String role) {
    return new PodBuilder()
        .withNewMetadata()
        .withName(name)
        .withNamespace(NAMESPACE)
        .addToLabels(PatroniUtil.ROLE_KEY, role)
        .endMetadata()
        .build();
  }

  @BeforeEach
  void setUp() {
    primary = buildPod(PRIMARY_POD_NAME, PatroniUtil.PRIMARY_ROLE);
    replica1 = buildPod(REPLICA_1_POD_NAME, PatroniUtil.REPLICA_ROLE);
    replica2 = buildPod(REPLICA_2_POD_NAME, PatroniUtil.REPLICA_ROLE);
    additionalPod = buildPod(REPLICA_3_POD_NAME, PatroniUtil.REPLICA_ROLE);

    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().setName(CLUSTER_NAME);
    cluster.getMetadata().setNamespace(NAMESPACE);
    cluster.getSpec().setInstances(3);

    dbOps = Fixtures.dbOps().loadRestart().get();
    dbOps.getMetadata().setName(DBOPS_NAME);
    dbOps.getMetadata().setNamespace(NAMESPACE);
    dbOps.getSpec().setSgCluster(CLUSTER_NAME);

    when(clusterWatcher.waitUntilIsReady(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(cluster));
  }

  @Test
  void givenACleanState_itShouldRestartAllPods() {
    when(clusterWatcher.getAvailablePrimary(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(Optional.of(PRIMARY_POD_NAME)));
    ClusterRestartState clusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(DbOpsMethodType.IN_PLACE)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary.getMetadata().getName())
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(),
            replica1, RestartReasons.of(),
            replica2, RestartReasons.of()))
        .isSwitchoverInitiated(false)
        .isSwitchoverFinalized(false)
        .build();

    when(podRestart.restartPod(any(), any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(1);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(postgresRestart.restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().nullItem());

    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertRestartingPodEventCount(events, clusterState.getTotalInstances().size());
    assertPodRestartedEventCount(events, clusterState.getTotalInstances().size());
    assertIncreasingInstanceEvent(events, false);
    assertInstancesIncreasedEvent(events, false);
    assertDecreasingInstanceEvent(events, false);
    assertInstancesDecreasedEvent(events, false);
    assertSwitchoverInitializedEvent(events, true);
    assertSwitchoverFinalizedEvent(events, true);
    assertRestartingPostgresEvent(events, true);
    assertPostgresRestartedEvent(events, true);
    assertPrimaryAvailableEvent(events, true);

    final InOrder order = inOrder(podRestart, switchoverHandler, clusterWatcher, postgresRestart);
    order.verify(clusterWatcher).getAvailablePrimary(CLUSTER_NAME, NAMESPACE);
    order.verify(postgresRestart).restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(replica1));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(replica2));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(primary));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(5)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(1)).restartPostgres(any(), any(), any());
    verify(podRestart, times(3)).restartPod(any(), any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithoutPrimary_itShouldRestartLatestPrimaryAndAllOtherPods() {
    when(clusterWatcher.getAvailablePrimary(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(Optional.empty()));
    primary.getMetadata().getLabels().remove(PatroniUtil.ROLE_KEY);

    ClusterRestartState clusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(DbOpsMethodType.IN_PLACE)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary.getMetadata().getName())
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(),
            replica1, RestartReasons.of(),
            replica2, RestartReasons.of()))
        .isSwitchoverInitiated(false)
        .isSwitchoverFinalized(false)
        .build();

    when(podRestart.restartPod(any(), any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(1);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(postgresRestart.restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().nullItem());

    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertRestartingPodEventCount(events, clusterState.getTotalInstances().size());
    assertPodRestartedEventCount(events, clusterState.getTotalInstances().size());
    assertIncreasingInstanceEvent(events, false);
    assertInstancesIncreasedEvent(events, false);
    assertDecreasingInstanceEvent(events, false);
    assertInstancesDecreasedEvent(events, false);
    assertSwitchoverInitializedEvent(events, false);
    assertSwitchoverFinalizedEvent(events, false);
    assertRestartingPostgresEvent(events, false);
    assertPostgresRestartedEvent(events, false);
    assertPrimaryAvailableEvent(events, false);

    final InOrder order = inOrder(podRestart, switchoverHandler, clusterWatcher, postgresRestart);
    order.verify(clusterWatcher).getAvailablePrimary(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(primary));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(replica1));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(replica2));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(3)).waitUntilIsReady(any(), any());
    verify(postgresRestart, never()).restartPostgres(any(), any(), any());
    verify(podRestart, times(3)).restartPod(any(), any());
    verify(switchoverHandler, never()).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithChangedPrimary_itShouldFail() {
    when(clusterWatcher.getAvailablePrimary(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(Optional.of(REPLICA_1_POD_NAME)));
    primary.getMetadata().getLabels().remove(PatroniUtil.ROLE_KEY);
    replica1.getMetadata().getLabels().put(PatroniUtil.ROLE_KEY,
        PatroniUtil.PRIMARY_ROLE);

    ClusterRestartState clusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(DbOpsMethodType.IN_PLACE)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary.getMetadata().getName())
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(),
            replica1, RestartReasons.of(),
            replica2, RestartReasons.of()))
        .isSwitchoverInitiated(false)
        .isSwitchoverFinalized(false)
        .build();

    AssertSubscriber<RestartEvent> subscriber = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .withSubscriber(AssertSubscriber.create(2))
        .awaitFailure()
        .assertFailedWith(RuntimeException.class,
            String.format("Primary instance %s changed from %s",
                REPLICA_1_POD_NAME, PRIMARY_POD_NAME));

    List<RestartEvent> events = subscriber.getItems();
    assertRestartingPodEventCount(events, 0);
    assertPodRestartedEventCount(events, 0);
    assertIncreasingInstanceEvent(events, false);
    assertInstancesIncreasedEvent(events, false);
    assertDecreasingInstanceEvent(events, false);
    assertInstancesDecreasedEvent(events, false);
    assertSwitchoverInitializedEvent(events, false);
    assertSwitchoverFinalizedEvent(events, false);
    assertRestartingPostgresEvent(events, false);
    assertPostgresRestartedEvent(events, false);
    assertPrimaryChangedEvent(events);

    final InOrder order = inOrder(podRestart, switchoverHandler, clusterWatcher, postgresRestart);
    order.verify(clusterWatcher).getAvailablePrimary(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, never()).waitUntilIsReady(any(), any());
    verify(postgresRestart, never()).restartPostgres(any(), any(), any());
    verify(podRestart, never()).restartPod(any(), any());
    verify(switchoverHandler, never()).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithARestartedPod_shouldNotRestartThatPod() {
    when(clusterWatcher.getAvailablePrimary(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(Optional.of(PRIMARY_POD_NAME)));
    ClusterRestartState clusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(DbOpsMethodType.IN_PLACE)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary.getMetadata().getName())
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

    when(podRestart.restartPod(any(), any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(1);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertRestartingPodEventCount(events,
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size());
    assertPodRestartedEventCount(events,
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size());
    assertIncreasingInstanceEvent(events, false);
    assertInstancesIncreasedEvent(events, false);
    assertDecreasingInstanceEvent(events, false);
    assertInstancesDecreasedEvent(events, false);
    assertSwitchoverInitializedEvent(events, true);
    assertSwitchoverFinalizedEvent(events, true);
    assertRestartingPostgresEvent(events, false);
    assertPostgresRestartedEvent(events, false);
    assertPrimaryAvailableEvent(events, true);

    final InOrder order = inOrder(podRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).getAvailablePrimary(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(replica2));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(primary));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(3)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(2)).restartPod(any(), any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithAPodInPendingRestartWithOnlyPendingRestart_shouldOnlyRestartThatPod() {
    when(clusterWatcher.getAvailablePrimary(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(Optional.of(PRIMARY_POD_NAME)));
    ClusterRestartState clusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(DbOpsMethodType.IN_PLACE)
        .isOnlyPendingRestart(true)
        .primaryInstance(primary.getMetadata().getName())
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

    when(podRestart.restartPod(any(), any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(1);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertRestartingPodEventCount(events, 1);
    assertPodRestartedEventCount(events, 1);
    assertIncreasingInstanceEvent(events, false);
    assertInstancesIncreasedEvent(events, false);
    assertDecreasingInstanceEvent(events, false);
    assertInstancesDecreasedEvent(events, false);
    assertSwitchoverInitializedEvent(events, false);
    assertSwitchoverFinalizedEvent(events, false);

    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).getAvailablePrimary(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(replica1));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(1)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any(), any());
    verify(switchoverHandler, times(0)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithPrimaryInPendingRestartWithOnlyPendingRestart_shouldOnlyRestartThatPod() {
    when(clusterWatcher.getAvailablePrimary(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(Optional.of(PRIMARY_POD_NAME)));
    ClusterRestartState clusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(DbOpsMethodType.IN_PLACE)
        .isOnlyPendingRestart(true)
        .primaryInstance(primary.getMetadata().getName())
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

    when(podRestart.restartPod(any(), any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(1);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(postgresRestart.restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().nullItem());

    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertRestartingPodEventCount(events, 1);
    assertPodRestartedEventCount(events, 1);
    assertIncreasingInstanceEvent(events, false);
    assertInstancesIncreasedEvent(events, false);
    assertDecreasingInstanceEvent(events, false);
    assertInstancesDecreasedEvent(events, false);
    assertSwitchoverInitializedEvent(events, true);
    assertSwitchoverFinalizedEvent(events, true);

    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).getAvailablePrimary(CLUSTER_NAME, NAMESPACE);
    order.verify(postgresRestart).restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(primary));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(3)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(1)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any(), any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithAllReplicasRestarted_shouldRestartOnlyThePrimaryNode() {
    when(clusterWatcher.getAvailablePrimary(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(Optional.of(PRIMARY_POD_NAME)));
    ClusterRestartState clusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(DbOpsMethodType.IN_PLACE)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary.getMetadata().getName())
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

    when(podRestart.restartPod(any(), any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(1);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertRestartingPodEventCount(events,
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size());
    assertPodRestartedEventCount(events,
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size());
    assertIncreasingInstanceEvent(events, false);
    assertInstancesIncreasedEvent(events, false);
    assertDecreasingInstanceEvent(events, false);
    assertInstancesDecreasedEvent(events, false);
    assertSwitchoverInitializedEvent(events, true);
    assertSwitchoverFinalizedEvent(events, true);
    assertRestartingPostgresEvent(events, false);
    assertPostgresRestartedEvent(events, false);
    assertPrimaryAvailableEvent(events, true);

    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).getAvailablePrimary(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(primary));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(2)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any(), any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  @Test
  void givenAClusterWithAllReplicasRestartedAndSwitchoverInitiated_shouldNotPerformSwitchover() {
    when(clusterWatcher.getAvailablePrimary(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(Optional.of(PRIMARY_POD_NAME)));
    ClusterRestartState clusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(DbOpsMethodType.IN_PLACE)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary.getMetadata().getName())
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

    when(podRestart.restartPod(any(), any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(1);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());
    assertRestartingPodEventCount(events,
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size());
    assertPodRestartedEventCount(events,
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size());
    assertIncreasingInstanceEvent(events, false);
    assertInstancesIncreasedEvent(events, false);
    assertDecreasingInstanceEvent(events, false);
    assertInstancesDecreasedEvent(events, false);
    assertSwitchoverInitializedEvent(events, false);
    assertSwitchoverFinalizedEvent(events, false);
    assertRestartingPostgresEvent(events, false);
    assertPostgresRestartedEvent(events, false);
    assertPrimaryAvailableEvent(events, true);

    final InOrder order = inOrder(podRestart, postgresRestart, switchoverHandler, clusterWatcher);
    order.verify(clusterWatcher).getAvailablePrimary(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(primary));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(1)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any(), any());
    verify(switchoverHandler, times(0)).performSwitchover(any(), any(), any());

    checkFinalSgClusterOnInPlace();
  }

  private void checkFinalSgClusterOnInPlace() {
    verify(instanceManager, never()).increaseClusterInstances(any(), any());
    verify(instanceManager, never()).decreaseClusterInstances(any(), any());
  }

  @Test
  void givenACleanStateWithReduceImpact_itShouldRestartAllPods() {
    when(clusterWatcher.getAvailablePrimary(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(Optional.of(PRIMARY_POD_NAME)));
    ClusterRestartState clusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(DbOpsMethodType.REDUCED_IMPACT)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary.getMetadata().getName())
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

    when(podRestart.restartPod(any(), any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(1);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();

    when(postgresRestart.restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().nullItem());

    when(switchoverHandler.performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().voidItem());

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertRestartingPodEventCount(events, clusterState.getTotalInstances().size());
    assertPodRestartedEventCount(events, clusterState.getTotalInstances().size());
    assertIncreasingInstanceEvent(events, true);
    assertInstancesIncreasedEvent(events, true);
    assertDecreasingInstanceEvent(events, true);
    assertInstancesDecreasedEvent(events, true);
    assertSwitchoverInitializedEvent(events, true);
    assertSwitchoverFinalizedEvent(events, true);
    assertRestartingPostgresEvent(events, true);
    assertPostgresRestartedEvent(events, true);
    assertPrimaryAvailableEvent(events, true);

    final InOrder order =
        inOrder(podRestart, switchoverHandler, instanceManager, postgresRestart, clusterWatcher);
    order.verify(clusterWatcher).getAvailablePrimary(CLUSTER_NAME, NAMESPACE);
    order.verify(postgresRestart).restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(instanceManager).increaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(replica1));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(replica2));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(primary));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(instanceManager).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(6)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(1)).restartPostgres(any(), any(), any());
    verify(podRestart, times(3)).restartPod(any(), any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());
    verify(instanceManager, times(1)).increaseClusterInstances(any(), any());
    verify(instanceManager, times(1)).decreaseClusterInstances(any(), any());
  }

  @Test
  void givenAClusterWithARestartedPodAndReducedImpact_shouldNotRestartThatPod() {
    when(clusterWatcher.getAvailablePrimary(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(Optional.of(PRIMARY_POD_NAME)));
    ClusterRestartState clusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(DbOpsMethodType.REDUCED_IMPACT)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary.getMetadata().getName())
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

    when(podRestart.restartPod(any(), any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(1);
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

    assertRestartingPodEventCount(events,
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size());
    assertPodRestartedEventCount(events,
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size());
    assertIncreasingInstanceEvent(events, false);
    assertInstancesIncreasedEvent(events, false);
    assertDecreasingInstanceEvent(events, true);
    assertInstancesDecreasedEvent(events, true);
    assertSwitchoverInitializedEvent(events, true);
    assertSwitchoverFinalizedEvent(events, true);
    assertRestartingPostgresEvent(events, false);
    assertPostgresRestartedEvent(events, false);
    assertPrimaryAvailableEvent(events, true);

    final InOrder order =
        inOrder(podRestart, postgresRestart, switchoverHandler, instanceManager, clusterWatcher);
    order.verify(clusterWatcher).getAvailablePrimary(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(replica2));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(switchoverHandler).performSwitchover(primaryName, CLUSTER_NAME, NAMESPACE);
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(primary));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(instanceManager).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(3)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(2)).restartPod(any(), any());
    verify(switchoverHandler, times(1)).performSwitchover(any(), any(), any());
    verify(instanceManager, times(0)).increaseClusterInstances(any(), any());
    verify(instanceManager, times(1)).decreaseClusterInstances(any(), any());
  }

  @Test
  @DisplayName("Given Cluster With All Replicas Restarted And Switchover Initiated And Reduced "
      + "Impact Should Not Perform Switchover")
  void givenClusterReplicasRestartedAndSwitchoverReducedImpact_shouldNotPerformSwitchover() {
    when(clusterWatcher.getAvailablePrimary(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(Optional.of(PRIMARY_POD_NAME)));
    ClusterRestartState clusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(DbOpsMethodType.REDUCED_IMPACT)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary.getMetadata().getName())
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

    when(podRestart.restartPod(any(), any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(1);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertRestartingPodEventCount(events,
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size());
    assertPodRestartedEventCount(events,
        clusterState.getTotalInstances().size() - clusterState.getRestartedInstances().size());
    assertIncreasingInstanceEvent(events, false);
    assertInstancesIncreasedEvent(events, false);
    assertDecreasingInstanceEvent(events, true);
    assertInstancesDecreasedEvent(events, true);
    assertSwitchoverInitializedEvent(events, false);
    assertSwitchoverFinalizedEvent(events, false);
    assertRestartingPostgresEvent(events, false);
    assertPostgresRestartedEvent(events, false);
    assertPrimaryAvailableEvent(events, true);

    final InOrder order =
        inOrder(podRestart, postgresRestart, switchoverHandler, instanceManager, clusterWatcher);
    order.verify(clusterWatcher).getAvailablePrimary(CLUSTER_NAME, NAMESPACE);
    order.verify(podRestart).restartPod(any(), eq(primary));
    order.verify(clusterWatcher).waitUntilIsReady(CLUSTER_NAME, NAMESPACE);
    order.verify(instanceManager).decreaseClusterInstances(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(clusterWatcher, times(1)).waitUntilIsReady(any(), any());
    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(1)).restartPod(any(), any());
    verify(switchoverHandler, times(0)).performSwitchover(any(), any(), any());
    verify(instanceManager, times(0)).increaseClusterInstances(any(), any());
    verify(instanceManager, times(1)).decreaseClusterInstances(any(), any());
  }

  @Test
  void givenAClusterWithAInstancedDecreasedAndReducedImpact_shouldNotDecreaseInstances() {
    when(clusterWatcher.getAvailablePrimary(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(Optional.of(PRIMARY_POD_NAME)));
    ClusterRestartState clusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(DbOpsMethodType.REDUCED_IMPACT)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary.getMetadata().getName())
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

    when(podRestart.restartPod(any(), any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(1);
      return Uni.createFrom().item(pod);
    });

    List<RestartEvent> events = clusterRestart.restartCluster(clusterState)
        .subscribe()
        .asStream()
        .collect(Collectors.toUnmodifiableList());

    assertRestartingPodEventCount(events, 0);
    assertPodRestartedEventCount(events, 0);
    assertIncreasingInstanceEvent(events, false);
    assertInstancesIncreasedEvent(events, false);
    assertDecreasingInstanceEvent(events, false);
    assertInstancesDecreasedEvent(events, false);
    assertSwitchoverInitializedEvent(events, false);
    assertSwitchoverFinalizedEvent(events, false);
    assertRestartingPostgresEvent(events, false);
    assertPostgresRestartedEvent(events, false);
    assertPrimaryAvailableEvent(events, true);

    final InOrder order =
        inOrder(clusterWatcher, podRestart, postgresRestart, switchoverHandler, instanceManager,
            clusterWatcher);
    order.verify(clusterWatcher).getAvailablePrimary(CLUSTER_NAME, NAMESPACE);
    order.verifyNoMoreInteractions();

    verify(postgresRestart, times(0)).restartPostgres(any(), any(), any());
    verify(podRestart, times(0)).restartPod(any(), any());
    verify(switchoverHandler, times(0)).performSwitchover(any(), any(), any());
    verify(instanceManager, times(0)).increaseClusterInstances(any(), any());
    verify(instanceManager, times(0)).decreaseClusterInstances(any(), any());
  }

  @Test()
  void givenAFailureOnPostgreRestart_itShouldSetStatusAsFailedPostgresRestart() {
    when(clusterWatcher.getAvailablePrimary(CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom().item(Optional.of(PRIMARY_POD_NAME)));
    ClusterRestartState clusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .restartMethod(DbOpsMethodType.IN_PLACE)
        .isOnlyPendingRestart(false)
        .primaryInstance(primary.getMetadata().getName())
        .addInitialInstances(primary, replica1, replica2)
        .addTotalInstances(primary, replica1, replica2)
        .putAllPodRestartReasonsMap(ImmutableMap.of(
            primary, RestartReasons.of(),
            replica1, RestartReasons.of(),
            replica2, RestartReasons.of()))
        .isSwitchoverInitiated(false)
        .isSwitchoverFinalized(false)
        .build();

    when(podRestart.restartPod(any(), any(Pod.class))).thenAnswer(invocationOnMock -> {
      Pod pod = invocationOnMock.getArgument(1);
      return Uni.createFrom().item(pod);
    });

    final String primaryName = primary.getMetadata().getName();
    when(postgresRestart.restartPostgres(primaryName, CLUSTER_NAME, NAMESPACE))
        .thenReturn(Uni.createFrom()
            .failure(new RuntimeException("woops!")));

    var failure = assertThrows(FailedRestartPostgresException.class,
        () -> clusterRestart.restartCluster(clusterState)
            .subscribe()
            .asStream()
            .count());

    assertEquals(String.format("Restart of primary instance in Pod %s failed", primaryName),
        failure.getMessage());
    assertEquals("woops!",
        failure.getCause().getMessage());
  }

  private void assertPodRestartedEventCount(List<RestartEvent> events, int times) {
    assertEquals(times,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POD_RESTARTED)
            .count(),
        "it should " + (times > 0 ? " " : "not ") + "send an event for every pod restart:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
  }

  private void assertRestartingPodEventCount(List<RestartEvent> events, int times) {
    assertEquals(times,
        events.stream().filter(event -> event.getEventType() == RestartEventType.RESTARTING_POD)
            .count(),
        "it should " + (times > 0 ? " " : "not ") + "send an event for every pod restart:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
  }

  private void assertSwitchoverFinalizedEvent(List<RestartEvent> events, boolean expected) {
    assertEquals(expected ? 1 : 0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_FINALIZED)
            .count(),
        "it should " + (expected ? " " : "not ") + "finalize a switchover:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
  }

  private void assertSwitchoverInitializedEvent(List<RestartEvent> events, boolean expected) {
    assertEquals(expected ? 1 : 0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.SWITCHOVER_INITIATED)
            .count(),
        "it should " + (expected ? " " : "not ") + "initiate a switchover:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
  }

  private void assertInstancesDecreasedEvent(List<RestartEvent> events, boolean expected) {
    assertEquals(expected ? 1 : 0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.INSTANCES_DECREASED)
            .count(),
        "it should " + (expected ? " " : "not ") + "delete a pod in InPlace restart:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
  }

  private void assertDecreasingInstanceEvent(List<RestartEvent> events, boolean expected) {
    assertEquals(expected ? 1 : 0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.DECREASING_INSTANCES)
            .count(),
        "it should " + (expected ? " " : "not ") + "delete a pod in InPlace restart:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
  }

  private void assertInstancesIncreasedEvent(List<RestartEvent> events, boolean expected) {
    assertEquals(expected ? 1 : 0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.INSTANCES_INCREASED)
            .count(),
        "it should " + (expected ? " " : "not ") + "create a pod in InPlace restart:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
  }

  private void assertIncreasingInstanceEvent(List<RestartEvent> events, boolean expected) {
    assertEquals(expected ? 1 : 0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.INCREASING_INSTANCES)
            .count(),
        "it should " + (expected ? " " : "not ") + "create a pod in InPlace restart:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
  }

  private void assertPostgresRestartedEvent(List<RestartEvent> events, boolean expected) {
    assertEquals(expected ? 1 : 0,
        events.stream().filter(event -> event.getEventType() == RestartEventType.POSTGRES_RESTARTED)
            .count(),
        "it should " + (expected ? " " : "not ") + "restart the primary postgres:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
  }

  private void assertRestartingPostgresEvent(List<RestartEvent> events, boolean expected) {
    assertEquals(expected ? 1 : 0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.RESTARTING_POSTGRES)
            .count(),
        "it should " + (expected ? " " : "not ")
        + "notify that the primary postgres has been restarted:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
  }

  private void assertPrimaryAvailableEvent(List<RestartEvent> events, boolean expected) {
    assertEquals(expected ? 1 : 0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.PRIMARY_AVAILABLE)
            .count(),
        "it should " + (expected ? " " : "not ") + "detect primary as available:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
    assertEquals(expected ? 0 : 1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.PRIMARY_NOT_AVAILABLE)
            .count(),
        "it should " + (!expected ? " " : "not ") + "detect primary as unavailable:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
    assertEquals(0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.PRIMARY_CHANGED)
            .count(),
        "it should not detect primary as changed: "
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
  }

  private void assertPrimaryChangedEvent(List<RestartEvent> events) {
    assertEquals(1,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.PRIMARY_CHANGED)
            .count(),
        "it should detect primary as changed:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
    assertEquals(0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.PRIMARY_AVAILABLE)
            .count(),
        "it should not detect primary as available:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
    assertEquals(0,
        events.stream()
            .filter(event -> event.getEventType() == RestartEventType.PRIMARY_NOT_AVAILABLE)
            .count(),
        "it should not detect primary as unavailable:\n"
            + events.stream().map(RestartEvent::getEventType)
            .map(Object::toString).collect(Collectors.joining("\n")));
  }

}
