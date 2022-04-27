/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.fixture;

import static java.lang.String.format;

import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.testutil.JsonUtil;

public class StackGresBackupConfigFixture {

  public StackGresBackupConfig build(String jsonFilename) {
    return JsonUtil.readFromJson(format("backup_config/%s.json", jsonFilename),
        StackGresBackupConfig.class);
  }

}
