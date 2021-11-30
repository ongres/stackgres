/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.clusterrestart;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.smallrye.mutiny.TimeoutException;
import io.smallrye.mutiny.Uni;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.jobs.dbops.lock.MockKubeDb;
import io.stackgres.testutil.JsonUtil;
import io.stackgres.testutil.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WithKubernetesTestServer
@QuarkusTest
class ClusterWatcherTest {

  @Inject
  PodTestUtil podTestUtil;

  @Inject
  ClusterWatcherImpl clusterWatcher;

  @Inject
  MockKubeDb kubeDb;

  @InjectMock
  PatroniApiHandlerImpl patroniApiHandler;

  String namespace;

  String clusterName;

  StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    namespace = StringUtils.getRandomNamespace();
    clusterName = StringUtils.getRandomClusterName();
    cluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
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
                .map(pod -> ImmutableClusterMember.builder()
                    .clusterName(clusterName)
                    .namespace(namespace)
                    .apiUrl("http://" + pod.getMetadata().getName() + ":8008/patroni")
                    .name(pod.getMetadata().getName())
                    .port(5432)
                    .host(pod.getMetadata().getName())
                    .state(MemberState.RUNNING)
                    .role(PatroniUtil.PRIMARY_ROLE
                        .equals(pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY))
                            ? MemberRole.LEADER
                            : MemberRole.REPlICA)
                    .lag(0)
                    .timeline(1)
                    .build())
                .collect(Collectors.toUnmodifiableList())));

    clusterWatcher.waitUntilIsReady(clusterName, namespace)
        .await().atMost(Duration.ofSeconds(1));

  }

  @Test
  void givenAClusterWithoutAllPodsCreated_shouldFail() {

    podTestUtil.preparePods(cluster, 1, 2);

    when(patroniApiHandler.getClusterMembers(clusterName, namespace)).thenReturn(
        Uni.createFrom().item(() -> podTestUtil.getClusterPods(cluster),
            (pods) -> pods.stream()
                .map(pod -> ImmutableClusterMember.builder()
                    .clusterName(clusterName)
                    .namespace(namespace)
                    .apiUrl("http://" + pod.getMetadata().getName() + ":8008/patroni")
                    .name(pod.getMetadata().getName())
                    .port(5432)
                    .host(pod.getMetadata().getName())
                    .state(MemberState.RUNNING)
                    .role(PatroniUtil.PRIMARY_ROLE
                        .equals(pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY))
                            ? MemberRole.LEADER
                            : MemberRole.REPlICA)
                    .lag(0)
                    .timeline(1)
                    .build())
                .collect(Collectors.toUnmodifiableList())));

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
                .map(pod -> ImmutableClusterMember.builder()
                    .clusterName(clusterName)
                    .namespace(namespace)
                    .apiUrl("http://" + pod.getMetadata().getName() + ":8008/patroni")
                    .name(pod.getMetadata().getName())
                    .port(5432)
                    .host(pod.getMetadata().getName())
                    .state(MemberState.RUNNING)
                    .role(PatroniUtil.PRIMARY_ROLE
                        .equals(pod.getMetadata().getLabels().get(PatroniUtil.ROLE_KEY))
                            ? MemberRole.LEADER
                            : MemberRole.REPlICA)
                    .lag(0)
                    .timeline(1)
                    .build())
                .collect(Collectors.toUnmodifiableList())));

    CompletableFuture<StackGresCluster> clusterReady = new CompletableFuture<>();

    clusterWatcher.waitUntilIsReady(clusterName, namespace)
        .subscribe().with(clusterReady::complete);

    Thread.sleep(100);

    var replicaPod = podTestUtil.buildReplicaPod(cluster, 3);
    podTestUtil.createPod(replicaPod);

    Uni.createFrom().completionStage(clusterReady)
        .await().atMost(Duration.ofSeconds(3));
  }
}
