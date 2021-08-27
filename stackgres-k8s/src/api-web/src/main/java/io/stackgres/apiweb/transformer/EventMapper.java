/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.fabric8.kubernetes.api.model.Event;
import io.stackgres.apiweb.dto.event.EventDto;

public class EventMapper {

  public static EventDto map(Event event) {
    EventDto eventDto = new EventDto();

    if (event == null) {
      return eventDto;
    }

    eventDto.setMetadata(MetadataMapper.map(event.getMetadata()));
    eventDto.setType(event.getType());
    eventDto.setAction(event.getAction());
    eventDto.setReason(event.getReason());
    eventDto.setMessage(event.getMessage());
    eventDto.setAction(event.getAction());
    eventDto.setCount(setupEventCount(event));
    eventDto.setFirstTimestamp(setupFirstTimestamp(event));
    eventDto.setLastTimestamp(setupLastTimestamp(event));
    eventDto.setReportingComponent(setupReportingComponent(event));
    eventDto.setReportingInstance(setupReportingInstance(event));
    eventDto.setInvolvedObject(ObjectReferenceMapper.map(event.getInvolvedObject()));
    eventDto.setRelated(ObjectReferenceMapper.map(event.getRelated()));
    return eventDto;
  }

  private static String setupReportingInstance(Event event) {
    String reportingInstance = event.getReportingInstance();
    if (isBlank(reportingInstance) && event.getSource() != null) {
      reportingInstance = event.getSource().getHost();
    }
    return reportingInstance;
  }

  private static String setupReportingComponent(Event event) {
    String reportingComponent = event.getReportingComponent();
    if (isBlank(reportingComponent) && event.getSource() != null) {
      reportingComponent = event.getSource().getComponent();
    }
    return reportingComponent;
  }

  private static Integer setupEventCount(Event event) {
    Integer eventCount = event.getCount();
    if (eventCount == null && event.getSeries() != null) {
      eventCount = event.getSeries().getCount();
    }
    return eventCount;
  }

  private static String setupFirstTimestamp(Event event) {
    String firstTimeStamp = event.getFirstTimestamp();
    if (isBlank(firstTimeStamp) && event.getEventTime() != null) {
      firstTimeStamp = event.getEventTime().getTime();
    }
    return firstTimeStamp;
  }

  private static String setupLastTimestamp(Event event) {
    String lastTimestamp = event.getLastTimestamp();
    if (isBlank(lastTimestamp)) {
      lastTimestamp = event.getSeries().getLastObservedTime().getTime();
    }
    return lastTimestamp;
  }

}
