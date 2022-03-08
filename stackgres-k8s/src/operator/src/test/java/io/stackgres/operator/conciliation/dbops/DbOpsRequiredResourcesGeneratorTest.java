/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ProfileConfigFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DbOpsRequiredResourcesGeneratorTest {

  @InjectMock
  ClusterFinder clusterFinder;

  @InjectMock
  ProfileConfigFinder profileFinder;

  @Inject
  DbOpsRequiredResourcesGenerator generator;

  private StackGresDbOps dbOps;
  private StackGresCluster cluster;
  private StackGresProfile profile;

  @BeforeEach
  void setUp() {
    dbOps = Fixtures.dbOps().loadRestart().get();
    cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL
        .getLatest().getLatestVersion());
    cluster.getMetadata().setNamespace(dbOps.getMetadata().getNamespace());
    cluster.getMetadata().setName(dbOps.getSpec().getSgCluster());
    profile = Fixtures.instanceProfile().loadSizeXs().get();
  }

  @Test
  void givenValidCluster_getRequiredResourcesShouldNotFail() {
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String clusterName = dbOps.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getResourceProfile();

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
  void givenValidCluster_getRequiredResourcesAllReturnedResourcesShouldHaveTheOwnerReference() {
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String clusterName = dbOps.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getResourceProfile();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    when(profileFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(profile));

    List<HasMetadata> resources = generator.getRequiredResources(dbOps);

    resources.forEach(resource -> {
      assertNotNull(resource.getMetadata().getOwnerReferences(),
          "Resource " + resource.getMetadata().getName() + " doesn't owner references");
      if (resource.getMetadata().getOwnerReferences().size() == 0) {
        fail("Resource " + resource.getMetadata().getName() + " doesn't have any owner");
      }
      assertTrue(resource.getMetadata().getOwnerReferences().stream().anyMatch(ownerReference
          -> ownerReference.getApiVersion().equals(HasMetadata.getApiVersion(StackGresDbOps.class))
          && ownerReference.getKind().equals(HasMetadata.getKind(StackGresDbOps.class))
          && ownerReference.getName().equals(dbOps.getMetadata().getName())
          && ownerReference.getUid().equals(dbOps.getMetadata().getUid())
          && Optional.ofNullable(ownerReference.getBlockOwnerDeletion()).orElse(Boolean.FALSE)
          .equals(Boolean.FALSE)));
    });

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(dbOpsNamespace));
    verify(profileFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(profileFinder).findByNameAndNamespace(eq(profileName), eq(dbOpsNamespace));
  }

  @Test
  void givenADbOpsInvalidCluster_getRequiredResourcesShouldFail() {
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String dbOpsName = dbOps.getMetadata().getName();
    final String clusterName = dbOps.getSpec().getSgCluster();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());

    assertException("SGDbOps " + dbOpsNamespace + "." + dbOpsName
        + " have a non existent SGCluster " + clusterName);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(dbOpsNamespace));
    verify(profileFinder, times(0)).findByNameAndNamespace(any(), any());
  }

  @Test
  void givenClusterWithoutProfile_getRequiredResourcesShouldFail() {
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String dbOpsName = dbOps.getMetadata().getName();
    final String clusterName = dbOps.getSpec().getSgCluster();
    final String profileName = cluster.getSpec().getResourceProfile();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    assertException("SGDbOps " + dbOpsNamespace + "." + dbOpsName
        + " target SGCluster " + clusterName
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
