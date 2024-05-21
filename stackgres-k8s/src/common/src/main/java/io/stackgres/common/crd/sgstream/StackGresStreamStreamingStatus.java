/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgstream;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresStreamStreamingStatus {

  private String lastEvent;

  private Long milliSecondsSinceLastEvent;

  private Long totalNumberOfEventsSeen;

  private Long totalNumberOfCreateEventsSeen;

  private Long totalNumberOfUpdateEventsSeen;

  private Long totalNumberOfDeleteEventsSeen;

  private Long numberOfEventsFiltered;

  private List<String> capturedTables;

  private Integer queueTotalCapacity;

  private Integer queueRemainingCapacity;

  private Boolean connected;

  private Long milliSecondsBehindSource;

  private Long numberOfCommittedTransactions;

  private Map<String, String> sourceEventPosition;

  private String lastTransactionId;

  private Long maxQueueSizeInBytes;

  private Long currentQueueSizeInBytes;

  public String getLastEvent() {
    return lastEvent;
  }

  public void setLastEvent(String lastEvent) {
    this.lastEvent = lastEvent;
  }

  public Long getMilliSecondsSinceLastEvent() {
    return milliSecondsSinceLastEvent;
  }

  public void setMilliSecondsSinceLastEvent(Long milliSecondsSinceLastEvent) {
    this.milliSecondsSinceLastEvent = milliSecondsSinceLastEvent;
  }

  public Long getTotalNumberOfEventsSeen() {
    return totalNumberOfEventsSeen;
  }

  public void setTotalNumberOfEventsSeen(Long totalNumberOfEventsSeen) {
    this.totalNumberOfEventsSeen = totalNumberOfEventsSeen;
  }

  public Long getTotalNumberOfCreateEventsSeen() {
    return totalNumberOfCreateEventsSeen;
  }

  public void setTotalNumberOfCreateEventsSeen(Long totalNumberOfCreateEventsSeen) {
    this.totalNumberOfCreateEventsSeen = totalNumberOfCreateEventsSeen;
  }

  public Long getTotalNumberOfUpdateEventsSeen() {
    return totalNumberOfUpdateEventsSeen;
  }

  public void setTotalNumberOfUpdateEventsSeen(Long totalNumberOfUpdateEventsSeen) {
    this.totalNumberOfUpdateEventsSeen = totalNumberOfUpdateEventsSeen;
  }

  public Long getTotalNumberOfDeleteEventsSeen() {
    return totalNumberOfDeleteEventsSeen;
  }

  public void setTotalNumberOfDeleteEventsSeen(Long totalNumberOfDeleteEventsSeen) {
    this.totalNumberOfDeleteEventsSeen = totalNumberOfDeleteEventsSeen;
  }

  public Long getNumberOfEventsFiltered() {
    return numberOfEventsFiltered;
  }

  public void setNumberOfEventsFiltered(Long numberOfEventsFiltered) {
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

  public Long getMilliSecondsBehindSource() {
    return milliSecondsBehindSource;
  }

  public void setMilliSecondsBehindSource(Long milliSecondsBehindSource) {
    this.milliSecondsBehindSource = milliSecondsBehindSource;
  }

  public Long getNumberOfCommittedTransactions() {
    return numberOfCommittedTransactions;
  }

  public void setNumberOfCommittedTransactions(Long numberOfCommittedTransactions) {
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

  public Long getMaxQueueSizeInBytes() {
    return maxQueueSizeInBytes;
  }

  public void setMaxQueueSizeInBytes(Long maxQueueSizeInBytes) {
    this.maxQueueSizeInBytes = maxQueueSizeInBytes;
  }

  public Long getCurrentQueueSizeInBytes() {
    return currentQueueSizeInBytes;
  }

  public void setCurrentQueueSizeInBytes(Long currentQueueSizeInBytes) {
    this.currentQueueSizeInBytes = currentQueueSizeInBytes;
  }

  @Override
  public int hashCode() {
    return Objects.hash(capturedTables, connected, currentQueueSizeInBytes, lastEvent,
        lastTransactionId, maxQueueSizeInBytes, milliSecondsBehindSource,
        milliSecondsSinceLastEvent, numberOfCommittedTransactions, numberOfEventsFiltered,
        queueRemainingCapacity, queueTotalCapacity, sourceEventPosition,
        totalNumberOfCreateEventsSeen, totalNumberOfDeleteEventsSeen, totalNumberOfEventsSeen,
        totalNumberOfUpdateEventsSeen);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamStreamingStatus)) {
      return false;
    }
    StackGresStreamStreamingStatus other = (StackGresStreamStreamingStatus) obj;
    return Objects.equals(capturedTables, other.capturedTables)
        && Objects.equals(connected, other.connected)
        && Objects.equals(currentQueueSizeInBytes, other.currentQueueSizeInBytes)
        && Objects.equals(lastEvent, other.lastEvent)
        && Objects.equals(lastTransactionId, other.lastTransactionId)
        && Objects.equals(maxQueueSizeInBytes, other.maxQueueSizeInBytes)
        && Objects.equals(milliSecondsBehindSource, other.milliSecondsBehindSource)
        && Objects.equals(milliSecondsSinceLastEvent, other.milliSecondsSinceLastEvent)
        && Objects.equals(numberOfCommittedTransactions, other.numberOfCommittedTransactions)
        && Objects.equals(numberOfEventsFiltered, other.numberOfEventsFiltered)
        && Objects.equals(queueRemainingCapacity, other.queueRemainingCapacity)
        && Objects.equals(queueTotalCapacity, other.queueTotalCapacity)
        && Objects.equals(sourceEventPosition, other.sourceEventPosition)
        && Objects.equals(totalNumberOfCreateEventsSeen, other.totalNumberOfCreateEventsSeen)
        && Objects.equals(totalNumberOfDeleteEventsSeen, other.totalNumberOfDeleteEventsSeen)
        && Objects.equals(totalNumberOfEventsSeen, other.totalNumberOfEventsSeen)
        && Objects.equals(totalNumberOfUpdateEventsSeen, other.totalNumberOfUpdateEventsSeen);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
