/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.initialization;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.utils.JsonUtil;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileInitializerTest extends AbstractInitializerTest<StackGresProfile> {

  @Override
  AbstractDefaultCustomResourceInitializer<StackGresProfile> getInstance() {
    return new ProfileInitializer();
  }

  @Override
  StackGresProfile getDefaultCR() {
    return JsonUtil
        .readFromJson("stackgres_profiles/size-s.json", StackGresProfile.class);
  }
}