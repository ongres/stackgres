/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import static java.lang.String.format;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.testutil.JsonUtil;

public class StackGresDistributedLogsFixture {

  public StackGresDistributedLogs build(String jsonFilename) {
    return JsonUtil.readFromJson(format("distributedlogs/%s.json", jsonFilename),
        StackGresDistributedLogs.class);
  }

}
