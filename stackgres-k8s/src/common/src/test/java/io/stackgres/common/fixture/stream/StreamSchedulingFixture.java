/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.stream;

import io.stackgres.common.crd.sgstream.StackGresStreamPodsScheduling;
import io.stackgres.testutil.fixture.Fixture;

public class StreamSchedulingFixture extends Fixture<StackGresStreamPodsScheduling> {

  public StreamSchedulingFixture loadDefault() {
    fixture = readFromJson(STACKGRES_STREAM_SCHEDULING_JSON);
    return this;
  }

}
