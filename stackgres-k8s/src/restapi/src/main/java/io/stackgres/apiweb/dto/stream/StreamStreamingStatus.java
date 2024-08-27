/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.stream;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class StreamStreamingStatus {

  private String lastEvent;

  private Integer milliSecondsSinceLastEvent;

  private Integer totalNumberOfEventsSeen;

  private Integer totalNumberOfCreateEventsSeen;

  private Integer totalNumberOfUpdateEventsSeen;

  private Integer totalNumberOfDeleteEventsSeen;

  private Integer numberOfEventsFiltered;

  private List<String> capturedTables;

  private Integer queueTotalCapacity;

  private Integer queueRemainingCapacity;

  private Boolean connected;

  private Integer milliSecondsBehindSource;

  private Integer numberOfCommittedTransactions;

  private Map<String, String> sourceEventPosition;

  private String lastTransactionId;

  private Integer maxQueueSizeInBytes;

  private Integer currentQueueSizeInBytes;

  public String getLastEvent() {
    return lastEvent;
  }

  public void setLastEvent(String lastEvent) {
    this.lastEvent = lastEvent;
  }

  public Integer getMilliSecondsSinceLastEvent() {
    return milliSecondsSinceLastEvent;
  }

  public void setMilliSecondsSinceLastEvent(Integer milliSecondsSinceLastEvent) {
    this.milliSecondsSinceLastEvent = milliSecondsSinceLastEvent;
  }

  public Integer getTotalNumberOfEventsSeen() {
    return totalNumberOfEventsSeen;
  }

  public void setTotalNumberOfEventsSeen(Integer totalNumberOfEventsSeen) {
    this.totalNumberOfEventsSeen = totalNumberOfEventsSeen;
  }

  public Integer getTotalNumberOfCreateEventsSeen() {
    return totalNumberOfCreateEventsSeen;
  }

  public void setTotalNumberOfCreateEventsSeen(Integer totalNumberOfCreateEventsSeen) {
    this.totalNumberOfCreateEventsSeen = totalNumberOfCreateEventsSeen;
  }

  public Integer getTotalNumberOfUpdateEventsSeen() {
    return totalNumberOfUpdateEventsSeen;
  }

  public void setTotalNumberOfUpdateEventsSeen(Integer totalNumberOfUpdateEventsSeen) {
    this.totalNumberOfUpdateEventsSeen = totalNumberOfUpdateEventsSeen;
  }

  public Integer getTotalNumberOfDeleteEventsSeen() {
    return totalNumberOfDeleteEventsSeen;
  }

  public void setTotalNumberOfDeleteEventsSeen(Integer totalNumberOfDeleteEventsSeen) {
    this.totalNumberOfDeleteEventsSeen = totalNumberOfDeleteEventsSeen;
  }

  public Integer getNumberOfEventsFiltered() {
    return numberOfEventsFiltered;
  }

  public void setNumberOfEventsFiltered(Integer numberOfEventsFiltered) {
    this.numberOfEventsFiltered = numberOfEventsFiltered;
  }

  public List<String> getCapturedTables() {
    return capturedTables;
  }

  public void setCapturedTables(List<String> capturedTables) {
    this.capturedTables = capturedTables;
  }

  public Integer getQueueTotalCapacity() {
    return queueTotalCapacity;
  }

  public void setQueueTotalCapacity(Integer queueTotalCapacity) {
    this.queueTotalCapacity = queueTotalCapacity;
  }

  public Integer getQueueRemainingCapacity() {
    return queueRemainingCapacity;
  }

  public void setQueueRemainingCapacity(Integer queueRemainingCapacity) {
    this.queueRemainingCapacity = queueRemainingCapacity;
  }

  public Boolean getConnected() {
    return connected;
  }

  public void setConnected(Boolean connected) {
    this.connected = connected;
  }

  public Integer getMilliSecondsBehindSource() {
    return milliSecondsBehindSource;
  }

  public void setMilliSecondsBehindSource(Integer milliSecondsBehindSource) {
    this.milliSecondsBehindSource = milliSecondsBehindSource;
  }

  public Integer getNumberOfCommittedTransactions() {
    return numberOfCommittedTransactions;
  }

  public void setNumberOfCommittedTransactions(Integer numberOfCommittedTransactions) {
    this.numberOfCommittedTransactions = numberOfCommittedTransactions;
  }

  public Map<String, String> getSourceEventPosition() {
    return sourceEventPosition;
  }

  public void setSourceEventPosition(Map<String, String> sourceEventPosition) {
    this.sourceEventPosition = sourceEventPosition;
  }

  public String getLastTransactionId() {
    return lastTransactionId;
  }

  public void setLastTransactionId(String lastTransactionId) {
    this.lastTransactionId = lastTransactionId;
  }

  public Integer getMaxQueueSizeInBytes() {
    return maxQueueSizeInBytes;
  }

  public void setMaxQueueSizeInBytes(Integer maxQueueSizeInBytes) {
    this.maxQueueSizeInBytes = maxQueueSizeInBytes;
  }

  public Integer getCurrentQueueSizeInBytes() {
    return currentQueueSizeInBytes;
  }

  public void setCurrentQueueSizeInBytes(Integer currentQueueSizeInBytes) {
    this.currentQueueSizeInBytes = currentQueueSizeInBytes;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
