/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.EventSeries;
import io.fabric8.kubernetes.api.model.EventSource;
import io.fabric8.kubernetes.api.model.MicroTime;
import io.stackgres.apiweb.dto.event.EventDto;
import org.apache.commons.lang3.StringUtils;

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
    return Optional.ofNullable(event.getReportingInstance())
        .filter(StringUtils::isNotBlank).or(() -> Optional.ofNullable(event.getSource())
            .map(EventSource::getHost)).orElse(null);
  }

  private static String setupReportingComponent(Event event) {
    return Optional.ofNullable(event.getReportingComponent())
        .filter(StringUtils::isNotBlank).or(() -> Optional.ofNullable(event.getSource())
            .map(EventSource::getComponent)).orElse(null);
  }

  private static Integer setupEventCount(Event event) {
    return Optional.ofNullable(event.getCount())
        .or(() -> Optional.ofNullable(event.getSeries())
            .map(EventSeries::getCount)).orElse(null);
  }

  private static String setupFirstTimestamp(Event event) {
    return Optional.ofNullable(event.getFirstTimestamp())
        .filter(StringUtils::isNotBlank).or(() -> Optional.ofNullable(event.getEventTime())
            .map(MicroTime::getTime)).orElse(null);
  }

  private static String setupLastTimestamp(Event event) {
    return Optional.ofNullable(event.getLastTimestamp())
        .filter(StringUtils::isNotBlank).or(() -> Optional.ofNullable(event.getSeries())
            .map(EventSeries::getLastObservedTime)
            .map(MicroTime::getTime)).orElse(null);
  }

}
