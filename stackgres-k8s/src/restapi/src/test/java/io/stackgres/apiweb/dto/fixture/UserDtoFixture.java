/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.user.UserDto;
import io.stackgres.testutil.fixture.Fixture;

public class UserDtoFixture extends Fixture<UserDto> {

  public UserDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_USER_DTO_JSON);
    return this;
  }

}
