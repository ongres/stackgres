/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterPodStatus;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterStatus {

  @JsonProperty("cpu_requested")
  private String cpuRequested;

  @JsonProperty("cpu_found")
  private String cpuFound;

  @JsonProperty("memory_requested")
  private String memoryRequested;

  @JsonProperty("memory_found")
  private String memoryFound;

  @JsonProperty("memory_used")
  private String memoryUsed;

  @JsonProperty("disk_found")
  private String diskFound;

  @JsonProperty("disk_used")
  private String diskUsed;

  @JsonProperty("average_load_1m")
  private String averageLoad1m;

  @JsonProperty("average_load_5m")
  private String averageLoad5m;

  @JsonProperty("average_load_10m")
  private String averageLoad10m;

  @JsonProperty("pods")
  private List<StackGresClusterPodStatus> pods;

  @JsonProperty("pods_ready")
  private String podsReady;

  public String getCpuRequested() {
    return cpuRequested;
  }

  public void setCpuRequested(String cpuRequested) {
    this.cpuRequested = cpuRequested;
  }

  public String getCpuFound() {
    return cpuFound;
  }

  public void setCpuFound(String cpuFound) {
    this.cpuFound = cpuFound;
  }

  public String getMemoryRequested() {
    return memoryRequested;
  }

  public void setMemoryRequested(String memoryRequested) {
    this.memoryRequested = memoryRequested;
  }

  public String getMemoryFound() {
    return memoryFound;
  }

  public void setMemoryFound(String memoryFound) {
    this.memoryFound = memoryFound;
  }

  public String getMemoryUsed() {
    return memoryUsed;
  }

  public void setMemoryUsed(String memoryUsed) {
    this.memoryUsed = memoryUsed;
  }

  public String getDiskFound() {
    return diskFound;
  }

  public void setDiskFound(String diskFound) {
    this.diskFound = diskFound;
  }

  public String getDiskUsed() {
    return diskUsed;
  }

  public void setDiskUsed(String diskUsed) {
    this.diskUsed = diskUsed;
  }

  public String getAverageLoad1m() {
    return averageLoad1m;
  }

  public void setAverageLoad1m(String averageLoad1m) {
    this.averageLoad1m = averageLoad1m;
  }

  public String getAverageLoad5m() {
    return averageLoad5m;
  }

  public void setAverageLoad5m(String averageLoad5m) {
    this.averageLoad5m = averageLoad5m;
  }

  public String getAverageLoad10m() {
    return averageLoad10m;
  }

  public void setAverageLoad10m(String averageLoad10m) {
    this.averageLoad10m = averageLoad10m;
  }

  public List<StackGresClusterPodStatus> getPods() {
    return pods;
  }

  public void setPods(List<StackGresClusterPodStatus> pods) {
    this.pods = pods;
  }

  public String getPodsReady() {
    return podsReady;
  }

  public void setPodsReady(String podsReady) {
    this.podsReady = podsReady;
  }
}
