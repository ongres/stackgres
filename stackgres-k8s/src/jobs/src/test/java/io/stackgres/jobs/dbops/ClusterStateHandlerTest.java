/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops;

import static io.stackgres.jobs.dbops.clusterrestart.PodTestUtil.assertPodEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.InjectMock;
import io.smallrye.mutiny.Multi;
import io.stackgres.common.ClusterRolloutUtil.RestartReasons;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsMethodType;
import io.stackgres.common.crd.sgdbops.DbOpsOperation;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.event.DbOpsEventEmitter;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.patroni.PatroniCtlInstance;
import io.stackgres.common.patroni.PatroniHistoryEntry;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestart;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.ImmutableRestartEventForTest;
import io.stackgres.jobs.dbops.clusterrestart.InvalidClusterException;
import io.stackgres.jobs.dbops.clusterrestart.PatroniCtlFinder;
import io.stackgres.jobs.dbops.clusterrestart.PodTestUtil;
import io.stackgres.jobs.dbops.clusterrestart.RestartEventType;
import io.stackgres.jobs.dbops.mock.MockKubeDbTest;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.apache.commons.compress.utils.Lists;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public abstract class ClusterStateHandlerTest extends MockKubeDbTest {

  @InjectMock
  public ClusterRestart clusterRestart;

  @InjectMock
  PatroniCtlFinder patroniCtlFinder;

  protected PatroniCtlInstance patroniCtl = Mockito.mock(PatroniCtlInstance.class);

  @Inject
  public PodTestUtil podTestUtil;

  @InjectMock
  public DbOpsEventEmitter eventEmitter;

  public String namespace = StringUtils.getRandomNamespace();

  public String dbOpsName = StringUtils.getRandomResourceName();

  public String clusterName = StringUtils.getRandomResourceName();

  public StackGresDbOps dbOps;

  public StackGresCluster cluster;

  protected static void assertEqualsRestartState(
      ClusterRestartState expected,
      ClusterRestartState actual) {
    assertEquals(expected.getClusterName(), actual.getClusterName());
    assertEquals(expected.getNamespace(), actual.getNamespace());

    assertEquals(expected.getPrimaryInstance(), actual.getPrimaryInstance());

    var expectedInitialInstances = expected.getInitialInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .toList();
    var actualInitialInstances = actual.getInitialInstances().stream()
        .sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .toList();

    Seq.zip(expectedInitialInstances, actualInitialInstances)
        .forEach(tuple -> assertPodEquals(tuple.v1, tuple.v2));

    final List<Pod> restartedInstances = expected.getRestartedInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .toList();

    final List<Pod> actualRestartedInstances = actual.getRestartedInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .toList();

    Seq.zip(restartedInstances, actualRestartedInstances)
        .forEach(tuple -> assertPodEquals(tuple.v1, tuple.v2));

    final List<Pod> expectedTotalInstances = expected.getTotalInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .toList();
    final List<Pod> actualTotalInstances = actual.getTotalInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .toList();

    Seq.zip(expectedTotalInstances, actualTotalInstances)
        .forEach(tuple -> assertPodEquals(tuple.v1, tuple.v2));
  }

  @BeforeEach
  public void setUp() {
    namespace = StringUtils.getRandomNamespace();
    clusterName = StringUtils.getRandomResourceName();

    dbOps = getDbOps();

    cluster = Fixtures.cluster().loadDefault().get();

    dbOps.getMetadata().setName(dbOpsName);
    dbOps.getMetadata().setNamespace(namespace);
    dbOps.getSpec().setSgCluster(clusterName);

    cluster.getMetadata().setName(clusterName);
    cluster.getMetadata().setNamespace(namespace);

    cluster = kubeDb.addOrReplaceCluster(cluster);
    dbOps = kubeDb.addOrReplaceDbOps(dbOps);

    lenient().doNothing().when(eventEmitter).sendEvent(any(), any(), any());
    lenient().when(patroniCtlFinder.findPatroniCtl(any(), any())).thenReturn(patroniCtl);
    lenient().when(patroniCtlFinder.getSuperuserCredentials(any(), any()))
        .thenReturn(Tuple.tuple("test", "test"));
  }

  protected abstract StackGresDbOps getDbOps();

  public abstract AbstractRestartStateHandler getRestartStateHandler();

  public abstract DbOpsRestartStatus getRestartStatus(StackGresDbOps dbOps);

  public abstract Optional<ClusterDbOpsRestartStatus> getRestartStatus(StackGresCluster cluster);

  protected abstract void initializeDbOpsStatus(StackGresDbOps dbOps, StackGresCluster cluster,
      List<Pod> pods);

  protected Pod getPrimaryInstance(StackGresCluster cluster, List<Pod> pods) {
    return pods.stream()
        .filter(pod -> PatroniUtil.getPrimaryRole(cluster).equals(
            pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY)))
        .findFirst().orElseThrow(() -> new InvalidClusterException(
            "Cluster has no primary pod"));
  }

  @Test
  void givenAnUninitializedJobState_itShouldInitializeIt() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getClusterPods(cluster);

    final String dbOpsName = dbOps.getMetadata().getName();
    getRestartStateHandler()
        .restartCluster(dbOps)
        .await()
        .atMost(Duration.ofMillis(500));
    var storedDbOps = kubeDb.getDbOps(dbOpsName, namespace);

    List<String> expectedInitialInstances = pods.stream().map(Pod::getMetadata)
        .map(ObjectMeta::getName)
        .sorted(String::compareTo)
        .toList();

    final DbOpsRestartStatus initializedRestartStatus = getRestartStatus(storedDbOps);

    Pod primaryPod = pods.stream()
        .filter(pod -> pod.getMetadata().getName().endsWith("-0"))
        .findAny().get();

    assertEquals(primaryPod.getMetadata().getName(), initializedRestartStatus
        .getPrimaryInstance());

    List<String> actualInitialInstances = initializedRestartStatus
        .getInitialInstances();

    assertEquals(expectedInitialInstances, actualInitialInstances);

    List<String> actualPendingRestartedInstances = initializedRestartStatus
        .getPendingToRestartInstances();

    assertEquals(expectedInitialInstances, actualPendingRestartedInstances);

    assertTrue(() -> initializedRestartStatus.getRestartedInstances() == null
        || initializedRestartStatus.getRestartedInstances().isEmpty());

    assertNull(initializedRestartStatus.getFailure());

    assertNull(initializedRestartStatus.getSwitchoverInitiated());

    assertEquals(dbOps, storedDbOps, "It should store the DBOps status changes");
  }

  @Test
  void givenAnUninitializedClusterStatus_itShouldInitializeIt() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getClusterPods(cluster);

    List<StackGresCluster> storedCluster = Lists.newArrayList();
    kubeDb.watchCluster(clusterName, namespace, storedCluster::add);

    List<StackGresDbOps> storedDbOps = Lists.newArrayList();
    kubeDb.watchDbOps(dbOpsName, namespace, storedDbOps::add);

    getRestartStateHandler()
        .restartCluster(dbOps)
        .await()
        .atMost(Duration.ofMillis(500));

    verifyClusterInitializedStatus(pods,
        Seq.seq(storedDbOps).findFirst().get(),
        Seq.seq(storedCluster).findFirst().get());
  }

  protected void verifyClusterInitializedStatus(List<Pod> pods, StackGresDbOps dbOps,
      StackGresCluster cluster) {
    List<String> expectedInitialInstances = pods.stream().map(Pod::getMetadata)
        .map(ObjectMeta::getName)
        .sorted(String::compareTo)
        .toList();

    final ClusterDbOpsRestartStatus initializedRestartStatus =
        getRestartStatus(cluster).orElseThrow();

    Pod primaryPod = pods.stream()
        .filter(pod -> pod.getMetadata().getName().endsWith("-0"))
        .findAny().get();

    assertEquals(primaryPod.getMetadata().getName(), initializedRestartStatus
        .getPrimaryInstance());

    List<String> actualInitialInstances = initializedRestartStatus
        .getInitialInstances();

    assertEquals(expectedInitialInstances, actualInitialInstances);
  }

  @Test
  void givenAnInitializedJobState_itShouldNotModifiedIt() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    getRestartStateHandler()
        .restartCluster(dbOps)
        .await()
        .atMost(Duration.ofMillis(500));

    var storedDbOps = kubeDb.getDbOps(dbOpsName,
        namespace);

    assertEquals(dbOps, storedDbOps);
  }

  @Test
  void givenAnInitializedClusterStatus_itShouldReuseAndNotModifyIt() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getClusterPods(cluster);

    initializeClusterStatus(dbOps, cluster, pods);

    List<StackGresCluster> storedCluster = Lists.newArrayList();
    kubeDb.watchCluster(clusterName, namespace, c -> storedCluster.add(c));
    List<StackGresDbOps> storedDbOps = Lists.newArrayList();
    kubeDb.watchDbOps(dbOpsName, namespace, storedDbOps::add);

    getRestartStateHandler()
        .restartCluster(dbOps)
        .await()
        .atMost(Duration.ofMillis(500));

    assertEquals(1, storedCluster.size());
    assertNull(storedCluster.getFirst().getStatus().getDbOps());
    assertEquals(1, storedDbOps.size());
    assertEquals(pods.stream().map(Pod::getMetadata).map(ObjectMeta::getName).toList(),
        getDbOpsRestartStatus(storedDbOps.getFirst()).getInitialInstances());
  }

  protected abstract void initializeClusterStatus(StackGresDbOps dbOps, StackGresCluster cluster,
      List<Pod> pods);

  protected abstract ClusterDbOpsRestartStatus getClusterDbOpsRestartStatus(
      StackGresCluster cluster);

  protected abstract DbOpsRestartStatus getDbOpsRestartStatus(
      StackGresDbOps dbOps);

  @Test
  void buildRestartState_shouldNotFail() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getClusterPods(cluster);

    final Pod primaryPod = pods.stream()
        .filter(pod -> pod.getMetadata().getName().endsWith("-0"))
        .findAny().get();

    var patroniHistoryEntry = new PatroniHistoryEntry();
    patroniHistoryEntry.setNewLeader(primaryPod.getMetadata().getName());
    when(patroniCtl.history()).thenReturn(List.of(patroniHistoryEntry));

    final Pod replica1Pod = pods.stream()
        .filter(pod -> pod.getMetadata().getName().endsWith("-1"))
        .findAny().get();

    dbOps = kubeDb.addOrReplaceDbOps(dbOps);

    var expectedClusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .isOnlyPendingRestart(false)
        .restartMethod(getRestartMethod(dbOps))
        .isSwitchoverInitiated(Boolean.FALSE)
        .isSwitchoverFinalized(Boolean.FALSE)
        .primaryInstance(primaryPod.getMetadata().getName())
        .addInitialInstances(primaryPod, replica1Pod)
        .addRestartedInstances(replica1Pod)
        .addAllTotalInstances(pods)
        .putAllPodRestartReasonsMap(pods.stream()
            .collect(ImmutableMap.toImmutableMap(
                Function.identity(), pod -> RestartReasons.of())))
        .build();

    var clusterRestartState = getRestartStateHandler().restartCluster(dbOps)
        .await()
        .atMost(Duration.ofMillis(500));

    assertEqualsRestartState(expectedClusterState, clusterRestartState);
  }

  @Test
  void buildRestartStateWithPodsWithNoRoles_shouldNotFail() {
    podTestUtil.preparePodsWithNoRoles(cluster, 0, 1, 2);

    var pods = podTestUtil.getClusterPods(cluster);

    final Pod primaryPod = pods.stream()
        .filter(pod -> pod.getMetadata().getName().endsWith("-0"))
        .findAny().get();

    var patroniHistoryEntry = new PatroniHistoryEntry();
    patroniHistoryEntry.setNewLeader(primaryPod.getMetadata().getName());
    when(patroniCtl.history()).thenReturn(List.of(patroniHistoryEntry));

    final Pod replica1Pod = pods.stream()
        .filter(pod -> pod.getMetadata().getName().endsWith("-1"))
        .findAny().get();

    dbOps = kubeDb.addOrReplaceDbOps(dbOps);

    var expectedClusterState = ClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(DbOpsOperation.fromString(dbOps.getSpec().getOp()))
        .clusterName(dbOps.getSpec().getSgCluster())
        .isOnlyPendingRestart(false)
        .restartMethod(getRestartMethod(dbOps))
        .isSwitchoverInitiated(Boolean.FALSE)
        .isSwitchoverFinalized(Boolean.FALSE)
        .primaryInstance(primaryPod.getMetadata().getName())
        .addInitialInstances(primaryPod, replica1Pod)
        .addRestartedInstances(replica1Pod)
        .addAllTotalInstances(pods)
        .putAllPodRestartReasonsMap(pods.stream()
            .collect(ImmutableMap.toImmutableMap(
                Function.identity(), pod -> RestartReasons.of())))
        .build();

    var clusterRestartState = getRestartStateHandler().restartCluster(dbOps)
        .await()
        .atMost(Duration.ofMillis(500));

    assertEqualsRestartState(expectedClusterState, clusterRestartState);
  }

  protected abstract DbOpsMethodType getRestartMethod(StackGresDbOps dbOps);

  @Test
  void givenACleanCluster_shouldUpdateTheOpStatus() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getClusterPods(cluster)
        .stream().sorted(Comparator.comparing(p -> p.getMetadata().getName()))
        .toList();

    when(clusterRestart.restartCluster(any()))
        .thenReturn(Multi.createFrom()
            .items(
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.RESTARTING_POSTGRES)
                .pod(pods.getFirst())
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.POSTGRES_RESTARTED)
                .pod(pods.getFirst())
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.INCREASING_INSTANCES)
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.INSTANCES_INCREASED)
                .pod(pods.get(2))
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.RESTARTING_POD)
                .pod(pods.get(1))
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.POD_RESTARTED)
                .pod(pods.get(1))
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.RESTARTING_POD)
                .pod(pods.get(2))
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.POD_RESTARTED)
                .pod(pods.get(2))
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.SWITCHOVER_INITIATED)
                .pod(pods.getFirst())
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.SWITCHOVER_FINALIZED)
                .pod(pods.getFirst())
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.RESTARTING_POD)
                .pod(pods.getFirst())
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.POD_RESTARTED)
                .pod(pods.getFirst())
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.DECREASING_INSTANCES)
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.INSTANCES_DECREASED)
                .build()));

    List<StackGresDbOps> storedDbOps = Lists.newArrayList();
    kubeDb.watchDbOps(dbOpsName, namespace, storedDbOps::add);

    getRestartStateHandler().restartCluster(dbOps)
        .await().atMost(Duration.ofMillis(500));

    verifyDbOpsRestartStatus(pods, Seq.seq(storedDbOps).findLast().get());

    var lastClusterStatus = kubeDb.getCluster(clusterName, namespace);
    assertTrue(getRestartStatus(lastClusterStatus).isEmpty(),
        "It should erase the dbOps status after job is complete");
  }

  @Test
  void givenACleanCluster_shouldRegisterEveryEvent() {

    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getClusterPods(cluster)
        .stream().sorted(Comparator.comparing(p -> p.getMetadata().getName()))
        .toList();

    when(clusterRestart.restartCluster(any()))
        .thenReturn(Multi.createFrom()
            .items(
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.RESTARTING_POSTGRES)
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.POSTGRES_RESTARTED)
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.INCREASING_INSTANCES)
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.INSTANCES_INCREASED)
                .pod(pods.get(2))
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.RESTARTING_POD)
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.POD_RESTARTED)
                .pod(pods.get(1))
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.RESTARTING_POD)
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.POD_RESTARTED)
                .pod(pods.get(2))
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.SWITCHOVER_INITIATED)
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.SWITCHOVER_FINALIZED)
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.RESTARTING_POD)
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.POD_RESTARTED)
                .pod(pods.getFirst())
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.DECREASING_INSTANCES)
                .build(),
                ImmutableRestartEventForTest.builder()
                .eventType(RestartEventType.INSTANCES_DECREASED)
                .build()));

    getRestartStateHandler().restartCluster(dbOps)
        .await().indefinitely();

    verifyEventEmission(
        RestartEventType.RESTARTING_POSTGRES,
        RestartEventType.POSTGRES_RESTARTED,
        RestartEventType.INCREASING_INSTANCES,
        RestartEventType.INSTANCES_INCREASED,
        RestartEventType.RESTARTING_POD,
        RestartEventType.POD_RESTARTED,
        RestartEventType.RESTARTING_POD,
        RestartEventType.POD_RESTARTED,
        RestartEventType.SWITCHOVER_INITIATED,
        RestartEventType.SWITCHOVER_FINALIZED,
        RestartEventType.RESTARTING_POD,
        RestartEventType.POD_RESTARTED,
        RestartEventType.DECREASING_INSTANCES,
        RestartEventType.INSTANCES_DECREASED
    );
  }

  private void verifyEventEmission(RestartEventType... events) {
    final InOrder inOrder = inOrder(eventEmitter);
    Arrays.stream(events).forEach(event -> {
      inOrder.verify(eventEmitter).sendEvent(eq(event), eq(event.toString()), any());
    });
  }

  protected void verifyDbOpsRestartStatus(List<Pod> pods, StackGresDbOps dbOps) {
    final var restartStatus = getRestartStatus(dbOps);

    assertTrue(restartStatus.getPendingToRestartInstances().isEmpty());
    assertNotNull(restartStatus.getSwitchoverInitiated());
    Instant.parse(restartStatus.getSwitchoverInitiated());
    assertNotNull(restartStatus.getSwitchoverFinalized());
    Instant.parse(restartStatus.getSwitchoverFinalized());
    assertEquals(pods.size() + 1, restartStatus.getRestartedInstances().size());
    assertEquals(pods.size(), restartStatus.getInitialInstances().size());
    assertTrue(() -> restartStatus.getFailure() == null
        || restartStatus.getFailure().isEmpty());
  }

}
