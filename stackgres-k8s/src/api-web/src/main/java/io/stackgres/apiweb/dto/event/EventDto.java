/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.event;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
public class EventDto extends ResourceDto {

  private String type;
  private String action;
  private Integer count;
  private String eventTime;
  private String firstTimestamp;
  private String lastTimestamp;
  private String reason;
  private String message;
  private ObjectReference involvedObject;
  private ObjectReference related;
  private String reportingComponent;
  private String reportingInstance;
  private Series series;
  private Source source;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }

  public String getEventTime() {
    return eventTime;
  }

  public void setEventTime(String eventTime) {
    this.eventTime = eventTime;
  }

  public String getFirstTimestamp() {
    return firstTimestamp;
  }

  public void setFirstTimestamp(String firstTimestamp) {
    this.firstTimestamp = firstTimestamp;
  }

  public String getLastTimestamp() {
    return lastTimestamp;
  }

  public void setLastTimestamp(String lastTimestamp) {
    this.lastTimestamp = lastTimestamp;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public ObjectReference getInvolvedObject() {
    return involvedObject;
  }

  public void setInvolvedObject(ObjectReference involvedObject) {
    this.involvedObject = involvedObject;
  }

  public ObjectReference getRelated() {
    return related;
  }

  public void setRelated(ObjectReference related) {
    this.related = related;
  }

  public String getReportingComponent() {
    return reportingComponent;
  }

  public void setReportingComponent(String reportingComponent) {
    this.reportingComponent = reportingComponent;
  }

  public String getReportingInstance() {
    return reportingInstance;
  }

  public void setReportingInstance(String reportingInstance) {
    this.reportingInstance = reportingInstance;
  }

  public Series getSeries() {
    return series;
  }

  public void setSeries(Series series) {
    this.series = series;
  }

  public Source getSource() {
    return source;
  }

  public void setSource(Source source) {
    this.source = source;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
