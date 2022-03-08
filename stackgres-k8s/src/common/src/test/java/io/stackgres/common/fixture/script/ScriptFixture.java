/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.script;

import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.testutil.fixture.Fixture;

public class ScriptFixture extends Fixture<StackGresScript> {

  public ScriptFixture loadDefault() {
    fixture = readFromJson(STACKGRES_SCRIPT_DEFAULT_JSON);
    return this;
  }

}
