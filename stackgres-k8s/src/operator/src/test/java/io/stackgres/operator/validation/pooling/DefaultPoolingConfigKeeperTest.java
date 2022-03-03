/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.pooling;

import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.operator.common.PoolingReview;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.DefaultKeeperTest;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultPoolingConfigKeeperTest
    extends DefaultKeeperTest<StackGresPoolingConfig, PoolingReview> {

  @Override
  protected AbstractDefaultConfigKeeper<StackGresPoolingConfig, PoolingReview>
      getValidatorInstance() {
    return new DefaultPoolingConfigKeeper();
  }

  @Override
  protected PoolingReview getCreationSample() {
    return JsonUtil.readFromJson("pooling_allow_request/create.json",
        PoolingReview.class);
  }

  @Override
  protected PoolingReview getDeleteSample() {
    return JsonUtil.readFromJson("pooling_allow_request/delete.json",
        PoolingReview.class);
  }

  @Override
  protected PoolingReview getUpdateSample() {
    return JsonUtil.readFromJson("pooling_allow_request/update.json",
        PoolingReview.class);
  }

}
