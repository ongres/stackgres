/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.initialization.DefaultPostgresFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPostgresConfigMutatorTest
    extends AbstractDefaultResourceMutatorTest<StackGresPostgresConfig, StackGresCluster,
        StackGresClusterReview, DefaultPostgresMutator> {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  @Override
  protected StackGresClusterReview getAdmissionReview() {
    return AdmissionReviewFixtures.cluster().loadCreate().get();
  }

  @Override
  protected DefaultPostgresMutator getDefaultConfigMutator() {
    var resourceFactory = new DefaultPostgresFactory(new OperatorPropertyContext());
    resourceFactory.init();
    var mutator = new DefaultPostgresMutator(
        resourceFactory, finder, scheduler);
    return mutator;
  }

  @Override
  protected Class<StackGresCluster> getResourceClass() {
    return StackGresCluster.class;
  }

  @Override
  protected void checkConfigurationIsSet(StackGresCluster newResource) {
    assertNotNull(newResource.getSpec().getConfigurations());
    assertNotNull(newResource.getSpec().getConfigurations().getSgPostgresConfig());
  }

  @Override
  protected void setUpExistingConfiguration() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
  }

  @Override
  protected void setUpMissingConfiguration() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    review.getRequest().getObject().getSpec().getConfigurations()
        .setSgPostgresConfig(null);
  }

  @Override
  protected void setUpMissingConfigurationSection() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    review.getRequest().getObject().getSpec().setConfigurations(null);
  }

}
