/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.customresource.sgcluster;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterStatus implements KubernetesResource {

  private static final long serialVersionUID = 4714141925270158016L;

  @Deprecated
  @JsonProperty("cpu_requested")
  private String cpuRequested;

  @Deprecated
  @JsonProperty("cpu_found")
  private String cpuFound;

  @Deprecated
  @JsonProperty("memory_requested")
  private String memoryRequested;

  @Deprecated
  @JsonProperty("memory_found")
  private String memoryFound;

  @Deprecated
  @JsonProperty("memory_used")
  private String memoryUsed;

  @Deprecated
  @JsonProperty("disk_found")
  private String diskFound;

  @Deprecated
  @JsonProperty("disk_used")
  private String diskUsed;

  @Deprecated
  @JsonProperty("average_load_1m")
  private String averageLoad1m;

  @Deprecated
  @JsonProperty("average_load_5m")
  private String averageLoad5m;

  @Deprecated
  @JsonProperty("average_load_10m")
  private String averageLoad10m;

  @JsonProperty("pods")
  private List<StackGresClusterPodStatus> pods;

  @JsonProperty("pods_ready")
  private String podsReady;

  @JsonProperty("replicas")
  private String replicas;

  @JsonProperty("conditions")
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private List<StackGresClusterCondition> conditions = new ArrayList<>();

  @Deprecated
  public String getCpuRequested() {
    return cpuRequested;
  }

  @Deprecated
  public void setCpuRequested(String cpuRequested) {
    this.cpuRequested = cpuRequested;
  }

  @Deprecated
  public String getCpuFound() {
    return cpuFound;
  }

  @Deprecated
  public void setCpuFound(String cpuFound) {
    this.cpuFound = cpuFound;
  }

  @Deprecated
  public String getMemoryRequested() {
    return memoryRequested;
  }

  @Deprecated
  public void setMemoryRequested(String memoryRequested) {
    this.memoryRequested = memoryRequested;
  }

  @Deprecated
  public String getMemoryFound() {
    return memoryFound;
  }

  @Deprecated
  public void setMemoryFound(String memoryFound) {
    this.memoryFound = memoryFound;
  }

  @Deprecated
  public String getMemoryUsed() {
    return memoryUsed;
  }

  @Deprecated
  public void setMemoryUsed(String memoryUsed) {
    this.memoryUsed = memoryUsed;
  }

  @Deprecated
  public String getDiskFound() {
    return diskFound;
  }

  @Deprecated
  public void setDiskFound(String diskFound) {
    this.diskFound = diskFound;
  }

  @Deprecated
  public String getDiskUsed() {
    return diskUsed;
  }

  @Deprecated
  public void setDiskUsed(String diskUsed) {
    this.diskUsed = diskUsed;
  }

  @Deprecated
  public String getAverageLoad1m() {
    return averageLoad1m;
  }

  @Deprecated
  public void setAverageLoad1m(String averageLoad1m) {
    this.averageLoad1m = averageLoad1m;
  }

  @Deprecated
  public String getAverageLoad5m() {
    return averageLoad5m;
  }

  @Deprecated
  public void setAverageLoad5m(String averageLoad5m) {
    this.averageLoad5m = averageLoad5m;
  }

  @Deprecated
  public String getAverageLoad10m() {
    return averageLoad10m;
  }

  @Deprecated
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

  public String getReplicas() {
    return replicas;
  }

  public void setReplicas(String replicas) {
    this.replicas = replicas;
  }

  public List<StackGresClusterCondition> getConditions() {
    return conditions;
  }

  public void setConditions(List<StackGresClusterCondition> conditions) {
    this.conditions = conditions;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("cpu_requested", cpuRequested)
        .add("cpu_found", cpuFound)
        .add("memory_requested", memoryRequested)
        .add("memory_found", memoryFound)
        .add("memory_used", memoryUsed)
        .add("disk_found", diskFound)
        .add("disk_used", diskUsed)
        .add("average_load_1m", averageLoad1m)
        .add("average_load_5m", averageLoad5m)
        .add("average_load_10m", averageLoad10m)
        .add("pods", pods)
        .add("pods_ready", podsReady)
        .add("replicas", replicas)
        .add("conditions", conditions)
        .toString();
  }

}
