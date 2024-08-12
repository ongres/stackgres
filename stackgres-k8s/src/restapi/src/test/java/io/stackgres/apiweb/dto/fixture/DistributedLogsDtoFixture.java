/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.distributedlogs.DistributedLogsDto;
import io.stackgres.testutil.fixture.Fixture;

public class DistributedLogsDtoFixture extends Fixture<DistributedLogsDto> {

  public DistributedLogsDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_DISTRIBUTED_LOGS_DTO_JSON);
    return this;
  }

}
