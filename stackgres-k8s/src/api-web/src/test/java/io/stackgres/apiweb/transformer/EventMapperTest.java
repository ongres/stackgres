/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import static io.stackgres.apiweb.dto.fixture.EventFixture.EVENT_TIME;
import static io.stackgres.apiweb.dto.fixture.EventFixture.LAST_OBSERVED_TIME;
import static io.stackgres.apiweb.dto.fixture.EventFixture.REPORTING_COMPOENENT_CONTENT;
import static io.stackgres.apiweb.dto.fixture.EventFixture.SERIES_COUNT;
import static io.stackgres.apiweb.dto.fixture.EventFixture.SOURCE_HOST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import io.fabric8.kubernetes.api.model.Event;
import io.stackgres.apiweb.dto.event.EventDto;
import io.stackgres.apiweb.dto.fixture.EventFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventMapperTest {

  private static final String EVENT_INSTANCE = "10.0.2.1";

  private static final String REPORTING_COMPONENT = "Reporting componenet content";

  private static final int EVENT_COUNT = 10;

  private static final String LAST_TIMESTAMP = "2021-01-01 12:22:22";

  private static final String FIRST_TIMESTAMP = "2021-01-01 11:22:22";

  private Event event;

  @BeforeEach
  void setup() {
    this.event = new EventFixture().withValidMetadata().withValidSource().withValidSeries()
        .withEventTime().withValidInvolvedObject().withValidRelatedObject().build();
  }

  @Test
  void shouldDoNotReplaceLastTimestampValue_onceEventAlreadyHasTheValue() {
    event.setLastTimestamp(LAST_TIMESTAMP);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(LAST_TIMESTAMP, eventDto.getLastTimestamp());
  }

  @Test
  void shouldReplaceLastTimestampWithLastTimestampFromSeries_onceHasNullOrEmptyValue() {
    event.setLastTimestamp(null);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(LAST_OBSERVED_TIME, eventDto.getLastTimestamp());
  }

  @Test
  void shouldDoNotReplaceFirstTimestampValue_onceEventAlreadyHasTheValue() {
    event.setFirstTimestamp(FIRST_TIMESTAMP);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(FIRST_TIMESTAMP, eventDto.getFirstTimestamp());
  }

  @Test
  void shouldReplaceFirstTimestampValue_onceHasNullOrEmptyValue() {
    event.setFirstTimestamp(null);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(EVENT_TIME, eventDto.getFirstTimestamp());
  }

  @Test
  void shouldDoNotReplaceEventCountValue_onceEventAlreadyHasTheValue() {
    event.setCount(EVENT_COUNT);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(EVENT_COUNT, eventDto.getCount().intValue());
  }

  @Test
  void shouldReplaceEventCountValue_onceHasNullOrEmptyValue() {
    event.setCount(null);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(SERIES_COUNT, eventDto.getCount().intValue());
  }

  @Test
  void shouldDoNotReplaceEventReportingComponentValue_onceEventAlreadyHasTheValue() {
    event.setReportingComponent(REPORTING_COMPONENT);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(REPORTING_COMPONENT, eventDto.getReportingComponent());
  }

  @Test
  void shouldReplaceEventReportingComponentValue_onceHasNullOrEmptyValue() {
    event.setReportingComponent(null);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(REPORTING_COMPOENENT_CONTENT, eventDto.getReportingComponent());
  }

  @Test
  void shouldDoNotReplaceEventReportingInstanceValue_onceEventAlreadyHasTheValue() {
    event.setReportingInstance(EVENT_INSTANCE);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(EVENT_INSTANCE, eventDto.getReportingInstance());
  }

  @Test
  void shouldReplaceEventReportingHostValue_onceHasNullOrEmptyValue() {
    event.setReportingInstance(null);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(SOURCE_HOST, eventDto.getReportingInstance());
  }

  @Test
  void shouldPopulatedObjectReference_onceInvolvedObjectIsAValidValue() {
    EventDto eventDto = EventMapper.map(event);
    assertEquals(event.getInvolvedObject().getName(), eventDto.getInvolvedObject().getName());
    assertEquals(event.getInvolvedObject().getKind(), eventDto.getInvolvedObject().getKind());
    assertEquals(event.getInvolvedObject().getNamespace(),
        eventDto.getInvolvedObject().getNamespace());
    assertEquals(event.getInvolvedObject().getUid(), eventDto.getInvolvedObject().getUid());
  }

  @Test
  void shouldIdentifyNullableInvolvedObject_onceEventInvolvedObjectHasNoValue() {
    event.setInvolvedObject(null);
    EventDto eventDto = EventMapper.map(event);
    assertNull(eventDto.getInvolvedObject());
  }

  @Test
  void shouldPopulateRelatedObject_onceEventRelatedisAValidValue() {
    EventDto eventDto = EventMapper.map(event);
    assertEquals(event.getRelated().getName(), eventDto.getRelated().getName());
    assertEquals(event.getRelated().getKind(), eventDto.getRelated().getKind());
    assertEquals(event.getRelated().getNamespace(), eventDto.getRelated().getNamespace());
    assertEquals(event.getRelated().getUid(), eventDto.getRelated().getUid());
  }

  @Test
  void shouldIdentifyNullableRelatedObject_onceEventInvolvedObjectHasNoValue() {
    event.setRelated(null);
    EventDto eventDto = EventMapper.map(event);
    assertNull(eventDto.getRelated());
  }

}
