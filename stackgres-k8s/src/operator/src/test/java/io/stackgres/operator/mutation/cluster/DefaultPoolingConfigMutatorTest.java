/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.initialization.DefaultPoolingConfigFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPoolingConfigMutatorTest
    extends AbstractDefaultResourceMutatorTest<StackGresPoolingConfig, HasMetadata, StackGresCluster,
        StackGresClusterReview, DefaultPoolingConfigMutator> {

  private static final String POSTGRES_VERSION =
      StackGresComponent.POSTGRESQL.getLatest().streamOrderedVersions().findFirst().get();

  @Override
  protected StackGresClusterReview getAdmissionReview() {
    return AdmissionReviewFixtures.cluster().loadCreate().get();
  }

  @Override
  protected DefaultPoolingConfigMutator getDefaultConfigMutator() {
    var resourceFactory = new DefaultPoolingConfigFactory();
    var mutator = new DefaultPoolingConfigMutator(
        resourceFactory);
    return mutator;
  }

  @Override
  protected Class<StackGresCluster> getResourceClass() {
    return StackGresCluster.class;
  }

  @Override
  protected void checkConfigurationIsSet(StackGresCluster newResource) {
    assertNotNull(newResource.getSpec().getConfigurations());
    assertNotNull(newResource.getSpec().getConfigurations()
        .getSgPoolingConfig());
  }

  @Override
  protected void setUpExistingConfiguration() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
  }

  @Override
  protected void setUpMissingConfiguration() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    review.getRequest().getObject().getSpec().getConfigurations()
        .setSgPoolingConfig(null);
  }

  @Override
  protected void setUpMissingConfigurationSection() {
    review.getRequest().getObject().getSpec().getPostgres().setVersion(POSTGRES_VERSION);
    review.getRequest().getObject().getSpec().setConfigurations(null);
  }

}
