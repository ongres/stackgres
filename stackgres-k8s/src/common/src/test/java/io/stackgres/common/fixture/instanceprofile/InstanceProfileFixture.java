/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.instanceprofile;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileBuilder;
import io.stackgres.common.fixture.VersionedFixture;

public class InstanceProfileFixture extends VersionedFixture<StackGresProfile> {

  public InstanceProfileFixture loadSizeXs() {
    fixture = readFromJson(STACKGRES_INSTANCE_PROFILE_SIZE_XS_JSON);
    return this;
  }

  public InstanceProfileFixture loadSizeS() {
    fixture = readFromJson(STACKGRES_INSTANCE_PROFILE_SIZE_S_JSON);
    return this;
  }

  public StackGresProfileBuilder getBuilder() {
    return new StackGresProfileBuilder(fixture);
  }

}
