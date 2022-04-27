/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.fixture;

import static java.lang.String.format;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.testutil.JsonUtil;

public class StackGresDbOpsFixture {

  public StackGresDbOps build(String string) {
    return JsonUtil.readFromJson(format("dbops/%s.json", string), StackGresDbOps.class);
  }

}
