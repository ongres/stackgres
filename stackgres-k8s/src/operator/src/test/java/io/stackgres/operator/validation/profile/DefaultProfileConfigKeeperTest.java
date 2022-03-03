/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.profile;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.common.SgProfileReview;
import io.stackgres.operator.validation.AbstractDefaultConfigKeeper;
import io.stackgres.operator.validation.DefaultKeeperTest;
import io.stackgres.testutil.JsonUtil;
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
    return JsonUtil
        .readFromJson("sgprofile_allow_request/create.json", SgProfileReview.class);
  }

  @Override
  protected SgProfileReview getDeleteSample() {
    return JsonUtil
        .readFromJson("sgprofile_allow_request/delete.json", SgProfileReview.class);
  }

  @Override
  protected SgProfileReview getUpdateSample() {
    return JsonUtil
        .readFromJson("sgprofile_allow_request/update.json", SgProfileReview.class);
  }

}
