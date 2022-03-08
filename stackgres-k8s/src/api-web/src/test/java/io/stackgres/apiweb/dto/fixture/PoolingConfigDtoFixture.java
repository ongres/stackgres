/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.pooling.PoolingConfigDto;
import io.stackgres.testutil.fixture.Fixture;

public class PoolingConfigDtoFixture extends Fixture<PoolingConfigDto> {

  public PoolingConfigDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_POOLING_CONFIG_DTO_JSON);
    return this;
  }

}
