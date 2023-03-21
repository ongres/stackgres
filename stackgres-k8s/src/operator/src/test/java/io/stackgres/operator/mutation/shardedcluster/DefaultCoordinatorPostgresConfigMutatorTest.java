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
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.initialization.DefaultPostgresFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultCoordinatorPostgresConfigMutatorTest
    extends AbstractDefaultResourceMutatorTest<StackGresPostgresConfig, StackGresShardedCluster,
        StackGresShardedClusterReview, DefaultCoordinatorPostgresMutator> {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  @Override
  protected StackGresShardedClusterReview getAdmissionReview() {
    return AdmissionReviewFixtures.shardedCluster().loadCreate().get();
  }

  @Override
  protected DefaultCoordinatorPostgresMutator getDefaultConfigMutator() {
    var resourceFactory = new DefaultPostgresFactory(new OperatorPropertyContext());
    resourceFactory.init();
    var mutator = new DefaultCoordinatorPostgresMutator(
        resourceFactory, finder, scheduler);
    mutator.init();
    return mutator;
  }

  @Override
  protected Class<StackGresShardedCluster> getResourceClass() {
    return StackGresShardedCluster.class;
  }

  @Override
  protected void checkConfigurationIsSet(StackGresShardedCluster newResource) {
    assertNotNull(newResource.getSpec().getCoordinator().getConfiguration());
    assertNotNull(newResource.getSpec().getCoordinator().getConfiguration().getPostgresConfig());
  }

  @Override
  protected void setUpExistingConfiguration() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
  }

  @Override
  protected void setUpMissingConfiguration() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    review.getRequest().getObject().getSpec().getCoordinator().getConfiguration()
        .setPostgresConfig(null);
  }

  @Override
  protected void setUpMissingConfigurationSection() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    review.getRequest().getObject().getSpec().getCoordinator().setConfiguration(null);
  }

}
