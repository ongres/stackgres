/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ProfileConfigFinder;
import io.stackgres.common.resource.ShardedClusterFinder;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ShardedDbOpsRequiredResourcesGeneratorTest {

  @InjectMock
  ShardedClusterFinder clusterFinder;

  @InjectMock
  ProfileConfigFinder profileFinder;

  @Inject
  ShardedDbOpsRequiredResourcesGenerator generator;

  private StackGresShardedDbOps dbOps;
  private StackGresShardedCluster cluster;
  private StackGresProfile profile;

  @BeforeEach
  void setUp() {
    dbOps = Fixtures.shardedDbOps().loadRestart().get();
    cluster = Fixtures.shardedCluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL
        .getLatest().getLatestVersion());
    cluster.getMetadata().setNamespace(dbOps.getMetadata().getNamespace());
    cluster.getMetadata().setName(dbOps.getSpec().getSgShardedCluster());
    profile = Fixtures.instanceProfile().loadSizeS().get();
  }

  @Test
  void givenValidCluster_getRequiredResourcesShouldNotFail() {
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String clusterName = dbOps.getSpec().getSgShardedCluster();
    final String profileName = cluster.getSpec().getCoordinator().getSgInstanceProfile();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    generator.getRequiredResources(dbOps);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(dbOpsNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(dbOpsNamespace));
  }

  @Test
  void givenADbOpsInvalidCluster_getRequiredResourcesShouldFail() {
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String dbOpsName = dbOps.getMetadata().getName();
    final String clusterName = dbOps.getSpec().getSgShardedCluster();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());

    assertException("SGShardedDbOps " + dbOpsNamespace + "." + dbOpsName
        + " have a non existent SGShardedCluster " + clusterName);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(dbOpsNamespace));
    verify(profileFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithoutProfile_getRequiredResourcesShouldFail() {
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String dbOpsName = dbOps.getMetadata().getName();
    final String clusterName = dbOps.getSpec().getSgShardedCluster();
    final String profileName = cluster.getSpec().getCoordinator().getSgInstanceProfile();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    assertException("SGShardedDbOps " + dbOpsNamespace + "." + dbOpsName
        + " target SGShardedCluster " + clusterName
        + " with a non existent SGInstanceProfile " + profileName);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(dbOpsNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(dbOpsNamespace));
  }

  private void assertException(String message) {
    var ex =
        assertThrows(IllegalArgumentException.class, () -> generator.getRequiredResources(dbOps));
    assertEquals(message, ex.getMessage());
  }

}
