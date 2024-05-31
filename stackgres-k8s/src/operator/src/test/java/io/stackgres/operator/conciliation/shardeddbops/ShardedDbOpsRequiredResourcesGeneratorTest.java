/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorClusterName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.ShardedDbOpsOperation;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ConfigScanner;
import io.stackgres.common.resource.ProfileConfigFinder;
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
  ProfileConfigFinder profileFinder;

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
  void givenValidCluster_getRequiredResourcesShouldNotFail() {
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String clusterName = dbOps.getSpec().getSgShardedCluster();
    final String coordinatorName = StackGresShardedClusterUtil.getCoordinatorClusterName(clusterName);
    final String profileName = cluster.getSpec().getCoordinator().getSgInstanceProfile();

    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config)));

    when(shardedClusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(coordinator));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    generator.getRequiredResources(dbOps);

    verify(shardedClusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(shardedClusterFinder).findByNameAndNamespace(eq(clusterName), eq(dbOpsNamespace));
    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(coordinatorName), eq(dbOpsNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(dbOpsNamespace));
  }

  @Test
  void givenADbOpsInvalidCluster_getRequiredResourcesShouldFail() {
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String dbOpsName = dbOps.getMetadata().getName();
    final String clusterName = dbOps.getSpec().getSgShardedCluster();
    final String coordinatorName = getCoordinatorClusterName(clusterName);

    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config)));

    when(shardedClusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());

    assertException("SGShardedDbOps " + dbOpsNamespace + "." + dbOpsName
        + " target non existent SGShardedCluster " + clusterName);

    verify(shardedClusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(shardedClusterFinder).findByNameAndNamespace(eq(clusterName), eq(dbOpsNamespace));
    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(coordinatorName), eq(dbOpsNamespace));
    verify(profileFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithoutProfile_getRequiredResourcesShouldFail() {
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String dbOpsName = dbOps.getMetadata().getName();
    final String clusterName = dbOps.getSpec().getSgShardedCluster();
    final String coordinatorName = getCoordinatorClusterName(clusterName);
    final String profileName = cluster.getSpec().getCoordinator().getSgInstanceProfile();

    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config)));

    when(shardedClusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(coordinator));

    assertException("SGShardedDbOps " + dbOpsNamespace + "." + dbOpsName
        + " target SGShardedCluster " + clusterName
        + " with a non existent SGInstanceProfile " + profileName);

    verify(shardedClusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(shardedClusterFinder).findByNameAndNamespace(eq(clusterName), eq(dbOpsNamespace));
    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(coordinatorName), eq(dbOpsNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(dbOpsNamespace));
  }

  @Test
  void givenClusterWithoutCoordinator_getRequiredResourcesShouldFail() {
    dbOps.getSpec().setOp(ShardedDbOpsOperation.RESHARDING.toString());
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String dbOpsName = dbOps.getMetadata().getName();
    final String clusterName = dbOps.getSpec().getSgShardedCluster();
    final String coordinatorName = getCoordinatorClusterName(clusterName);
    final String profileName = cluster.getSpec().getCoordinator().getSgInstanceProfile();

    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config)));

    when(shardedClusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    assertException("SGShardedDbOps " + dbOpsNamespace + "." + dbOpsName
        + " target SGShardedCluster " + clusterName
        + " with a non existent coordinator SGCluster " + coordinatorName);

    verify(shardedClusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(shardedClusterFinder).findByNameAndNamespace(eq(clusterName), eq(dbOpsNamespace));
    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(coordinatorName), eq(dbOpsNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(dbOpsNamespace));
  }

  private void assertException(String message) {
    var ex =
        assertThrows(IllegalArgumentException.class, () -> generator.getRequiredResources(dbOps));
    assertEquals(message, ex.getMessage());
  }

}
