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
public class StackGresStreamSnapshotStatus {

  private String lastEvent;

  private Long milliSecondsSinceLastEvent;

  private Long totalNumberOfEventsSeen;

  private Long numberOfEventsFiltered;

  private List<String> capturedTables;

  private Integer queueTotalCapacity;

  private Integer queueRemainingCapacity;

  private Integer totalTableCount;

  private Integer remainingTableCount;

  private Boolean snapshotRunning;

  private Boolean snapshotPaused;

  private Boolean snapshotAborted;

  private Boolean snapshotCompleted;

  private Long snapshotDurationInSeconds;

  private Long snapshotPausedDurationInSeconds;

  private Map<String, Long> rowsScanned;

  private Long maxQueueSizeInBytes;

  private Long currentQueueSizeInBytes;

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

  public Long getSnapshotDurationInSeconds() {
    return snapshotDurationInSeconds;
  }

  public void setSnapshotDurationInSeconds(Long snapshotDurationInSeconds) {
    this.snapshotDurationInSeconds = snapshotDurationInSeconds;
  }

  public Long getSnapshotPausedDurationInSeconds() {
    return snapshotPausedDurationInSeconds;
  }

  public void setSnapshotPausedDurationInSeconds(Long snapshotPausedDurationInSeconds) {
    this.snapshotPausedDurationInSeconds = snapshotPausedDurationInSeconds;
  }

  public Map<String, Long> getRowsScanned() {
    return rowsScanned;
  }

  public void setRowsScanned(Map<String, Long> rowsScanned) {
    this.rowsScanned = rowsScanned;
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
  public int hashCode() {
    return Objects.hash(capturedTables, chunkFrom, chunkId, chunkTo, currentQueueSizeInBytes,
        lastEvent, maxQueueSizeInBytes, milliSecondsSinceLastEvent, numberOfEventsFiltered,
        queueRemainingCapacity, queueTotalCapacity, remainingTableCount, rowsScanned,
        snapshotAborted, snapshotCompleted, snapshotDurationInSeconds, snapshotPaused,
        snapshotPausedDurationInSeconds, snapshotRunning, tableFrom, tableTo,
        totalNumberOfEventsSeen, totalTableCount);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresStreamSnapshotStatus)) {
      return false;
    }
    StackGresStreamSnapshotStatus other = (StackGresStreamSnapshotStatus) obj;
    return Objects.equals(capturedTables, other.capturedTables)
        && Objects.equals(chunkFrom, other.chunkFrom) && Objects.equals(chunkId, other.chunkId)
        && Objects.equals(chunkTo, other.chunkTo)
        && Objects.equals(currentQueueSizeInBytes, other.currentQueueSizeInBytes)
        && Objects.equals(lastEvent, other.lastEvent)
        && Objects.equals(maxQueueSizeInBytes, other.maxQueueSizeInBytes)
        && Objects.equals(milliSecondsSinceLastEvent, other.milliSecondsSinceLastEvent)
        && Objects.equals(numberOfEventsFiltered, other.numberOfEventsFiltered)
        && Objects.equals(queueRemainingCapacity, other.queueRemainingCapacity)
        && Objects.equals(queueTotalCapacity, other.queueTotalCapacity)
        && Objects.equals(remainingTableCount, other.remainingTableCount)
        && Objects.equals(rowsScanned, other.rowsScanned)
        && Objects.equals(snapshotAborted, other.snapshotAborted)
        && Objects.equals(snapshotCompleted, other.snapshotCompleted)
        && Objects.equals(snapshotDurationInSeconds, other.snapshotDurationInSeconds)
        && Objects.equals(snapshotPaused, other.snapshotPaused)
        && Objects.equals(snapshotPausedDurationInSeconds, other.snapshotPausedDurationInSeconds)
        && Objects.equals(snapshotRunning, other.snapshotRunning)
        && Objects.equals(tableFrom, other.tableFrom) && Objects.equals(tableTo, other.tableTo)
        && Objects.equals(totalNumberOfEventsSeen, other.totalNumberOfEventsSeen)
        && Objects.equals(totalTableCount, other.totalTableCount);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
