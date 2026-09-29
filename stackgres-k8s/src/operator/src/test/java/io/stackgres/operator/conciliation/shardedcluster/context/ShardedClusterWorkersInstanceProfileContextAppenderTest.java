/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfile;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfileBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorker;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterWorkerBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresWorkerType;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterWorkersInstanceProfileContextAppenderTest {

  private ShardedClusterWorkersInstanceProfileContextAppender contextAppender;

  private DefaultProfileFactory defaultProfileFactory = new DefaultProfileFactory();

  private StackGresShardedCluster cluster;

  private List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>> workers;

  private List<Tuple3<Integer, Optional<StackGresShardedClusterWorker>, StackGresCluster>> queryRouters;

  @Spy
  private StackGresShardedClusterContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresInstanceProfile> profileFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.shardedCluster().loadDefault().get();
    workers = List.of(
        Tuple.tuple(0, Optional.empty(), StackGresShardedClusterForCitusUtil
            .getWorkerCluster(cluster, 0, Optional.empty())),
        Tuple.tuple(1, Optional.empty(), StackGresShardedClusterForCitusUtil
            .getWorkerCluster(cluster, 1, Optional.empty())));
    queryRouters = List.of(
        Tuple.tuple(1024, Optional.empty(), StackGresShardedClusterForCitusUtil
            .getQueryRouterCluster(cluster, 1024, Optional.empty())));
    contextAppender = new ShardedClusterWorkersInstanceProfileContextAppender(
        profileFinder,
        new DefaultProfileFactory());
  }

  @Test
  void givenClusterWithProfile_shouldPass() {
    final var profile = Optional.of(
        new StackGresInstanceProfileBuilder()
        .withNewSpec()
        .endSpec()
        .build());
    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(profile);
    contextAppender.appendContext(cluster, contextBuilder, workers, queryRouters);
    verify(contextBuilder).workersProfiles(List.of(
        Tuple.tuple(0, profile),
        Tuple.tuple(1, profile)));
    verify(contextBuilder).queryRoutersProfiles(List.of(
        Tuple.tuple(1024, profile)));
  }

  @Test
  void givenClusterWithoutProfile_shouldFail() {
    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(
            cluster, contextBuilder, workers, queryRouters));
    assertEquals("SGInstanceProfile size-s was not found", ex.getMessage());
  }

  @Test
  void givenClusterWithoutDefaultProfile_shouldPass() {
    cluster.getSpec().getWorkers().setSgInstanceProfile(
        defaultProfileFactory.getDefaultResourceName(cluster));
    cluster.getSpec().getCoordinator().setSgInstanceProfile(
        defaultProfileFactory.getDefaultResourceName(cluster));
    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    contextAppender.appendContext(cluster, contextBuilder, workers, queryRouters);
    verify(contextBuilder).workersProfiles(List.of(
        Tuple.tuple(0, Optional.empty()),
        Tuple.tuple(1, Optional.empty())));
    verify(contextBuilder).queryRoutersProfiles(List.of(
        Tuple.tuple(1024, Optional.empty())));
  }

  @Test
  void givenClusterWithoutQueryRouterOverride_shouldUseCoordinatorProfile() {
    cluster.getSpec().getCoordinator().setSgInstanceProfile("size-xs");
    final var workersProfile = Optional.of(profile("size-s"));
    final var coordinatorProfile = Optional.of(profile("size-xs"));
    when(profileFinder.findByNameAndNamespace(eq("size-s"), any()))
        .thenReturn(workersProfile);
    when(profileFinder.findByNameAndNamespace(eq("size-xs"), any()))
        .thenReturn(coordinatorProfile);
    contextAppender.appendContext(cluster, contextBuilder, workers, queryRouters);
    verify(contextBuilder).workersProfiles(List.of(
        Tuple.tuple(0, workersProfile),
        Tuple.tuple(1, workersProfile)));
    verify(contextBuilder).queryRoutersProfiles(List.of(
        Tuple.tuple(1024, coordinatorProfile)));
  }

  @Test
  void givenClusterWithQueryRouterOverride_shouldUseOverriddenProfile() {
    queryRouters = List.of(
        Tuple.tuple(1024, Optional.of(
            new StackGresShardedClusterWorkerBuilder()
            .withIndex(1024)
            .withType(StackGresWorkerType.QUERY_ROUTER.toString())
            .withSgInstanceProfile("size-xs")
            .build()),
            StackGresShardedClusterForCitusUtil
            .getQueryRouterCluster(cluster, 1024, Optional.empty())));
    final var workersProfile = Optional.of(profile("size-s"));
    final var overriddenProfile = Optional.of(profile("size-xs"));
    when(profileFinder.findByNameAndNamespace(eq("size-s"), any()))
        .thenReturn(workersProfile);
    when(profileFinder.findByNameAndNamespace(eq("size-xs"), any()))
        .thenReturn(overriddenProfile);
    contextAppender.appendContext(cluster, contextBuilder, workers, queryRouters);
    verify(contextBuilder).workersProfiles(List.of(
        Tuple.tuple(0, workersProfile),
        Tuple.tuple(1, workersProfile)));
    verify(contextBuilder).queryRoutersProfiles(List.of(
        Tuple.tuple(1024, overriddenProfile)));
  }

  private StackGresInstanceProfile profile(String name) {
    return new StackGresInstanceProfileBuilder()
        .withNewMetadata()
        .withName(name)
        .endMetadata()
        .withNewSpec()
        .endSpec()
        .build();
  }

}
