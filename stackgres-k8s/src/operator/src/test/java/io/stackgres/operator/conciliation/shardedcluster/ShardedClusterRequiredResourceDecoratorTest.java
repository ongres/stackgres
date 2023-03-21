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
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.common.Prometheus;
import io.stackgres.operator.conciliation.AbstractRequiredResourceDecoratorTest;
import io.stackgres.operator.conciliation.RequiredResourceDecorator;
import io.stackgres.operator.conciliation.cluster.ImmutableStackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;

@QuarkusTest
@WithKubernetesTestServer
class ShardedClusterRequiredResourceDecoratorTest
    extends AbstractRequiredResourceDecoratorTest<StackGresShardedClusterContext> {

  @Inject
  ShardedClusterRequiredResourceDecorator resourceDecorator;

  private StackGresShardedCluster resource;
  private StackGresPostgresConfig pgConfig;
  private StackGresProfile profile;
  private Optional<StackGresPoolingConfig> pooling;

  @BeforeEach
  public void setup() {
    this.resource = Fixtures.shardedCluster().loadDefault().withLatestPostgresVersion().get();
    this.pgConfig = Fixtures.postgresConfig().loadDefault().get();
    this.profile = Fixtures.instanceProfile().loadSizeS().get();
    this.pooling = ofNullable(Fixtures.poolingConfig().loadDefault().get());
  }

  @Override
  protected RequiredResourceDecorator<StackGresShardedClusterContext> getResourceDecorator() {
    return this.resourceDecorator;
  }

  @Override
  protected StackGresShardedClusterContext getResourceContext() {
    return ImmutableStackGresShardedClusterContext.builder()
        .source(resource)
        .coordinator(getCoordinatorContext())
        .shards(getShardsContext())
        .build();
  }

  private StackGresClusterContext getCoordinatorContext() {
    return ImmutableStackGresClusterContext.builder()
        .source(StackGresShardedClusterUtil.getCoordinatorCluster(resource))
        .postgresConfig(pgConfig)
        .profile(profile)
        .poolingConfig(pooling)
        .prometheus(new Prometheus(false, null))
        .build();
  }

  private List<StackGresClusterContext> getShardsContext() {
    return Seq.range(0, resource.getSpec().getShards().getClusters())
        .<StackGresClusterContext>map(index -> ImmutableStackGresClusterContext.builder()
            .source(StackGresShardedClusterUtil.getShardsCluster(resource, index))
            .postgresConfig(pgConfig)
            .profile(profile)
            .poolingConfig(pooling)
            .build())
        .toList();
  }

  @Override
  protected String usingCrdFilename() {
    return "SGShardedCluster.yaml";
  }

  @Override
  protected HasMetadata getResource() {
    return resource;
  }

}
