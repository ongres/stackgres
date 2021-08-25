/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.fixture;

import static java.lang.String.format;

import java.util.UUID;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventSeries;
import io.fabric8.kubernetes.api.model.EventSource;
import io.fabric8.kubernetes.api.model.MicroTime;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectReference;

public class EventFixture {

  public static final String SOURCE_HOST = "127.0.0.1";
  public static final String REPORTING_COMPOENENT_CONTENT = "Reporting Compoenent Content";
  public static final int SERIES_COUNT = 100;
  public static final String EVENT_TIME = "1629920450";
  public static final String LAST_OBSERVED_TIME = "1629920435";
  private Event event;

  public EventFixture() {
    this.event = new Event();
  }

  public EventFixture withValidMetadata() {
    this.event.setMetadata(new ObjectMeta());
    return this;
  }

  public Event build() {
    return this.event;
  }

  public EventFixture withValidSeries() {
    this.event.setSeries(new EventSeries());
    this.event.getSeries().setLastObservedTime(new MicroTime(LAST_OBSERVED_TIME));
    this.event.getSeries().setCount(SERIES_COUNT);
    return this;
  }

  public EventFixture withEventTime() {
    this.event.setEventTime(new MicroTime(EVENT_TIME));
    return this;
  }

  public EventFixture withValidSource() {
    this.event.setSource(new EventSource());
    this.event.getSource().setComponent(REPORTING_COMPOENENT_CONTENT);
    this.event.getSource().setHost(SOURCE_HOST);
    return this;
  }

  public EventFixture withValidInvolvedObject() {
    event.setInvolvedObject(buildNewObjectReference("involved"));
    return this;
  }

  public EventFixture withValidRelatedObject() {
    this.event.setRelated(buildNewObjectReference("related"));
    return this;
  }

  private ObjectReference buildNewObjectReference(String prefix) {
    ObjectReference involvedObject = new ObjectReference();
    involvedObject.setKind(format("%s-kind-content", prefix));
    involvedObject.setNamespace(format("%s-namespace", prefix));
    involvedObject.setName(format("%s-name", prefix));
    involvedObject.setUid(format("%s-%s", prefix, UUID.randomUUID().toString()));
    return involvedObject;
  }

}
