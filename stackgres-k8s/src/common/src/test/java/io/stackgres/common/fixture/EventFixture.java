/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture;

import io.fabric8.kubernetes.api.model.Event;
import io.stackgres.testutil.fixture.Fixture;

public class EventFixture extends Fixture<Event> {

  public EventFixture loadDefault() {
    this.fixture = readFromJson(EVENT_EVENT_VALID_JSON);
    return this;
  }

}
