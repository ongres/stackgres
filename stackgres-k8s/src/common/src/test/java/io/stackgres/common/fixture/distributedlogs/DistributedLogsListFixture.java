/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.distributedlogs;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsList;
import io.stackgres.testutil.fixture.Fixture;

public class DistributedLogsListFixture extends Fixture<StackGresDistributedLogsList> {

  public DistributedLogsListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_DISTRIBUTED_LOGS_LIST_JSON);
    return this;
  }

}
