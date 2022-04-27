/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.fixture;

import static java.lang.String.format;

import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.testutil.JsonUtil;

public class StackGresBackupFixture {

  public StackGresBackup build(String jsonFilename) {
    return JsonUtil.readFromJson(format("backup/%s.json", jsonFilename),
        StackGresBackup.class);
  }

}
