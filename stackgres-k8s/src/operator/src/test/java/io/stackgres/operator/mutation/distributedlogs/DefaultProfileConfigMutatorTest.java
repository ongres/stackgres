/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.configuration.OperatorPropertyContext;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutatorTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultProfileConfigMutatorTest
    extends AbstractDefaultResourceMutatorTest<StackGresProfile, StackGresDistributedLogs,
        StackGresDistributedLogsReview, DefaultProfileMutator> {

  @Override
  protected StackGresDistributedLogsReview getAdmissionReview() {
    return AdmissionReviewFixtures.distributedLogs().loadCreate().get();
  }

  @Override
  protected DefaultProfileMutator getDefaultConfigMutator() {
    var resourceFactory = new DefaultProfileFactory(new OperatorPropertyContext());
    resourceFactory.init();
    var mutator = new DefaultProfileMutator(
        resourceFactory, finder, scheduler);
    mutator.init();
    return mutator;
  }

  @Override
  protected Class<StackGresDistributedLogs> getResourceClass() {
    return StackGresDistributedLogs.class;
  }

  @Override
  protected void checkConfigurationIsSet(StackGresDistributedLogs newResource) {
    assertNotNull(newResource.getSpec().getResourceProfile());
  }

  @Override
  protected void setUpExistingConfiguration() {
    // Nothing to do.
  }

  @Override
  protected void setUpMissingConfiguration() {
    review.getRequest().getObject().getSpec().setResourceProfile(null);
  }

  @Override
  protected void setUpMissingConfigurationSection() {
    review.getRequest().getObject().getSpec().setConfiguration(null);
  }

}
