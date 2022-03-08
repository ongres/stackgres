/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.testutil.fixture.Fixture;

public class ScriptDtoFixture extends Fixture<ScriptDto> {

  public ScriptDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_SCRIPT_DTO_JSON);
    return this;
  }

}
