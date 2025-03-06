/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ConfigScanner;
import io.stackgres.common.resource.ProfileFinder;
import io.stackgres.common.resource.ShardedClusterFinder;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ShardedDbOpsRequiredResourcesGeneratorTest {

  @InjectMock
  ConfigScanner configScanner;

  @InjectMock
  ShardedClusterFinder shardedClusterFinder;

  @InjectMock
  ClusterFinder clusterFinder;

  @InjectMock
  ProfileFinder profileFinder;

  @Inject
  ShardedDbOpsRequiredResourcesGenerator generator;

  private StackGresConfig config;
  private StackGresShardedDbOps dbOps;
  private StackGresShardedCluster cluster;
  private StackGresCluster coordinator;
  private StackGresProfile profile;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    dbOps = Fixtures.shardedDbOps().loadRestart().get();
    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL
        .getLatest().getLatestVersion());
    cluster.getMetadata().setNamespace(dbOps.getMetadata().getNamespace());
    cluster.getMetadata().setName(dbOps.getSpec().getSgShardedCluster());
    coordinator = Fixtures.cluster().loadDefault().get();
    profile = Fixtures.instanceProfile().loadSizeS().get();
  }

  @Test
  void givenValidDbOps_shouldPass() {
    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config)));

    when(shardedClusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(coordinator));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    generator.getRequiredResources(dbOps);
  }

}
