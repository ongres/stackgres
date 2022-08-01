/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.upgrade;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileBuilder;
import io.stackgres.testutil.fixture.Fixture;

public class InstanceProfileFixture extends Fixture<StackGresProfile> {

  public InstanceProfileFixture loadDefault() {
    fixture = readFromJson(UPGRADE_SGINSTANCEPROFILE_JSON);
    return this;
  }

  public StackGresProfileBuilder getBuilder() {
    return new StackGresProfileBuilder(fixture);
  }

}
