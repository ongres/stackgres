/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.common.StackGresShardedClusterForCitusUtil;
import io.stackgres.operator.conciliation.AbstractRequiredResourceGeneratorTest;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.cluster.ImmutableStackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
@WithKubernetesTestServer
class ShardedClusterRequiredResourceDecoratorTest
    extends AbstractRequiredResourceGeneratorTest<StackGresShardedClusterContext> {

  @Inject
  ShardedClusterResourceGenerationDiscoverer resourceGenerationDiscoverer;

  private StackGresConfig config;
  private StackGresShardedCluster resource;
  private StackGresPostgresConfig pgConfig;
  private StackGresProfile profile;
  private Optional<StackGresPoolingConfig> pooling;

  @BeforeEach
  public void setup() {
    this.config = Fixtures.config().loadDefault().get();
    this.resource = Fixtures.shardedCluster().loadDefault().withLatestPostgresVersion().get();
    this.pgConfig = Fixtures.postgresConfig().loadDefault().get();
    this.profile = Fixtures.instanceProfile().loadSizeM().get();
    this.pooling = ofNullable(Fixtures.poolingConfig().loadDefault().get());
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
        .coordinator(getCoordinatorContext())
        .shards(getShardsContext())
        .build();
  }

  private StackGresClusterContext getCoordinatorContext() {
    return ImmutableStackGresClusterContext.builder()
        .config(config)
        .source(StackGresShardedClusterForCitusUtil.getCoordinatorCluster(resource))
        .postgresConfig(pgConfig)
        .profile(profile)
        .poolingConfig(pooling)
        .prometheus(new Prometheus(false, null))
        .build();
  }

  private List<StackGresClusterContext> getShardsContext() {
    return Seq.range(0, resource.getSpec().getShards().getClusters())
        .<StackGresClusterContext>map(index -> ImmutableStackGresClusterContext.builder()
            .config(config)
            .source(StackGresShardedClusterForCitusUtil.getShardsCluster(resource, index))
            .postgresConfig(pgConfig)
            .profile(profile)
            .poolingConfig(pooling)
            .build())
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
