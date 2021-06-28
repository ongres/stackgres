/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer;

import io.fabric8.kubernetes.api.model.Event;
import io.stackgres.apiweb.dto.event.EventDto;
import io.stackgres.apiweb.dto.event.ObjectReference;
import io.stackgres.apiweb.dto.event.Series;

public class EventMapper {

  public static EventDto map(Event event) {
    EventDto eventDto = new EventDto();
    eventDto.setMetadata(MetadataMapper.map(event.getMetadata()));
    eventDto.setType(event.getType());
    eventDto.setAction(event.getAction());
    eventDto.setCount(event.getCount());
    if (event.getEventTime() != null) {
      eventDto.setEventTime(event.getEventTime().getTime());
    }
    eventDto.setFirstTimestamp(event.getFirstTimestamp());
    eventDto.setLastTimestamp(event.getLastTimestamp());
    eventDto.setReason(event.getReason());
    eventDto.setMessage(event.getMessage());
    eventDto.setReportingComponent(event.getReportingComponent());
    eventDto.setReportingInstance(event.getReportingInstance());
    eventDto.setAction(event.getAction());
    if (event.getInvolvedObject() != null) {
      eventDto.setInvolvedObject(new ObjectReference());
      eventDto.getInvolvedObject().setKind(event.getInvolvedObject().getKind());
      eventDto.getInvolvedObject().setNamespace(event.getInvolvedObject().getNamespace());
      eventDto.getInvolvedObject().setName(event.getInvolvedObject().getName());
      eventDto.getInvolvedObject().setUid(event.getInvolvedObject().getUid());
    }
    if (event.getRelated() != null) {
      eventDto.setRelated(new ObjectReference());
      eventDto.getRelated().setKind(event.getRelated().getKind());
      eventDto.getRelated().setNamespace(event.getRelated().getNamespace());
      eventDto.getRelated().setName(event.getRelated().getName());
      eventDto.getRelated().setUid(event.getRelated().getUid());
    }
    if (event.getSeries() != null) {
      eventDto.setSeries(new Series());
      eventDto.getSeries().setCount(event.getSeries().getCount());
      if (eventDto.getSeries().getLastObservedTime() != null) {
        eventDto.getSeries().setLastObservedTime(
            event.getSeries().getLastObservedTime().getTime());
      }
    }
    return eventDto;
  }

}
