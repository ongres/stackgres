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
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.smallrye.mutiny.Multi;
import io.stackgres.common.ClusterPendingRestartUtil.RestartReasons;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.ClusterDbOpsRestartStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.DbOpsRestartStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.event.DbOpsEventEmitter;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartImpl;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.ImmutableClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.ImmutableRestartEvent;
import io.stackgres.jobs.dbops.clusterrestart.InvalidCluster;
import io.stackgres.jobs.dbops.clusterrestart.PodTestUtil;
import io.stackgres.jobs.dbops.clusterrestart.RestartEventType;
import io.stackgres.jobs.dbops.lock.MockKubeDb;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.apache.commons.compress.utils.Lists;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@WithKubernetesTestServer
public abstract class ClusterStateHandlerTest {

  @InjectMock
  public ClusterRestartImpl clusterRestart;

  @Inject
  public PodTestUtil podTestUtil;

  @Inject
  public MockKubeDb kubeDb;

  @InjectMock
  public DbOpsEventEmitter eventEmitter;

  public String namespace = StringUtils.getRandomNamespace();

  public String dbOpsName = StringUtils.getRandomClusterName();

  public String clusterName = StringUtils.getRandomClusterName();

  public StackGresDbOps dbOps;

  public StackGresCluster cluster;

