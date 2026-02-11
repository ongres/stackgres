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
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
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
class ShardingSphereShardedClusterInitSecretTest {

  @Mock
  private LabelFactoryForShardedCluster labelFactory;

  @Mock
  private StackGresShardedClusterContext context;

  private ShardingSphereShardedClusterInitSecret initSecret;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    initSecret = new ShardingSphereShardedClusterInitSecret(labelFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getShardedCluster()).thenReturn(cluster);
    lenient().when(labelFactory.coordinatorLabels(any())).thenReturn(Map.of());
  }

  @Test
  void generateResource_whenTypeIsShardingSphere_shouldGenerateSecret() {
    cluster.getSpec().setType(StackGresShardingType.SHARDING_SPHERE.toString());
    setupShardingSphereContext();

    List<HasMetadata> resources = initSecret.generateResource(context).toList();

    assertFalse(resources.isEmpty());
    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof Secret);
  }

  @Test
  void generateResource_whenTypeIsNotShardingSphere_shouldGenerateNoSecret() {
    cluster.getSpec().setType(StackGresShardingType.CITUS.toString());

    List<HasMetadata> resources = initSecret.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  private void setupShardingSphereContext() {
    lenient().when(context.getSuperuserUsername()).thenReturn(Optional.of("postgres"));
    lenient().when(context.getSuperuserPassword()).thenReturn(Optional.of("password"));
    lenient().when(context.getDatabaseSecret()).thenReturn(Optional.empty());
    lenient().when(context.getGeneratedSuperuserPassword()).thenReturn("generated-password");
  }

}
