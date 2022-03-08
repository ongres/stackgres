/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;

import io.fabric8.kubernetes.api.model.Event;
import io.stackgres.apiweb.dto.event.EventDto;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventMapperTest {

  private static final String EVENT_INSTANCE = randomAlphabetic(10);

  private static final String REPORTING_COMPONENT = randomAlphabetic(30);

  private static final int EVENT_COUNT = 10;

  private static final String LAST_TIMESTAMP = LocalDate.now().toString();

  private static final String FIRST_TIMESTAMP = LocalDate.now().toString();

  private Event event;

  @BeforeEach
  void setup() {
    this.event = Fixtures.event().loadDefault().get();
  }

  @Test
  void shouldReturnAEmptyDto_onceEventObjectIsNotValid() {
    EventDto eventDto = EventMapper.map(null);
    assertNotNull(eventDto);
    assertNull(eventDto.getMetadata().getNamespace());
  }

  @Test
  void shouldDoNotReplaceLastTimestampValue_onceEventAlreadyHasTheValue() {
    event.setLastTimestamp(LAST_TIMESTAMP);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(LAST_TIMESTAMP, eventDto.getLastTimestamp());
  }

  @Test
  void shouldReplaceLastTimestampWithFirstTimestamp_onceHasNullOrEmptyValue() {
    event.setLastTimestamp(null);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(event.getFirstTimestamp(), eventDto.getLastTimestamp());
  }

  @Test
  void shouldReplaceLastTimestampWithLastTimestampFromSeries_onceHasNullOrEmptyValue() {
    event.setFirstTimestamp(null);
    event.setLastTimestamp(null);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(event.getSeries().getLastObservedTime().getTime(), eventDto.getLastTimestamp());
  }

  @Test
  void shouldSetLastTimestampNull_onceLastTimestampAndLasObservableTimeAreNull() {
    event.setLastTimestamp(null);
    event.getSeries().setLastObservedTime(null);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(event.getMetadata().getCreationTimestamp(), eventDto.getLastTimestamp());
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
    assertEquals(event.getEventTime().getTime(), eventDto.getFirstTimestamp());
  }

  @Test
  void shouldSetFirstTimestampAsNull_onceFirstTimestampAndEventTimeAreNull() {
    event.setFirstTimestamp(null);
    event.getEventTime().setTime(null);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(event.getMetadata().getCreationTimestamp(), eventDto.getFirstTimestamp());
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
    assertEquals(event.getSeries().getCount().intValue(), eventDto.getCount().intValue());
  }

  @Test
  void shouldSetCountAsNull_onceCountAndEventAndEventSeriesAreNull() {
    event.setCount(null);
    event.getSeries().setCount(null);
    EventDto eventDto = EventMapper.map(event);
    assertEquals(Integer.valueOf(1), eventDto.getCount());
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
    assertEquals(event.getSource().getComponent(), eventDto.getReportingComponent());
  }

  @Test
  void shouldSetReportingComponentAsNull_onceReportingComponentAndSourceComponentAreNull() {
    event.setReportingComponent(null);
    event.getSource().setComponent(null);
    EventDto eventDto = EventMapper.map(event);
    assertNull(eventDto.getReportingComponent());
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
    assertEquals(event.getSource().getHost(), eventDto.getReportingInstance());
  }

  @Test
  void shouldsetReportingInstanceAsNull_onceReportingInstanceAndSourceHostAreNull() {
    event.setReportingInstance(null);
    event.getSource().setHost(null);
    EventDto eventDto = EventMapper.map(event);
    assertNull(eventDto.getReportingInstance());
  }

}