  protected static void assertEqualsRestartState(ClusterRestartState expected,
                                                 ClusterRestartState actual) {
    assertEquals(expected.getClusterName(), actual.getClusterName());
    assertEquals(expected.getNamespace(), actual.getNamespace());

    assertPodEquals(expected.getPrimaryInstance(), actual.getPrimaryInstance());

    var expectedInitialInstances = expected.getInitialInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());
    var actualInitialInstances = actual.getInitialInstances().stream()
        .sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());

    Seq.zip(expectedInitialInstances, actualInitialInstances)
        .forEach(tuple -> assertPodEquals(tuple.v1, tuple.v2));

    final List<Pod> restartedInstances = expected.getRestartedInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());

    final List<Pod> actualRestartedInstances = actual.getRestartedInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());

    Seq.zip(restartedInstances, actualRestartedInstances)
        .forEach(tuple -> assertPodEquals(tuple.v1, tuple.v2));

    final List<Pod> expectedTotalInstances = expected.getTotalInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());
    final List<Pod> actualTotalInstances = actual.getTotalInstances()
        .stream().sorted(Comparator.comparing(pod -> pod.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());

    Seq.zip(expectedTotalInstances, actualTotalInstances)
        .forEach(tuple -> assertPodEquals(tuple.v1, tuple.v2));
  }

  @BeforeEach
  public void setUp() {
    namespace = StringUtils.getRandomNamespace();
    clusterName = StringUtils.getRandomClusterName();

    dbOps = getDbOps();

    cluster = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);

    dbOps.getMetadata().setName(dbOpsName);
    dbOps.getMetadata().setNamespace(namespace);
    dbOps.getSpec().setSgCluster(clusterName);

    cluster.getMetadata().setName(clusterName);
    cluster.getMetadata().setNamespace(namespace);

    cluster = kubeDb.addOrReplaceCluster(cluster);
    dbOps = kubeDb.addOrReplaceDbOps(dbOps);

    lenient().doNothing().when(eventEmitter).sendEvent(any(), any(), any());
  }

  protected abstract StackGresDbOps getDbOps();

  public abstract AbstractRestartStateHandler getRestartStateHandler();

  public abstract DbOpsRestartStatus getRestartStatus(StackGresDbOps dbOps);

  public abstract Optional<ClusterDbOpsRestartStatus> getRestartStatus(StackGresCluster cluster);

  protected abstract void initializeDbOpsStatus(StackGresDbOps dbOps, StackGresCluster cluster,
      List<Pod> pods);

  protected Pod getPrimaryInstance(List<Pod> pods) {
    return pods.stream()
        .filter(pod -> PatroniUtil.PRIMARY_ROLE.equals(
            pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY)))
        .findFirst().orElseThrow(() -> new InvalidCluster("Cluster has no primary pod"));
  }

  @Test
  void givenAnUninitializedJobState_itShouldInitializeIt() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getClusterPods(cluster);

    final String dbOpsName = dbOps.getMetadata().getName();
    getRestartStateHandler()
        .restartCluster(dbOps)
        .await()
        .atMost(Duration.ofMillis(50));
    var storedDbOps = kubeDb.getDbOps(dbOpsName, namespace);

    List<String> expectedInitialInstances = pods.stream().map(Pod::getMetadata)
        .map(ObjectMeta::getName)
        .sorted(String::compareTo)
        .collect(Collectors.toUnmodifiableList());

    final DbOpsRestartStatus initializedSecurityUpgradeStatus = getRestartStatus(storedDbOps);

    Pod primaryPod = pods.stream()
        .filter(pod -> pod.getMetadata().getName().endsWith("-0"))
        .findAny().get();

    assertEquals(primaryPod.getMetadata().getName(), initializedSecurityUpgradeStatus
        .getPrimaryInstance());

    List<String> actualInitialInstances = initializedSecurityUpgradeStatus
        .getInitialInstances();

    assertEquals(expectedInitialInstances, actualInitialInstances);

    List<String> actualPendingRestartedInstances = initializedSecurityUpgradeStatus
        .getPendingToRestartInstances();

    assertEquals(expectedInitialInstances, actualPendingRestartedInstances);

    assertTrue(() -> initializedSecurityUpgradeStatus.getRestartedInstances() == null
        || initializedSecurityUpgradeStatus.getRestartedInstances().isEmpty());

    assertNull(initializedSecurityUpgradeStatus.getFailure());

    assertNull(initializedSecurityUpgradeStatus.getSwitchoverInitiated());

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
        .atMost(Duration.ofMillis(50));

    verifyClusterInitializedStatus(pods,
        Seq.seq(storedDbOps).findFirst().get(),
        Seq.seq(storedCluster).findFirst().get());
  }

  protected void verifyClusterInitializedStatus(List<Pod> pods, StackGresDbOps dbOps,
      StackGresCluster cluster) {
    List<String> expectedInitialInstances = pods.stream().map(Pod::getMetadata)
        .map(ObjectMeta::getName)
        .sorted(String::compareTo)
        .collect(Collectors.toUnmodifiableList());

    final ClusterDbOpsRestartStatus initializedSecurityUpgradeStatus =
        getRestartStatus(cluster).orElseThrow();

    Pod primaryPod = pods.stream()
        .filter(pod -> pod.getMetadata().getName().endsWith("-0"))
        .findAny().get();

    assertEquals(primaryPod.getMetadata().getName(), initializedSecurityUpgradeStatus
        .getPrimaryInstance());

    List<String> actualInitialInstances = initializedSecurityUpgradeStatus
        .getInitialInstances();

    assertEquals(expectedInitialInstances, actualInitialInstances);
  }

  @Test
  void givenAnInitializedJobState_itShouldNotModifiedIt() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    getRestartStateHandler()
        .restartCluster(dbOps)
        .await()
        .atMost(Duration.ofMillis(50));

    var storedDbOps = kubeDb.getDbOps(dbOpsName,
        namespace);

    assertEquals(dbOps, storedDbOps);
  }

  @Test
  void givenAnInitializedClusterStatus_itShouldNotModifiedIt() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getClusterPods(cluster);

    initializeClusterStatus(dbOps, cluster, pods);

    List<StackGresCluster> storedCluster = Lists.newArrayList();
    kubeDb.watchCluster(clusterName, namespace, storedCluster::add);

    getRestartStateHandler()
        .restartCluster(dbOps)
        .await()
        .atMost(Duration.ofMillis(50));

    assertEquals(1, storedCluster.size());
  }

  protected abstract void initializeClusterStatus(StackGresDbOps dbOps, StackGresCluster cluster,
      List<Pod> pods);

  @Test
  void buildRestartState_shouldNotFail() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getClusterPods(cluster);

    final Pod primaryPod = pods.stream()
        .filter(pod -> pod.getMetadata().getName().endsWith("-0"))
        .findAny().get();

    final Pod replica1Pod = pods.stream()
        .filter(pod -> pod.getMetadata().getName().endsWith("-1"))
        .findAny().get();

    dbOps = kubeDb.addOrReplaceDbOps(dbOps);

    var expectedClusterState = ImmutableClusterRestartState.builder()
        .namespace(dbOps.getMetadata().getNamespace())
        .dbOpsName(dbOps.getMetadata().getName())
        .dbOpsOperation(dbOps.getSpec().getOp())
        .clusterName(dbOps.getSpec().getSgCluster())
        .isOnlyPendingRestart(false)
        .restartMethod(getRestartMethod(dbOps))
        .isSwitchoverInitiated(Boolean.FALSE)
        .isSwitchoverFinalized(Boolean.FALSE)
        .primaryInstance(primaryPod)
        .addInitialInstances(primaryPod, replica1Pod)
        .addRestartedInstances(replica1Pod)
        .addAllTotalInstances(pods)
        .putAllPodRestartReasonsMap(pods.stream()
            .collect(ImmutableMap.toImmutableMap(
                Function.identity(), pod -> RestartReasons.of())))
        .build();

    var clusterRestartState = getRestartStateHandler().restartCluster(dbOps)
        .await()
        .atMost(Duration.ofMillis(50));

    assertEqualsRestartState(expectedClusterState, clusterRestartState);
  }

  protected abstract String getRestartMethod(StackGresDbOps dbOps);

  @Test
  void givenACleanCluster_shouldUpdateTheOpStatus() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getClusterPods(cluster)
        .stream().sorted(Comparator.comparing(p -> p.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());

    when(clusterRestart.restartCluster(any()))
        .thenReturn(Multi.createFrom()
            .items(
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.RESTARTING_POSTGRES)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POSTGRES_RESTARTED)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.INCREASING_INSTANCES)
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.INSTANCES_INCREASED)
                    .pod(pods.get(2))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.RESTARTING_POD)
                    .pod(pods.get(1))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_RESTARTED)
                    .pod(pods.get(1))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.RESTARTING_POD)
                    .pod(pods.get(2))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_RESTARTED)
                    .pod(pods.get(2))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.SWITCHOVER_INITIATED)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.SWITCHOVER_FINALIZED)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.RESTARTING_POD)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_RESTARTED)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.DECREASING_INSTANCES)
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.INSTANCES_DECREASED)
                    .build()));

    List<StackGresDbOps> storedDbOps = Lists.newArrayList();
    kubeDb.watchDbOps(dbOpsName, namespace, storedDbOps::add);

    getRestartStateHandler().restartCluster(dbOps)
        .await().atMost(Duration.ofMillis(50));

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
        .collect(Collectors.toUnmodifiableList());

    when(clusterRestart.restartCluster(any()))
        .thenReturn(Multi.createFrom()
            .items(
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.RESTARTING_POSTGRES)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POSTGRES_RESTARTED)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.INCREASING_INSTANCES)
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.INSTANCES_INCREASED)
                    .pod(pods.get(2))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.RESTARTING_POD)
                    .pod(pods.get(1))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_RESTARTED)
                    .pod(pods.get(1))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.RESTARTING_POD)
                    .pod(pods.get(2))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_RESTARTED)
                    .pod(pods.get(2))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.SWITCHOVER_INITIATED)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.SWITCHOVER_FINALIZED)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.RESTARTING_POD)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_RESTARTED)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.DECREASING_INSTANCES)
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.INSTANCES_DECREASED)
                    .build()));

    getRestartStateHandler().restartCluster(dbOps)
        .await().atMost(Duration.ofMillis(50));

    final String sgcluster = HasMetadata.getSingular(StackGresCluster.class);
    verifyEventEmission(
        Tuple.tuple(RestartEventType.RESTARTING_POSTGRES,
            "Restarting postgres of pod " + pods.get(0).getMetadata().getName()),
        Tuple.tuple(RestartEventType.POSTGRES_RESTARTED,
            "Restarted postgres of pod " + pods.get(0).getMetadata().getName()),
        Tuple.tuple(RestartEventType.INCREASING_INSTANCES,
            "Increasing instances of " + sgcluster + " " + clusterName),
        Tuple.tuple(RestartEventType.INSTANCES_INCREASED,
            "Increased instances of " + sgcluster + " " + clusterName),
        Tuple.tuple(RestartEventType.RESTARTING_POD,
            "Restarting pod " + pods.get(1).getMetadata().getName()),
        Tuple.tuple(RestartEventType.POD_RESTARTED,
            "Pod " + pods.get(1).getMetadata().getName() + " successfully restarted"),
        Tuple.tuple(RestartEventType.RESTARTING_POD,
            "Restarting pod " + pods.get(2).getMetadata().getName()),
        Tuple.tuple(RestartEventType.POD_RESTARTED,
            "Pod " + pods.get(2).getMetadata().getName() + " successfully restarted"),
        Tuple.tuple(RestartEventType.SWITCHOVER_INITIATED,
            "Switchover of " + sgcluster + " " + clusterName + " initiated"),
        Tuple.tuple(RestartEventType.SWITCHOVER_FINALIZED,
            "Switchover of " + sgcluster + " " + clusterName + " completed"),
        Tuple.tuple(RestartEventType.RESTARTING_POD,
            "Restarting pod " + pods.get(0).getMetadata().getName()),
        Tuple.tuple(RestartEventType.POD_RESTARTED,
            "Pod " + pods.get(0).getMetadata().getName() + " successfully restarted"),
        Tuple.tuple(RestartEventType.DECREASING_INSTANCES,
            "Decreasing instances of " + sgcluster + " " + clusterName),
        Tuple.tuple(RestartEventType.INSTANCES_DECREASED,
            "Decreased instances of " + sgcluster + " " + clusterName)
    );
  }

  @SafeVarargs
  private void verifyEventEmission(Tuple2<RestartEventType, String>... events) {
    final InOrder inOrder = inOrder(eventEmitter);
    Arrays.stream(events).forEach(event -> {
      inOrder.verify(eventEmitter).sendEvent(eq(event.v1), eq(event.v2), any());
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
