/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster.ddp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgscript.StackGresScript;
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
class DdpShardedClusterShardsScriptTest {

  private final LabelFactoryForShardedCluster labelFactory =
      new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());

  @Mock
  private StackGresShardedClusterContext context;

  private DdpShardedClusterShardsScript factory;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    factory = new DdpShardedClusterShardsScript(labelFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
  }

  @Test
  void generateResource_whenTypeDdp_shouldGenerateScript() {
    cluster.getSpec().setType("ddp");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.getFirst() instanceof StackGresScript);
  }

  @Test
  void generateResource_whenTypeDdp_shouldHaveCorrectName() {
    cluster.getSpec().setType("ddp");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);

    List<HasMetadata> resources = factory.generateResource(context).toList();

    StackGresScript script = (StackGresScript) resources.getFirst();
    assertEquals(
        StackGresShardedClusterUtil.shardsScriptName(cluster),
        script.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(),
        script.getMetadata().getNamespace());
  }

  @Test
  void generateResource_whenTypeNotDdp_shouldNotGenerateScript() {
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

    List<HasMetadata> resources = factory.generateResource(context).toList();

    StackGresScript script = (StackGresScript) resources.getFirst();
    assertTrue(script.getMetadata().getLabels() != null);
  }

}
