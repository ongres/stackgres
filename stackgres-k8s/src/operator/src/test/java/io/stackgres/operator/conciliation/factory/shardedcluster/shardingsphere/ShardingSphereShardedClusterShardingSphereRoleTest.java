/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster.shardingsphere;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.stackgres.common.crd.ShardingSphereServiceAccount;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCoordinatorConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterShardingSphere;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardingType;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardingSphereShardedClusterShardingSphereRoleTest {

  @Mock
  private LabelFactoryForShardedCluster labelFactory;

  @Mock
  private StackGresShardedClusterContext context;

  private ShardingSphereShardedClusterShardingSphereRole shardingSphereRole;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    shardingSphereRole = new ShardingSphereShardedClusterShardingSphereRole(labelFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getShardedCluster()).thenReturn(cluster);
    lenient().when(labelFactory.coordinatorLabels(any())).thenReturn(Map.of());
  }

  @Test
  void generateResource_whenTypeIsShardingSphere_shouldGenerateRoleAndRoleBinding() {
    cluster.getSpec().setType(StackGresShardingType.SHARDING_SPHERE.toString());
    setupShardingSphereConfig();

    List<HasMetadata> resources = shardingSphereRole.generateResource(context).toList();

    assertFalse(resources.isEmpty());
    assertEquals(2, resources.size());
    assertTrue(resources.stream().anyMatch(Role.class::isInstance));
    assertTrue(resources.stream().anyMatch(RoleBinding.class::isInstance));
  }

  @Test
  void generateResource_whenTypeIsNotShardingSphere_shouldGenerateNoResources() {
    cluster.getSpec().setType(StackGresShardingType.CITUS.toString());

    List<HasMetadata> resources = shardingSphereRole.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  private void setupShardingSphereConfig() {
    var coordinator = cluster.getSpec().getCoordinator();
    if (coordinator.getConfigurationsForCoordinator() == null) {
      coordinator.setConfigurationsForCoordinator(
          new StackGresShardedClusterCoordinatorConfigurations());
    }
    var shardingSphere = new StackGresShardedClusterShardingSphere();
    var serviceAccount = new ShardingSphereServiceAccount();
    serviceAccount.setNamespace("shardingsphere-operator");
    serviceAccount.setName("shardingsphere-operator-sa");
    shardingSphere.setServiceAccount(serviceAccount);
    coordinator.getConfigurationsForCoordinator().setShardingSphere(shardingSphere);

    var config = Fixtures.config().loadDefault().get();
    lenient().when(context.getConfig()).thenReturn(config);
  }

}
