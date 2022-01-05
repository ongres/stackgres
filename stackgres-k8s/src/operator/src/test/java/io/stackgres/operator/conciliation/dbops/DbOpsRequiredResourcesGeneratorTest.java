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
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DbOpsRequiredResourcesGeneratorTest {

  @InjectMock
  ClusterFinder clusterFinder;

  @Inject
  DbOpsRequiredResourcesGenerator generator;

  private StackGresDbOps dbOps;
  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    dbOps = JsonUtil
        .readFromJson("stackgres_dbops/dbops_restart.json", StackGresDbOps.class);
    cluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
    cluster.getSpec().getPostgres().setVersion(StackGresComponent.POSTGRESQL
        .getLatest().findLatestVersion());
    cluster.getMetadata().setNamespace(dbOps.getMetadata().getNamespace());
    cluster.getMetadata().setName(dbOps.getSpec().getSgCluster());
  }

  @Test
  void givenValidCluster_getRequiredResourcesShouldNotFail() {
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String clusterName = dbOps.getSpec().getSgCluster();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    generator.getRequiredResources(dbOps);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(dbOpsNamespace));
  }

  @Test
  void givenValidCluster_getRequiredResourcesAllReturnedResourcesShouldHaveTheOwnerReference() {
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String clusterName = dbOps.getSpec().getSgCluster();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

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
  }

  @Test
  void givenADbOpsInvalidCluster_getRequiredResourcesShouldFail() {
    final String dbOpsNamespace = dbOps.getMetadata().getNamespace();
    final String dbOpsName = dbOps.getMetadata().getName();
    final String clusterName = dbOps.getSpec().getSgCluster();

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());

    assertException("SGDbOps " + dbOpsNamespace + "/" + dbOpsName
        + " have a non existent SGCluster " + clusterName);

    verify(clusterFinder, times(1)).findByNameAndNamespace(any(), any());
    verify(clusterFinder).findByNameAndNamespace(eq(clusterName), eq(dbOpsNamespace));
  }

  private void assertException(String message) {
    var ex =
        assertThrows(IllegalArgumentException.class, () -> generator.getRequiredResources(dbOps));
    assertEquals(message, ex.getMessage());
  }

}
