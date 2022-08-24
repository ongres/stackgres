/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.distributedlogs;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsBuilder;
import io.stackgres.common.fixture.VersionedFixture;

public class DistributedLogsFixture extends VersionedFixture<StackGresDistributedLogs> {

  public DistributedLogsSpecFixture spec() {
    return new DistributedLogsSpecFixture();
  }

  public DistributedLogsFixture loadDefault() {
    fixture = readFromJson(STACKGRES_DISTRIBUTED_LOGS_DEFAULT_JSON);
    return this;
  }

  public StackGresDistributedLogsBuilder getBuilder() {
    return new StackGresDistributedLogsBuilder(fixture);
  }

}
