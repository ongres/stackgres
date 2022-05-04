/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.fixture;

import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatus;
import io.stackgres.testutil.JsonUtil;

public class StackGresDbOpsPgbenchStatusFixture {

  public StackGresDbOpsPgbenchStatus build() {
    return JsonUtil.readFromJson("dbops/pgbench_status.json", StackGresDbOpsPgbenchStatus.class);
  }

}
