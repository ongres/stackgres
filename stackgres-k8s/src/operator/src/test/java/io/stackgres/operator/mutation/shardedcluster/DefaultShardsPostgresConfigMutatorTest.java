/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.initialization.DefaultShardedClusterPostgresConfigFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultShardsPostgresConfigMutatorTest
    extends AbstractDefaultResourceMutatorTest<StackGresPostgresConfig,
        StackGresShardedCluster, StackGresShardedCluster,
        StackGresShardedClusterReview, DefaultShardsPostgresConfigMutator> {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  @Override
  protected StackGresShardedClusterReview getAdmissionReview() {
    return AdmissionReviewFixtures.shardedCluster().loadCreate().get();
  }

  @Override
  protected DefaultShardsPostgresConfigMutator getDefaultConfigMutator() {
    var resourceFactory = new DefaultShardedClusterPostgresConfigFactory();
    var mutator = new DefaultShardsPostgresConfigMutator(
        resourceFactory);
    return mutator;
  }

  @Override
  protected Class<StackGresShardedCluster> getResourceClass() {
    return StackGresShardedCluster.class;
  }

  @Override
  protected void checkConfigurationIsSet(StackGresShardedCluster newResource) {
    assertNotNull(newResource.getSpec().getShards());
    assertNotNull(newResource.getSpec().getShards().getConfigurations());
    assertNotNull(newResource.getSpec().getShards().getConfigurations().getSgPostgresConfig());
  }

  @Override
  protected void setUpExistingConfiguration() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
  }

  @Override
  protected void setUpMissingConfiguration() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    review.getRequest().getObject().getSpec().getShards().getConfigurations()
        .setSgPostgresConfig(null);
  }

  @Override
  protected void setUpMissingConfigurationSection() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    review.getRequest().getObject().getSpec().setShards(null);
  }

}
