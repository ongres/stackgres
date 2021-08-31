/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import io.fabric8.kubernetes.api.model.Event;
import io.stackgres.testutil.JsonUtil;

public class EventFixture {

  private Event event;

  public EventFixture() {
    this.event = new Event();
  }

  public Event build() {
    this.event = JsonUtil.readFromJson("events/event_valid.json", Event.class);
    return this.event;
  }

}
