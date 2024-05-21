/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.stackgres.apiweb.dto.stream.StreamDto;
import io.stackgres.testutil.fixture.Fixture;

public class StreamDtoFixture extends Fixture<StreamDto> {

  public StreamDtoFixture loadDefault() {
    fixture = readFromJson(STACKGRES_STREAM_DTO_JSON);
    return this;
  }

}
