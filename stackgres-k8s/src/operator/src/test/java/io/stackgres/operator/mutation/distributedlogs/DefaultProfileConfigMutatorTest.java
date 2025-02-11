/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import io.stackgres.operator.mutation.AbstractDefaultResourceMutatorTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultProfileConfigMutatorTest
    extends AbstractDefaultResourceMutatorTest<StackGresProfile, HasMetadata, StackGresDistributedLogs,
        StackGresDistributedLogsReview, DefaultProfileMutator> {

  @Override
  protected StackGresDistributedLogsReview getAdmissionReview() {
    return AdmissionReviewFixtures.distributedLogs().loadCreate().get();
  }

  @Override
  protected DefaultProfileMutator getDefaultConfigMutator() {
    var resourceFactory = new DefaultProfileFactory();
    var mutator = new DefaultProfileMutator(
        resourceFactory, finder, scheduler);
    return mutator;
  }

  @Override
  protected Class<StackGresDistributedLogs> getResourceClass() {
    return StackGresDistributedLogs.class;
  }

  @Override
  protected void checkConfigurationIsSet(StackGresDistributedLogs newResource) {
    assertNotNull(newResource.getSpec().getSgInstanceProfile());
  }

  @Override
  protected void setUpExistingConfiguration() {
    // Nothing to do.
  }

  @Override
  protected void setUpMissingConfiguration() {
    review.getRequest().getObject().getSpec().setSgInstanceProfile(null);
  }

  @Override
  protected void setUpMissingConfigurationSection() {
    // Nothing to do.
  }

  @Test
  @Disabled("There is no configuration section for profile")
  @Override
  protected void clusteWithNoConfigurationSection_shouldSetOne() throws Exception {
    super.clusteWithNoConfigurationSection_shouldSetOne();
  }

}
