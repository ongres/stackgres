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
import io.stackgres.apiweb.transformer.util.TransformerUtil;
import org.apache.commons.lang3.StringUtils;

public class EventMapper {

  public static EventDto map(Event event) {
    EventDto eventDto = new EventDto();

    if (event == null) {
      return eventDto;
    }

    eventDto.setMetadata(TransformerUtil.fromResource(event.getMetadata()));
    eventDto.setType(event.getType());
    eventDto.setAction(event.getAction());
    eventDto.setReason(event.getReason());
    eventDto.setMessage(event.getMessage());
    eventDto.setFirstTimestamp(getFirstTimestamp(event));
    eventDto.setLastTimestamp(getLastTimestamp(event));
    eventDto.setCount(getEventCount(event));
    eventDto.setReportingComponent(getReportingComponent(event));
    eventDto.setReportingInstance(getReportingInstance(event));
    eventDto.setInvolvedObject(ObjectReferenceMapper.map(event.getInvolvedObject()));
    eventDto.setRelated(ObjectReferenceMapper.map(event.getRelated()));
    return eventDto;
  }

  private static String getFirstTimestamp(Event event) {
    return Optional.ofNullable(event.getFirstTimestamp())
        .filter(StringUtils::isNotBlank)
        .or(() -> Optional.ofNullable(event.getEventTime())
            .map(MicroTime::getTime))
        .orElseGet(() -> event.getMetadata().getCreationTimestamp());
  }

  private static String getLastTimestamp(Event event) {
    return Optional.ofNullable(event.getLastTimestamp())
        .filter(StringUtils::isNotBlank)
        .orElseGet(() -> getFirstTimestamp(event));
  }

  private static Integer getEventCount(Event event) {
    return Optional.ofNullable(event.getCount())
        .or(() -> Optional.ofNullable(event.getSeries())
            .map(EventSeries::getCount)).orElse(1);
  }

  private static String getReportingComponent(Event event) {
    return Optional.ofNullable(event.getReportingComponent())
        .filter(StringUtils::isNotBlank).or(() -> Optional.ofNullable(event.getSource())
            .map(EventSource::getComponent)).orElse(null);
  }

  private static String getReportingInstance(Event event) {
    return Optional.ofNullable(event.getReportingInstance())
        .filter(StringUtils::isNotBlank).or(() -> Optional.ofNullable(event.getSource())
            .map(EventSource::getHost)).orElse(null);
  }

}
