/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.common.StackGresPoolingConfigReview;
import io.stackgres.operator.common.fixture.AdmissionReviewFixtures;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.DefaultKeeperTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPoolingConfigKeeperTest
    extends DefaultKeeperTest<StackGresPoolingConfig, StackGresPoolingConfigReview> {

  @Override
  protected AbstractDefaultConfigKeeper<StackGresPoolingConfig, StackGresPoolingConfigReview>
      getValidatorInstance() {
    return new DefaultPoolingConfigKeeper();
  }

  @Override
  protected StackGresPoolingConfigReview getCreationSample() {
    return AdmissionReviewFixtures.poolingConfig().loadCreate().get();
  }

  @Override
  protected StackGresPoolingConfigReview getDeleteSample() {
    return AdmissionReviewFixtures.poolingConfig().loadDelete().get();
  }

  @Override
  protected StackGresPoolingConfigReview getUpdateSample() {
    return AdmissionReviewFixtures.poolingConfig().loadUpdate().get();
  }

}
