/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.profile.ProfileDto;
import io.stackgres.testutil.fixture.Fixture;

public class InstanceProfileDtoFixture extends Fixture<ProfileDto> {

  public InstanceProfileDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_INSTANCE_PROFILE_DTO_JSON);
    return this;
  }

}
