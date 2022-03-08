/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.instanceprofile;

import io.stackgres.common.crd.sgprofile.StackGresProfileList;
import io.stackgres.testutil.fixture.Fixture;

public class InstanceProfileListFixture extends Fixture<StackGresProfileList> {

  public InstanceProfileListFixture loadDefault() {
    fixture = readFromJson(STACKGRES_INSTANCE_PROFILE_LIST_JSON);
    return this;
  }

}
