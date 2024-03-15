/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Pod;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.patroni.PatroniMember;
import io.stackgres.jobs.dbops.lock.MockKubeDb;
import io.stackgres.testutil.StringUtils;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer
@QuarkusTest
class ClusterWatcherTest {

  @Inject
  PodTestUtil podTestUtil;

  @Inject
  ClusterWatcher clusterWatcher;

  @Inject
  MockKubeDb kubeDb;

  @InjectMock
  PatroniApiHandler patroniApiHandler;

  String namespace;

  String clusterName;

  StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    namespace = StringUtils.getRandomNamespace();
    clusterName = StringUtils.getRandomResourceName();
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().setNamespace(namespace);
    cluster.getMetadata().setName(clusterName);
    cluster.getSpec().setInstances(3);
    cluster = kubeDb.addOrReplaceCluster(cluster);
  }

  @Test
  void givenAReadyCluster_shouldReturnPass() {
    podTestUtil.preparePods(cluster, 1, 2, 3);

    when(patroniApiHandler.getClusterMembers(clusterName, namespace)).thenReturn(
        Uni.createFrom().item(() -> podTestUtil.getClusterPods(cluster),
            (pods) -> pods.stream()
                .map(pod -> createMember(
                    pod,
                    role -> PatroniMember.RUNNING,
                    role -> PatroniUtil.PRIMARY_ROLE.equals(role) ? PatroniMember.LEADER : PatroniMember.REPLICA))
                .toList()));

    clusterWatcher.waitUntilIsReady(clusterName, namespace)
        .await().atMost(Duration.ofSeconds(1));

  }

  @Test
  void givenAClusterWithoutAllPodsCreated_shouldFail() {
    podTestUtil.preparePods(cluster, 1, 2);

    when(patroniApiHandler.getClusterMembers(clusterName, namespace)).thenReturn(
        Uni.createFrom().item(() -> podTestUtil.getClusterPods(cluster),
            (pods) -> pods.stream()
            .map(pod -> createMember(
                pod,
                role -> PatroniMember.RUNNING,
                role -> PatroniUtil.PRIMARY_ROLE.equals(role) ? PatroniMember.LEADER : PatroniMember.REPLICA))
            .toList()));

    assertThrows(TimeoutException.class,
        () -> clusterWatcher.waitUntilIsReady(clusterName, namespace)
            .await().atMost(Duration.ofSeconds(1)));
  }

  @Test
  void givenAClusterThatLaterBecameReady_shouldPass()
      throws InterruptedException, ExecutionException {
    podTestUtil.preparePods(cluster, 1, 2);

    when(patroniApiHandler.getClusterMembers(clusterName, namespace)).thenReturn(
        Uni.createFrom().item(() -> podTestUtil.getClusterPods(cluster),
            (pods) -> pods.stream()
            .map(pod -> createMember(
                pod,
                role -> PatroniMember.RUNNING,
                role -> PatroniUtil.PRIMARY_ROLE.equals(role) ? PatroniMember.LEADER : PatroniMember.REPLICA))
            .toList()));

    CompletableFuture<StackGresCluster> clusterReady = new CompletableFuture<>();

    clusterWatcher.waitUntilIsReady(clusterName, namespace)
        .subscribe().with(clusterReady::complete);

    Thread.sleep(100);

    var replicaPod = podTestUtil.buildReplicaPod(cluster, 3);
    podTestUtil.createPod(replicaPod);

    Uni.createFrom().completionStage(clusterReady)
        .await()
        .atMost(Duration.ofSeconds(3));
  }

  @Test
  void givenAReadyClusterWithOnlyPrimaryReady_shouldReturnPass() {
    podTestUtil.preparePods(cluster, 1, 2, 3);

    when(patroniApiHandler.getClusterMembers(clusterName, namespace)).thenReturn(
        Uni.createFrom().item(() -> podTestUtil.getClusterPods(cluster),
            (pods) -> pods.stream()
            .map(pod -> createMember(
                pod,
                role -> PatroniUtil.PRIMARY_ROLE.equals(role) ? PatroniMember.RUNNING : PatroniMember.STOPPED,
                role -> PatroniUtil.PRIMARY_ROLE.equals(role) ? PatroniMember.LEADER : PatroniMember.REPLICA))
            .toList()));

    clusterWatcher.waitUntilIsReady(clusterName, namespace)
        .await().atMost(Duration.ofSeconds(1));
  }

  @Test
  void givenAReadyClusterWithPrimaryReady_shouldReturnThePrimaryName() {
    podTestUtil.preparePods(cluster, 1, 2, 3);

    when(patroniApiHandler.getClusterMembers(clusterName, namespace)).thenReturn(
        Uni.createFrom().item(() -> podTestUtil.getClusterPods(cluster),
            (pods) -> pods.stream()
            .map(pod -> createMember(
                pod,
                role -> PatroniUtil.PRIMARY_ROLE.equals(role) ? PatroniMember.RUNNING : PatroniMember.STOPPED,
                role -> PatroniUtil.PRIMARY_ROLE.equals(role) ? PatroniMember.LEADER : PatroniMember.REPLICA))
            .toList()));

    clusterWatcher.getAvailablePrimary(clusterName, namespace)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .awaitItem()
        .assertCompleted()
        .assertItem(Optional.of(podTestUtil.getClusterPods(cluster)
            .stream()
            .filter(pod -> PatroniUtil.PRIMARY_ROLE
                .equals(pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY)))
            .findAny().orElseThrow().getMetadata().getName()));
  }

  @Test
  void givenAReadyClusterWithoutPrimaryReady_shouldReturnEmpty() {
    podTestUtil.preparePods(cluster, 1, 2, 3);

    when(patroniApiHandler.getClusterMembers(clusterName, namespace)).thenReturn(
        Uni.createFrom().item(() -> podTestUtil.getClusterPods(cluster),
            (pods) -> pods.stream()
            .map(pod -> createMember(
                pod,
                role -> PatroniUtil.PRIMARY_ROLE.equals(role) ? PatroniMember.RUNNING : PatroniMember.STOPPED,
                role -> PatroniMember.REPLICA))
            .toList()));

    clusterWatcher.getAvailablePrimary(clusterName, namespace)
        .subscribe()
        .withSubscriber(UniAssertSubscriber.create())
        .awaitItem()
        .assertCompleted()
        .assertItem(Optional.empty());
  }

  private PatroniMember createMember(
      Pod pod,
      Function<String, String> stateForRole,
      Function<String, String> roleForRole) {
    final String role = pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY);
    final var member = new PatroniMember();
    member.setCluster(clusterName);
    member.setMember(pod.getMetadata().getName());
    member.setHost(pod.getMetadata().getName());
    member.setState(stateForRole.apply(role));
    member.setRole(roleForRole.apply(role));
    member.setLagInMb(new IntOrString(0));
    member.setTimeline("1");
    return member;
  }

}
