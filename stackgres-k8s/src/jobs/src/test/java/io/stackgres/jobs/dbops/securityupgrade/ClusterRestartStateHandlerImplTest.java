/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.securityupgrade;

import static io.stackgres.jobs.dbops.clusterrestart.PodTestUtil.assertPodEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.smallrye.mutiny.Multi;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSecurityUpgradeStatus;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.jobs.dbops.StateHandler;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartImpl;
import io.stackgres.jobs.dbops.clusterrestart.ClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.ImmutableClusterRestartState;
import io.stackgres.jobs.dbops.clusterrestart.ImmutableRestartEvent;
import io.stackgres.jobs.dbops.clusterrestart.PodTestUtil;
import io.stackgres.jobs.dbops.clusterrestart.RestartEventType;
import io.stackgres.jobs.dbops.lock.MockKubeDb;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer
@QuarkusTest
class ClusterRestartStateHandlerImplTest {

  @InjectMock
  ClusterRestartImpl clusterRestart;

  @Inject
  @StateHandler("securityUpgrade")
  ClusterRestartStateHandlerImpl restartStateHandler;

  @Inject
  PodTestUtil podTestUtil;

  @Inject
  MockKubeDb kubeDb;

  String namespace = StringUtils.getRandomNamespace();

  String clusterName = StringUtils.getRandomClusterName();

  StackGresDbOps securityUpgradeOp;

  StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    namespace = StringUtils.getRandomNamespace();
    clusterName = StringUtils.getRandomClusterName();

    securityUpgradeOp = JsonUtil.readFromJson("stackgres_dbops/dbops_securityupgrade.json",
        StackGresDbOps.class);

    cluster = JsonUtil.readFromJson("stackgres_cluster/default.json",
        StackGresCluster.class);

    securityUpgradeOp.getMetadata().setNamespace(namespace);
    securityUpgradeOp.getSpec().setSgCluster(clusterName);
    securityUpgradeOp.setStatus(new StackGresDbOpsStatus());
    securityUpgradeOp.getStatus().setOpRetries(0);
    securityUpgradeOp.getStatus().setOpStarted(Instant.now().toString());
    securityUpgradeOp.getSpec().setOp("securityUpgrade");

    cluster.getMetadata().setName(clusterName);
    cluster.getMetadata().setNamespace(namespace);

