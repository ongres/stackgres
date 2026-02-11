/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster.ddp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DdpShardedClusterUpdateShardsSecretTest {

  private final LabelFactoryForShardedCluster labelFactory =
      new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());

  @Mock
  private StackGresShardedClusterContext context;

  private DdpShardedClusterUpdateShardsSecret factory;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    factory = new DdpShardedClusterUpdateShardsSecret(labelFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
  }

  @Test
  void generateResource_whenTypeDdp_shouldGenerateSecret() {
    cluster.getSpec().setType("ddp");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getSuperuserUsername()).thenReturn(Optional.empty());
    lenient().when(context.getSuperuserPassword()).thenReturn(Optional.of("test-pass"));
    lenient().when(context.getDatabaseSecret()).thenReturn(Optional.empty());

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.getFirst() instanceof Secret);
  }

  @Test
  void generateResource_whenTypeDdp_shouldHaveCorrectName() {
    cluster.getSpec().setType("ddp");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getSuperuserUsername()).thenReturn(Optional.empty());
    lenient().when(context.getSuperuserPassword()).thenReturn(Optional.of("test-pass"));
    lenient().when(context.getDatabaseSecret()).thenReturn(Optional.empty());

    List<HasMetadata> resources = factory.generateResource(context).toList();

    Secret secret = (Secret) resources.getFirst();
    String expectedName =
        StackGresShardedClusterUtil.coordinatorScriptName(cluster) + "-update-shards";
    assertEquals(expectedName, secret.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(), secret.getMetadata().getNamespace());
  }

  @Test
  void generateResource_whenTypeNotDdp_shouldNotGenerateSecret() {
    cluster.getSpec().setType("citus");
    when(context.getShardedCluster()).thenReturn(cluster);

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenTypeDdp_shouldHaveLabels() {
    cluster.getSpec().setType("ddp");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getSuperuserUsername()).thenReturn(Optional.empty());
    lenient().when(context.getSuperuserPassword()).thenReturn(Optional.of("test-pass"));
    lenient().when(context.getDatabaseSecret()).thenReturn(Optional.empty());

    List<HasMetadata> resources = factory.generateResource(context).toList();

    Secret secret = (Secret) resources.getFirst();
    assertTrue(secret.getMetadata().getLabels() != null);
  }

}
