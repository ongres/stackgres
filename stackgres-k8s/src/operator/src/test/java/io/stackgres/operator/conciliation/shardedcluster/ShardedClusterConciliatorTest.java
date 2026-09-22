/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
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

  private ShardedClusterConciliator conciliator;

  private StackGresShardedCluster shardedCluster;

  @BeforeEach
  void setUp() {
    conciliator = new ShardedClusterConciliator(
        client, finder, requiredResourceGenerator, deployedResourcesScanner,
        deployedResourcesCache, labelFactory);
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
}