    cluster = kubeDb.addOrReplaceCluster(cluster);
    securityUpgradeOp = kubeDb.addOrReplaceDbOps(securityUpgradeOp);
  }

  @Test
  void givenAnUninitializedJobState_itShouldInitializeIt() {

    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getCLusterPods(cluster);

    var initializedOp = restartStateHandler.initSecurityUpgrade(securityUpgradeOp, pods);

    List<String> expectedInitialInstances = pods.stream().map(Pod::getMetadata)
        .map(ObjectMeta::getName)
        .sorted(String::compareTo)
        .collect(Collectors.toUnmodifiableList());

    final StackGresDbOpsSecurityUpgradeStatus initializedSecurityUpgradeStatus = initializedOp
        .getStatus().getSecurityUpgrade();

    Pod primaryPod = podTestUtil.buildPrimaryPod(cluster, 0);

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

    assertFalse(Boolean.parseBoolean(initializedSecurityUpgradeStatus.getSwitchoverInitiated()));

    var storedOp = kubeDb.getDbOps(securityUpgradeOp.getMetadata().getName(),
        namespace);

    assertEquals(initializedOp, storedOp, "It should store the DBOps status changes");

  }

  @Test
  void givenAnInitializedJobState_itShouldNotModifiedIt() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getCLusterPods(cluster);

    var initializedOp = restartStateHandler
        .initSecurityUpgrade(securityUpgradeOp, pods);

    assertEquals(securityUpgradeOp, initializedOp);
  }

  @Test
  void buildRestartState_shouldNotFail() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getCLusterPods(cluster);

    var secUpgradeStatus = new StackGresDbOpsSecurityUpgradeStatus();

    final Pod primaryPod = podTestUtil.buildPrimaryPod(cluster, 0);
    final String primaryPodName = primaryPod.getMetadata().getName();

    final Pod replica1Pod = podTestUtil.buildReplicaPod(cluster, 1);
    final String replica1PodName = replica1Pod.getMetadata().getName();

    final Pod replica2Pod = podTestUtil.buildReplicaPod(cluster, 2);
    final String replica2PodName = replica2Pod.getMetadata().getName();

    secUpgradeStatus.setPrimaryInstance(primaryPodName);
    secUpgradeStatus.setInitialInstances(List.of(primaryPodName, replica1PodName));
    secUpgradeStatus.setRestartedInstances(List.of(replica1PodName));
    secUpgradeStatus.setPendingToRestartInstances(List.of(primaryPodName, replica2PodName));
    secUpgradeStatus.setSwitchoverInitiated(Boolean.FALSE.toString());

    securityUpgradeOp.getStatus().setSecurityUpgrade(secUpgradeStatus);

    securityUpgradeOp = kubeDb.addOrReplaceDbOps(securityUpgradeOp);

    var expectedClusterState = ImmutableClusterRestartState.builder()
        .namespace(namespace)
        .clusterName(clusterName)
        .restartMethod(securityUpgradeOp.getSpec().getSecurityUpgrade().getMethod())
        .isSwitchoverInitiated(Boolean.FALSE)
        .primaryInstance(primaryPod)
        .addInitialInstances(primaryPod, replica1Pod)
        .addRestartedInstances(replica1Pod)
        .addAllTotalInstances(pods)
        .build();

    var clusterState = restartStateHandler.buildClusterState(securityUpgradeOp, pods);

    assertEqualsRestartState(expectedClusterState, clusterState);

  }

  @Test
  void givenACleanCluster_shouldUpdateTheOpStatus() {
    podTestUtil.preparePods(cluster, 0, 1, 2);

    var pods = podTestUtil.getCLusterPods(cluster)
        .stream().sorted(Comparator.comparing(p -> p.getMetadata().getName()))
        .collect(Collectors.toUnmodifiableList());

    when(clusterRestart.restartCluster(any()))
        .thenReturn(Multi.createFrom()
            .items(
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_CREATED)
                    .pod(podTestUtil.buildReplicaPod(cluster, 3))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_RESTART)
                    .pod(pods.get(1))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_RESTART)
                    .pod(pods.get(2))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.SWITCHOVER)
                    .pod(pods.get(0))
                    .build(),
                ImmutableRestartEvent.builder()
                    .eventType(RestartEventType.POD_RESTART)
                    .pod(pods.get(0))
                    .build()));

    var lastDbOp = restartStateHandler.restartCluster(securityUpgradeOp)
        .await().atMost(Duration.ofSeconds(2));

    StackGresDbOps updatedOp = kubeDb
        .getDbOps(securityUpgradeOp.getMetadata().getName(), namespace);

    assertEquals(updatedOp.toString(), lastDbOp.toString());

    final var securityUpgradeStatus = updatedOp.getStatus()
        .getSecurityUpgrade();

    assertTrue(securityUpgradeStatus.getPendingToRestartInstances().isEmpty());
    assertEquals(Boolean.TRUE.toString(), securityUpgradeStatus.getSwitchoverInitiated());
    assertEquals(pods.size() + 1, securityUpgradeStatus.getRestartedInstances().size());
    assertEquals(pods.size(), securityUpgradeStatus.getInitialInstances().size());
    assertTrue(() -> securityUpgradeStatus.getFailure() == null
        || securityUpgradeStatus.getFailure().isEmpty());
  }

  private static void assertEqualsRestartState(ClusterRestartState expected,
                                               ClusterRestartState actual){
    assertEquals(expected.getClusterName(), actual.getClusterName());
    assertEquals(expected.getNamespace(), actual.getNamespace());

    assertPodEquals(expected.getPrimaryInstance(), actual.getPrimaryInstance());

    var expectedInitialInstances = expected.getInitialInstances();
    var actualInitialInstances = actual.getInitialInstances();

    Seq.zip(expectedInitialInstances, actualInitialInstances)
        .forEach(tuple -> assertPodEquals(tuple.v1, tuple.v2));

    Seq.zip(expected.getRestartedInstances(), actual.getRestartedInstances())
        .forEach(tuple -> assertPodEquals(tuple.v1, tuple.v2));

    Seq.zip(expected.getTotalInstances(), actual.getTotalInstances())
        .forEach(tuple -> assertPodEquals(tuple.v1, tuple.v2));
  }
}