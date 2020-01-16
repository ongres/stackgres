/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import io.stackgres.operator.utils.JsonUtil;
import io.stackgres.operator.common.RestoreConfigReview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestoreConfigMutatorTest extends MutationResourceTest<RestoreConfigReview> {

  @BeforeEach
  void setUp() {
    resource = new RestoreConfigMutator(pipeline);

    review = JsonUtil
        .readFromJson("restore_config_allow_request/create.json", RestoreConfigReview.class);
  }


}