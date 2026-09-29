/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryScriptStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterPatroni;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class ShardedClustersTest {

  @Mock
  private StackGresShardedClusterContext context;

  private LabelFactoryForShardedCluster labelFactory;
  private StackGresConfig config;
  private StackGresShardedCluster shardedCluster;
  private StackGresCluster coordinator;
  private StackGresCluster worker0;
  private StackGresCluster worker1;
  private ShardedClusters shardedClusters;

  @BeforeEach
  public void setup() {
    openMocks(this);
    config = Fixtures.config().loadDefault().get();
    shardedCluster = Fixtures.shardedCluster().loadDefault().get();

    labelFactory = new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());
    coordinator = Fixtures.cluster().loadDefault().get();
    worker0 = Fixtures.cluster().loadDefault().get();
    worker1 = Fixtures.cluster().loadDefault().get();
    shardedClusters = new ShardedClusters(labelFactory);
    when(context.getConfig()).thenReturn(config);
    when(context.getSource()).thenReturn(shardedCluster);
    when(context.getShardedCluster()).thenReturn(shardedCluster);
    when(context.getCoordinator()).thenReturn(coordinator);
    when(context.getWorkers()).thenReturn(List.of(worker0, worker1));
  }

  @Test
  public void generateShardedClusters_shouldSetLabels() {
    var clusters = shardedClusters.generateResource(context).toList();
    assertEquals(3, clusters.size());
    assertEquals(labelFactory.coordinatorLabels(shardedCluster),
        clusters.getFirst().getMetadata().getLabels());
    assertEquals(labelFactory.workersLabels(shardedCluster),
        clusters.get(1).getMetadata().getLabels());
    assertEquals(labelFactory.workersLabels(shardedCluster),
        clusters.get(2).getMetadata().getLabels());
  }

  @Test
  public void generateResource_whenEmptyWorkers_shouldGenerateOnlyCoordinator() {
    when(context.getWorkers()).thenReturn(List.of());
    var clusters = shardedClusters.generateResource(context).toList();
    assertEquals(1, clusters.size());
    assertEquals(labelFactory.coordinatorLabels(shardedCluster),
        clusters.getFirst().getMetadata().getLabels());
  }

  @Test
  public void generateResource_whenDifferentTopology_shouldReflectInClusters() {
    shardedCluster.getSpec().setType("ddp");
    var clusters = shardedClusters.generateResource(context).toList();
    assertEquals(3, clusters.size());
    assertEquals(labelFactory.coordinatorLabels(shardedCluster),
        clusters.getFirst().getMetadata().getLabels());
    assertEquals(labelFactory.workersLabels(shardedCluster),
        clusters.get(1).getMetadata().getLabels());
    assertEquals(labelFactory.workersLabels(shardedCluster),
        clusters.get(2).getMetadata().getLabels());
  }

  @Test
  public void generateResource_whenQueryRouterIsRegisteredByTheCoordinator_shouldSetStartGateAnnotation() {
    var queryRouter = queryRouterWithStartGate(1025);
    when(context.getQueryRouters()).thenReturn(List.of(queryRouter));
    when(context.getDeployedCoordinator()).thenReturn(
        Optional.of(deployedCoordinator("1025,1026")));

    var clusters = shardedClusters.generateResource(context).toList();

    assertEquals("1025", clusters.getLast().getMetadata().getAnnotations()
        .get(StackGresContext.CITUS_GROUP_REGISTERED_ANNOTATION));
    assertEquals(labelFactory.queryRoutersLabels(shardedCluster),
        clusters.getLast().getMetadata().getLabels());
  }

  @Test
  public void generateResource_whenQueryRouterIsNotRegisteredByTheCoordinator_shouldNotSetStartGateAnnotation() {
    var queryRouter = queryRouterWithStartGate(1027);
    when(context.getQueryRouters()).thenReturn(List.of(queryRouter));
    when(context.getDeployedCoordinator()).thenReturn(
        Optional.of(deployedCoordinator("1025,1026")));

    var clusters = shardedClusters.generateResource(context).toList();

    assertFalse(Optional.ofNullable(clusters.getLast().getMetadata().getAnnotations())
        .orElse(Map.of())
        .containsKey(StackGresContext.CITUS_GROUP_REGISTERED_ANNOTATION));
  }

  @Test
  public void generateResource_whenCoordinatorIsNotDeployed_shouldNotSetStartGateAnnotation() {
    var queryRouter = queryRouterWithStartGate(1025);
    when(context.getQueryRouters()).thenReturn(List.of(queryRouter));
    when(context.getDeployedCoordinator()).thenReturn(Optional.empty());

    var clusters = shardedClusters.generateResource(context).toList();

    assertFalse(Optional.ofNullable(clusters.getLast().getMetadata().getAnnotations())
        .orElse(Map.of())
        .containsKey(StackGresContext.CITUS_GROUP_REGISTERED_ANNOTATION));
  }

  private StackGresCluster queryRouterWithStartGate(int group) {
    var queryRouter = Fixtures.cluster().loadDefault().get();
    queryRouter.getSpec().setConfigurations(new StackGresClusterConfigurations());
    queryRouter.getSpec().getConfigurations().setPatroni(new StackGresClusterPatroni());
    queryRouter.getSpec().getConfigurations().getPatroni().setStartGateAnnotations(Map.of(
        StackGresContext.CITUS_GROUP_REGISTERED_ANNOTATION, String.valueOf(group)));
    return queryRouter;
  }

  private StackGresCluster deployedCoordinator(String registeredGroups) {
    var deployedCoordinator = Fixtures.cluster().loadDefault().get();
    deployedCoordinator.getSpec().setManagedSql(new StackGresClusterManagedSql());
    deployedCoordinator.getSpec().getManagedSql().setScripts(List.of(
        new StackGresClusterManagedScriptEntryBuilder()
        .withId(1)
        .withSgScript(StackGresShardedClusterUtil.coordinatorScriptName(shardedCluster))
        .build()));
    if (deployedCoordinator.getStatus() == null) {
      deployedCoordinator.setStatus(new StackGresClusterStatus());
    }
    var scriptStatus = new StackGresClusterManagedScriptEntryScriptStatus();
    scriptStatus.setId(StackGresShardedClusterForCitusUtil.QUERY_ROUTERS_WITHOUT_SHARDS_SCRIPT_ID);
    scriptStatus.setVersion(0);
    scriptStatus.setValue(registeredGroups);
    var managedScriptStatus = new StackGresClusterManagedScriptEntryStatus();
    managedScriptStatus.setId(1);
    managedScriptStatus.setScripts(List.of(scriptStatus));
    deployedCoordinator.getStatus().setManagedSql(new StackGresClusterManagedSqlStatus());
    deployedCoordinator.getStatus().getManagedSql().setScripts(List.of(managedScriptStatus));
    return deployedCoordinator;
  }

}
