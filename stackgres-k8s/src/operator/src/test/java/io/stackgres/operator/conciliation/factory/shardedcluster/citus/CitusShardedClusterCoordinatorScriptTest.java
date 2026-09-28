/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster.citus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgscript.StackGresScriptEntry;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterCitusConfigurations;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterConfigurations;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CitusShardedClusterCoordinatorScriptTest {

  private final LabelFactoryForShardedCluster labelFactory =
      new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());

  @Mock
  private StackGresShardedClusterContext context;

  private CitusShardedClusterCoordinatorScript factory;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    factory = new CitusShardedClusterCoordinatorScript(labelFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
  }

  @Test
  void generateResource_whenTypeCitus_shouldGenerateScript() {
    cluster.getSpec().setType("citus");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getSuperuserUsername()).thenReturn(Optional.empty());
    lenient().when(context.getSuperuserPassword()).thenReturn(Optional.of("test-pass"));
    lenient().when(context.getDatabaseSecret()).thenReturn(Optional.empty());

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.getFirst() instanceof StackGresScript);
  }

  @Test
  void generateResource_whenTypeCitus_shouldHaveCorrectName() {
    cluster.getSpec().setType("citus");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getSuperuserUsername()).thenReturn(Optional.empty());
    lenient().when(context.getSuperuserPassword()).thenReturn(Optional.of("test-pass"));
    lenient().when(context.getDatabaseSecret()).thenReturn(Optional.empty());

    List<HasMetadata> resources = factory.generateResource(context).toList();

    StackGresScript script = (StackGresScript) resources.getFirst();
    assertEquals(
        StackGresShardedClusterUtil.coordinatorScriptName(cluster),
        script.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(),
        script.getMetadata().getNamespace());
  }

  @Test
  void generateResource_whenTypeNotCitus_shouldNotGenerateScript() {
    cluster.getSpec().setType("ddp");
    when(context.getShardedCluster()).thenReturn(cluster);

    List<HasMetadata> resources = factory.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenTypeCitus_shouldHaveLabels() {
    cluster.getSpec().setType("citus");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getSuperuserUsername()).thenReturn(Optional.empty());
    lenient().when(context.getSuperuserPassword()).thenReturn(Optional.of("test-pass"));
    lenient().when(context.getDatabaseSecret()).thenReturn(Optional.empty());

    List<HasMetadata> resources = factory.generateResource(context).toList();

    StackGresScript script = (StackGresScript) resources.getFirst();
    assertTrue(script.getMetadata().getLabels() != null);
  }

  @Test
  void generateResource_whenTypeCitus_shouldScheduleTheNodesUpdateAndReadTheRegisteredQueryRouters() {
    cluster.getSpec().setType("citus");
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getSuperuserUsername()).thenReturn(Optional.empty());
    lenient().when(context.getSuperuserPassword()).thenReturn(Optional.of("test-pass"));
    lenient().when(context.getDatabaseSecret()).thenReturn(Optional.empty());

    StackGresScript script = (StackGresScript) factory.generateResource(context).toList().getFirst();

    List<StackGresScriptEntry> entries = script.getSpec().getScripts();
    assertEquals(List.of(0, 1, 2, 3, 4),
        entries.stream().map(StackGresScriptEntry::getId).toList());
    assertEquals(List.of("citus-update-workers", "citus-remove-pg-cron-jobs", "citus-update-nodes",
        "citus-query-routers-without-shards", "citus-registered-groups"),
        entries.stream().map(StackGresScriptEntry::getName).toList());
    assertNull(entries.get(0).getCron());
    assertNull(entries.get(1).getCron());
    assertTrue(entries.get(1).getScript().contains("cron.unschedule"));
    assertEquals("0/10 * * * * ?", entries.get(2).getCron());
    assertEquals(cluster.getSpec().getDatabase(), entries.get(2).getDatabase());
    assertTrue(entries.get(2).getScript().contains("IF false THEN"));
    assertFalse(entries.get(2).getScript().contains("%"));
    assertEquals("0/10 * * * * ?", entries.get(3).getCron());
    assertTrue(entries.get(3).getSetValueOrDefault());
    assertEquals(cluster.getSpec().getDatabase(), entries.get(3).getDatabase());
    assertTrue(entries.get(3).getScript().contains("NOT shouldhaveshards"));
    assertEquals(
        StackGresShardedClusterForCitusUtil.QUERY_ROUTERS_WITHOUT_SHARDS_SCRIPT_ID,
        entries.get(3).getId());
    assertEquals("0/10 * * * * ?", entries.get(4).getCron());
    assertTrue(entries.get(4).getSetValueOrDefault());
    assertEquals(cluster.getSpec().getDatabase(), entries.get(4).getDatabase());
    assertTrue(entries.get(4).getScript().contains("pg_dist_node"));
    assertEquals(
        StackGresShardedClusterForCitusUtil.REGISTERED_GROUPS_SCRIPT_ID,
        entries.get(4).getId());
  }

  @Test
  void generateResource_whenCitusConfigurationsAreSet_shouldUseThem() {
    cluster.getSpec().setType("citus");
    cluster.getSpec().setConfigurations(new StackGresShardedClusterConfigurations());
    cluster.getSpec().getConfigurations().setCitus(new StackGresShardedClusterCitusConfigurations());
    cluster.getSpec().getConfigurations().getCitus().setUpdateNodeInterval("PT2M");
    cluster.getSpec().getConfigurations().getCitus().setEnableNodeAutoRemoval(true);
    when(context.getShardedCluster()).thenReturn(cluster);
    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getSuperuserUsername()).thenReturn(Optional.empty());
    lenient().when(context.getSuperuserPassword()).thenReturn(Optional.of("test-pass"));
    lenient().when(context.getDatabaseSecret()).thenReturn(Optional.empty());

    StackGresScript script = (StackGresScript) factory.generateResource(context).toList().getFirst();

    List<StackGresScriptEntry> entries = script.getSpec().getScripts();
    assertEquals("0 0/2 * * * ?", entries.get(2).getCron());
    assertTrue(entries.get(2).getScript().contains("IF true THEN"));
    assertEquals("0 0/2 * * * ?", entries.get(3).getCron());
    assertEquals("0 0/2 * * * ?", entries.get(4).getCron());
  }

}
