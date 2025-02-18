/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.initialization.DefaultDistributedLogsPostgresConfigFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPostgresConfigMutatorTest
    extends AbstractDefaultResourceMutatorTest<StackGresPostgresConfig, StackGresDistributedLogs,
        StackGresDistributedLogs, StackGresDistributedLogsReview, DefaultPostgresConfigMutator> {

  @Override
  protected StackGresDistributedLogsReview getAdmissionReview() {
    return AdmissionReviewFixtures.distributedLogs().loadCreate().get();
  }

  @Override
  protected DefaultPostgresConfigMutator getDefaultConfigMutator() {
    var resourceFactory = new DefaultDistributedLogsPostgresConfigFactory();
    var mutator = new DefaultPostgresConfigMutator(
        resourceFactory);
    return mutator;
  }

  @Override
  protected Class<StackGresDistributedLogs> getResourceClass() {
    return StackGresDistributedLogs.class;
  }

  @Override
  protected void checkConfigurationIsSet(StackGresDistributedLogs newResource) {
    assertNotNull(newResource.getSpec().getConfigurations());
    assertNotNull(newResource.getSpec().getConfigurations().getSgPostgresConfig());
  }

  @Override
  protected void setUpExistingConfiguration() {
    // Nothing to do.
  }

  @Override
  protected void setUpMissingConfiguration() {
    review.getRequest().getObject().getSpec().getConfigurations()
        .setSgPostgresConfig(null);
  }

  @Override
  protected void setUpMissingConfigurationSection() {
    review.getRequest().getObject().getSpec().setConfigurations(null);
  }

}
