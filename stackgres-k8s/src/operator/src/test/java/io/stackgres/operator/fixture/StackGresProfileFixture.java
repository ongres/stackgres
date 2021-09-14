/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.fixture;

import static java.lang.String.format;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.testutil.JsonUtil;

public class StackGresProfileFixture {

  public StackGresProfile build(String jsonFilaname) {
    return JsonUtil.readFromJson(format("stackgres_profiles/%s.json", jsonFilaname),
        StackGresProfile.class);
  }

}
