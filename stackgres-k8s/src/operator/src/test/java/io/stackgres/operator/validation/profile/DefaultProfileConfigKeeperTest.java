/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.DefaultKeeperTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultProfileConfigKeeperTest extends DefaultKeeperTest<StackGresProfile, SgProfileReview> {

  @Override
  protected AbstractDefaultConfigKeeper<StackGresProfile, SgProfileReview> getValidatorInstance() {
    return new DefaultProfileConfigKeeper();
  }

  @Override
  protected SgProfileReview getCreationSample() {
    return AdmissionReviewFixtures.instanceProfile().loadCreate().get();
  }

  @Override
  protected SgProfileReview getDeleteSample() {
    return AdmissionReviewFixtures.instanceProfile().loadDelete().get();
  }

  @Override
  protected SgProfileReview getUpdateSample() {
    return AdmissionReviewFixtures.instanceProfile().loadUpdate().get();
  }

}
