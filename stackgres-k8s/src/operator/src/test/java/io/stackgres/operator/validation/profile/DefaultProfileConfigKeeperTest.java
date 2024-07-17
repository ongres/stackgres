/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.StackGresInstanceProfileReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.DefaultKeeperTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultProfileConfigKeeperTest extends DefaultKeeperTest<StackGresProfile, StackGresInstanceProfileReview> {

  @Override
  protected AbstractDefaultConfigKeeper<StackGresProfile, StackGresInstanceProfileReview> getValidatorInstance() {
    return new DefaultProfileConfigKeeper();
  }

  @Override
  protected StackGresInstanceProfileReview getCreationSample() {
    return AdmissionReviewFixtures.instanceProfile().loadCreate().get();
  }

  @Override
  protected StackGresInstanceProfileReview getDeleteSample() {
    return AdmissionReviewFixtures.instanceProfile().loadDelete().get();
  }

  @Override
  protected StackGresInstanceProfileReview getUpdateSample() {
    return AdmissionReviewFixtures.instanceProfile().loadUpdate().get();
  }

}
