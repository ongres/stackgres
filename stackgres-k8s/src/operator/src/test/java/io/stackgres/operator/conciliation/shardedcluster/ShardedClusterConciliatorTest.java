/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryScriptStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntryStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSqlStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterConciliatorTest {

  private final LabelFactoryForShardedCluster labelFactory =
      new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());

  @Mock
  private KubernetesClient client;

  @Mock
  private CustomResourceFinder<StackGresShardedCluster> finder;

  @Mock
  private RequiredResourceGenerator<StackGresShardedCluster> requiredResourceGenerator;

  @Mock
  private AbstractDeployedResourcesScanner<StackGresShardedCluster> deployedResourcesScanner;

  @Mock
  private DeployedResourcesCache deployedResourcesCache;

  @Mock
  private CustomResourceFinder<StackGresCluster> clusterFinder;

  private ShardedClusterConciliator conciliator;

  private StackGresShardedCluster shardedCluster;

  @BeforeEach
  void setUp() {
    conciliator = new ShardedClusterConciliator(
        client, finder, requiredResourceGenerator, deployedResourcesScanner,
        deployedResourcesCache, labelFactory, clusterFinder);
    shardedCluster = Fixtures.shardedCluster().loadDefault().get();
  }

  private <T extends HasMetadata> T withDefaultConfigLabels(T resource) {
    resource.getMetadata().setLabels(labelFactory.defaultConfigLabels(shardedCluster));
    return resource;
  }

  @Test
  void givenALease_shouldSkipDeletion() {
    var lease = new LeaseBuilder()
        .withNewMetadata()
        .withNamespace(shardedCluster.getMetadata().getNamespace())
        .withName(shardedCluster.getMetadata().getName())
        .endMetadata()
        .build();

    assertTrue(conciliator.skipDeletion(lease, shardedCluster),
        "the Lease reconciliation handler never deletes a Lease, so reporting the deletion"
            + " would make the reconciliation never converge");
  }

  @Test
  void givenADefaultPostgresConfig_shouldSkipDeletion() {
    var postgresConfig = withDefaultConfigLabels(new StackGresPostgresConfig());
    assertTrue(conciliator.skipDeletion(postgresConfig, shardedCluster));
  }

  @Test
  void givenADefaultInstanceProfile_shouldSkipDeletion() {
    var profile = withDefaultConfigLabels(new StackGresInstanceProfile());
    assertTrue(conciliator.skipDeletion(profile, shardedCluster));
  }

  @Test
  void givenADefaultPoolingConfig_shouldSkipDeletion() {
    var poolingConfig = withDefaultConfigLabels(new StackGresPoolingConfig());
    assertTrue(conciliator.skipDeletion(poolingConfig, shardedCluster));
  }

  @Test
  void givenAUserProvidedPostgresConfig_shouldNotSkipDeletion() {
    var postgresConfig = new StackGresPostgresConfig();
    postgresConfig.getMetadata().setLabels(labelFactory.genericLabels(shardedCluster));

    assertFalse(conciliator.skipDeletion(postgresConfig, shardedCluster),
        "a config that is not a default one is deleted by the handler and must be reported");
  }

  @Test
  void givenAnyOtherResource_shouldNotSkipDeletion() {
    var configMap = new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(shardedCluster.getMetadata().getNamespace())
        .withName(shardedCluster.getMetadata().getName())
        .endMetadata()
        .build();

    assertFalse(conciliator.skipDeletion(configMap, shardedCluster));
  }

  @Test
  void givenAChildClusterScaledToZero_shouldSkipDeletion() {
    var cluster = Fixtures.cluster().loadDefault().get();
    cluster.getSpec().setInstances(0);

    assertTrue(conciliator.skipDeletion(cluster, shardedCluster));
  }

  @Test
  void givenAChildClusterRegisteredInCitus_shouldSkipDeletion() {
    when(clusterFinder.findByNameAndNamespace(
        StackGresShardedClusterUtil.getCoordinatorClusterName(shardedCluster),
        shardedCluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(deployedCoordinator("1,2,1025")));

    assertTrue(conciliator.skipDeletion(childCluster(
        StackGresShardedClusterUtil.getWorkerClusterName(shardedCluster, 1)), shardedCluster));
    assertTrue(conciliator.skipDeletion(childCluster(
        StackGresShardedClusterUtil.getQueryRouterClusterName(shardedCluster, "0")),
        shardedCluster));
  }

  @Test
  void givenAChildClusterNotRegisteredInCitus_shouldNotSkipDeletion() {
    when(clusterFinder.findByNameAndNamespace(
        StackGresShardedClusterUtil.getCoordinatorClusterName(shardedCluster),
        shardedCluster.getMetadata().getNamespace()))
        .thenReturn(Optional.of(deployedCoordinator("1,2,1025")));

    assertFalse(conciliator.skipDeletion(childCluster(
        StackGresShardedClusterUtil.getWorkerClusterName(shardedCluster, 2)), shardedCluster));
    assertFalse(conciliator.skipDeletion(childCluster(
        StackGresShardedClusterUtil.getQueryRouterClusterName(shardedCluster, "1")),
        shardedCluster));
  }

  @Test
  void givenAChildClusterWithoutRegisteredGroupsReported_shouldNotSkipDeletion() {
    when(clusterFinder.findByNameAndNamespace(
        StackGresShardedClusterUtil.getCoordinatorClusterName(shardedCluster),
        shardedCluster.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());

    assertFalse(conciliator.skipDeletion(childCluster(
        StackGresShardedClusterUtil.getWorkerClusterName(shardedCluster, 1)), shardedCluster));
  }

  @Test
  void givenAChildClusterOfANonCitusShardedCluster_shouldNotSkipDeletion() {
    shardedCluster.getSpec().setType("ddp");

    assertFalse(conciliator.skipDeletion(childCluster(
        StackGresShardedClusterUtil.getWorkerClusterName(shardedCluster, 1)), shardedCluster));
    verify(clusterFinder, never()).findByNameAndNamespace(any(), any());
  }

  private StackGresCluster childCluster(String name) {
    var cluster = Fixtures.cluster().loadDefault().get();
    cluster.getMetadata().setNamespace(shardedCluster.getMetadata().getNamespace());
    cluster.getMetadata().setName(name);
    cluster.getSpec().setInstances(2);
    return cluster;
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
    scriptStatus.setId(StackGresShardedClusterForCitusUtil.REGISTERED_GROUPS_SCRIPT_ID);
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
