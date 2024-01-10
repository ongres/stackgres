/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.AbstractRequiredResourceGeneratorTest;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
@WithKubernetesTestServer
class ShardedClusterRequiredResourceDecoratorTest
    extends AbstractRequiredResourceGeneratorTest<StackGresShardedClusterContext> {

  @Inject
  ShardedClusterResourceGenerationDiscoverer resourceGenerationDiscoverer;

  private StackGresShardedCluster resource;
  private StackGresPostgresConfig pgConfig;

  @BeforeEach
  public void setup() {
    this.resource = Fixtures.shardedCluster().loadDefault().withLatestPostgresVersion().get();
    this.pgConfig = Fixtures.postgresConfig().loadDefault().get();
  }

  @Override
  protected ResourceGenerationDiscoverer<StackGresShardedClusterContext>
      getResourceGenerationDiscoverer() {
    return this.resourceGenerationDiscoverer;
  }

  @Override
  protected StackGresShardedClusterContext getResourceContext() {
    return ImmutableStackGresShardedClusterContext.builder()
        .source(resource)
        .coordinatorConfig(pgConfig)
        .coordinator(getCoordinator())
        .shards(getShards())
        .build();
  }

  private StackGresCluster getCoordinator() {
    return StackGresShardedClusterForCitusUtil.getCoordinatorCluster(resource);
  }

  private List<StackGresCluster> getShards() {
    return Seq.range(0, resource.getSpec().getShards().getClusters())
        .map(index -> StackGresShardedClusterForCitusUtil.getShardsCluster(resource, index))
        .toList();
  }

  @Override
  protected String usingKind() {
    return StackGresShardedCluster.KIND;
  }

  @Override
  protected HasMetadata getResource() {
    return resource;
  }

}
