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
public class StreamSnapshotStatus {

  private String lastEvent;

  private Integer milliSecondsSinceLastEvent;

  private Integer totalNumberOfEventsSeen;

  private Integer numberOfEventsFiltered;

  private List<String> capturedTables;

  private Integer queueTotalCapacity;

  private Integer queueRemainingCapacity;

  private Integer totalTableCount;

  private Integer remainingTableCount;

  private Boolean snapshotRunning;

  private Boolean snapshotPaused;

  private Boolean snapshotAborted;

  private Boolean snapshotCompleted;

  private Integer snapshotDurationInSeconds;

  private Integer snapshotPausedDurationInSeconds;

  private Map<String, Integer> rowsScanned;

  private Integer maxQueueSizeInBytes;

  private Integer currentQueueSizeInBytes;

  private String chunkId;

  private String chunkFrom;

  private String chunkTo;

  private String tableFrom;

  private String tableTo;

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

  public Integer getTotalTableCount() {
    return totalTableCount;
  }

  public void setTotalTableCount(Integer totalTableCount) {
    this.totalTableCount = totalTableCount;
  }

  public Integer getRemainingTableCount() {
    return remainingTableCount;
  }

  public void setRemainingTableCount(Integer remainingTableCount) {
    this.remainingTableCount = remainingTableCount;
  }

  public Boolean getSnapshotRunning() {
    return snapshotRunning;
  }

  public void setSnapshotRunning(Boolean snapshotRunning) {
    this.snapshotRunning = snapshotRunning;
  }

  public Boolean getSnapshotPaused() {
    return snapshotPaused;
  }

  public void setSnapshotPaused(Boolean snapshotPaused) {
    this.snapshotPaused = snapshotPaused;
  }

  public Boolean getSnapshotAborted() {
    return snapshotAborted;
  }

  public void setSnapshotAborted(Boolean snapshotAborted) {
    this.snapshotAborted = snapshotAborted;
  }

  public Boolean getSnapshotCompleted() {
    return snapshotCompleted;
  }

  public void setSnapshotCompleted(Boolean snapshotCompleted) {
    this.snapshotCompleted = snapshotCompleted;
  }

  public Integer getSnapshotDurationInSeconds() {
    return snapshotDurationInSeconds;
  }

  public void setSnapshotDurationInSeconds(Integer snapshotDurationInSeconds) {
    this.snapshotDurationInSeconds = snapshotDurationInSeconds;
  }

  public Integer getSnapshotPausedDurationInSeconds() {
    return snapshotPausedDurationInSeconds;
  }

  public void setSnapshotPausedDurationInSeconds(Integer snapshotPausedDurationInSeconds) {
    this.snapshotPausedDurationInSeconds = snapshotPausedDurationInSeconds;
  }

  public Map<String, Integer> getRowsScanned() {
    return rowsScanned;
  }

  public void setRowsScanned(Map<String, Integer> rowsScanned) {
    this.rowsScanned = rowsScanned;
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

  public String getChunkId() {
    return chunkId;
  }

  public void setChunkId(String chunkId) {
    this.chunkId = chunkId;
  }

  public String getChunkFrom() {
    return chunkFrom;
  }

  public void setChunkFrom(String chunkFrom) {
    this.chunkFrom = chunkFrom;
  }

  public String getChunkTo() {
    return chunkTo;
  }

  public void setChunkTo(String chunkTo) {
    this.chunkTo = chunkTo;
  }

  public String getTableFrom() {
    return tableFrom;
  }

  public void setTableFrom(String tableFrom) {
    this.tableFrom = tableFrom;
  }

  public String getTableTo() {
    return tableTo;
  }

  public void setTableTo(String tableTo) {
    this.tableTo = tableTo;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
