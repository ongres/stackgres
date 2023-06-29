/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.cluster.ClusterStats;
import io.stackgres.apiweb.dto.cluster.KubernetesPod;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterClusterStats implements ClusterStats {

  @JsonProperty("cpuRequested")
  private String cpuRequested;

  @JsonProperty("cpuFound")
  private String cpuFound;

  @JsonProperty("cpuPsiAvg10")
  private String cpuPsiAvg10;

  @JsonProperty("cpuPsiAvg60")
  private String cpuPsiAvg60;

  @JsonProperty("cpuPsiAvg300")
  private String cpuPsiAvg300;

  @JsonProperty("cpuPsiTotal")
  private String cpuPsiTotal;

  @JsonProperty("memoryRequested")
  private String memoryRequested;

  @JsonProperty("memoryFound")
  private String memoryFound;

  @JsonProperty("memoryUsed")
  private String memoryUsed;

  @JsonProperty("memoryPsiAvg10")
  private String memoryPsiAvg10;

  @JsonProperty("memoryPsiAvg60")
  private String memoryPsiAvg60;

  @JsonProperty("memoryPsiAvg300")
  private String memoryPsiAvg300;

  @JsonProperty("memoryPsiTotal")
  private String memoryPsiTotal;

  @JsonProperty("memoryPsiFullAvg10")
  private String memoryPsiFullAvg10;

  @JsonProperty("memoryPsiFullAvg60")
  private String memoryPsiFullAvg60;

  @JsonProperty("memoryPsiFullAvg300")
  private String memoryPsiFullAvg300;

  @JsonProperty("memoryPsiFullTotal")
  private String memoryPsiFullTotal;

  @JsonProperty("diskRequested")
  private String diskRequested;

  @JsonProperty("diskFound")
  private String diskFound;

  @JsonProperty("diskUsed")
  private String diskUsed;

  @JsonProperty("diskPsiAvg10")
  private String diskPsiAvg10;

  @JsonProperty("diskPsiAvg60")
  private String diskPsiAvg60;

  @JsonProperty("diskPsiAvg300")
  private String diskPsiAvg300;

  @JsonProperty("diskPsiTotal")
  private String diskPsiTotal;

  @JsonProperty("diskPsiFullAvg10")
  private String diskPsiFullAvg10;

  @JsonProperty("diskPsiFullAvg60")
  private String diskPsiFullAvg60;

  @JsonProperty("diskPsiFullAvg300")
  private String diskPsiFullAvg300;

  @JsonProperty("diskPsiFullTotal")
  private String diskPsiFullTotal;

  @JsonProperty("averageLoad1m")
  private String averageLoad1m;

  @JsonProperty("averageLoad5m")
  private String averageLoad5m;

  @JsonProperty("averageLoad10m")
  private String averageLoad10m;

  @JsonProperty("connections")
  private String connections;

  @JsonProperty("pods")
  private List<KubernetesPod> pods;

  @JsonProperty("podsReady")
  private Integer podsReady;

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

  public String getCpuPsiAvg10() {
    return cpuPsiAvg10;
  }

  public void setCpuPsiAvg10(String cpuPsiAvg10) {
    this.cpuPsiAvg10 = cpuPsiAvg10;
  }

  public String getCpuPsiAvg60() {
    return cpuPsiAvg60;
  }

  public void setCpuPsiAvg60(String cpuPsiAvg60) {
    this.cpuPsiAvg60 = cpuPsiAvg60;
  }

  public String getCpuPsiAvg300() {
    return cpuPsiAvg300;
  }

  public void setCpuPsiAvg300(String cpuPsiAvg300) {
    this.cpuPsiAvg300 = cpuPsiAvg300;
  }

  public String getCpuPsiTotal() {
    return cpuPsiTotal;
  }

  public void setCpuPsiTotal(String cpuPsiTotal) {
    this.cpuPsiTotal = cpuPsiTotal;
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

  public String getMemoryPsiAvg10() {
    return memoryPsiAvg10;
  }

  public void setMemoryPsiAvg10(String memoryPsiAvg10) {
    this.memoryPsiAvg10 = memoryPsiAvg10;
  }

  public String getMemoryPsiAvg60() {
    return memoryPsiAvg60;
  }

  public void setMemoryPsiAvg60(String memoryPsiAvg60) {
    this.memoryPsiAvg60 = memoryPsiAvg60;
  }

  public String getMemoryPsiAvg300() {
    return memoryPsiAvg300;
  }

  public void setMemoryPsiAvg300(String memoryPsiAvg300) {
    this.memoryPsiAvg300 = memoryPsiAvg300;
  }

  public String getMemoryPsiTotal() {
    return memoryPsiTotal;
  }

  public void setMemoryPsiTotal(String memoryPsiTotal) {
    this.memoryPsiTotal = memoryPsiTotal;
  }

  public String getMemoryPsiFullAvg10() {
    return memoryPsiFullAvg10;
  }

  public void setMemoryPsiFullAvg10(String memoryPsiFullAvg10) {
    this.memoryPsiFullAvg10 = memoryPsiFullAvg10;
  }

  public String getMemoryPsiFullAvg60() {
    return memoryPsiFullAvg60;
  }

  public void setMemoryPsiFullAvg60(String memoryPsiFullAvg60) {
    this.memoryPsiFullAvg60 = memoryPsiFullAvg60;
  }

  public String getMemoryPsiFullAvg300() {
    return memoryPsiFullAvg300;
  }

  public void setMemoryPsiFullAvg300(String memoryPsiFullAvg300) {
    this.memoryPsiFullAvg300 = memoryPsiFullAvg300;
  }

  public String getMemoryPsiFullTotal() {
    return memoryPsiFullTotal;
  }

  public void setMemoryPsiFullTotal(String memoryPsiFullTotal) {
    this.memoryPsiFullTotal = memoryPsiFullTotal;
  }

  public String getDiskRequested() {
    return diskRequested;
  }

  public void setDiskRequested(String diskRequested) {
    this.diskRequested = diskRequested;
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

  public String getDiskPsiAvg10() {
    return diskPsiAvg10;
  }

  public void setDiskPsiAvg10(String diskPsiAvg10) {
    this.diskPsiAvg10 = diskPsiAvg10;
  }

  public String getDiskPsiAvg60() {
    return diskPsiAvg60;
  }

  public void setDiskPsiAvg60(String diskPsiAvg60) {
    this.diskPsiAvg60 = diskPsiAvg60;
  }

  public String getDiskPsiAvg300() {
    return diskPsiAvg300;
  }

  public void setDiskPsiAvg300(String diskPsiAvg300) {
    this.diskPsiAvg300 = diskPsiAvg300;
  }

  public String getDiskPsiTotal() {
    return diskPsiTotal;
  }

  public void setDiskPsiTotal(String diskPsiTotal) {
    this.diskPsiTotal = diskPsiTotal;
  }

  public String getDiskPsiFullAvg10() {
    return diskPsiFullAvg10;
  }

  public void setDiskPsiFullAvg10(String diskPsiFullAvg10) {
    this.diskPsiFullAvg10 = diskPsiFullAvg10;
  }

  public String getDiskPsiFullAvg60() {
    return diskPsiFullAvg60;
  }

  public void setDiskPsiFullAvg60(String diskPsiFullAvg60) {
    this.diskPsiFullAvg60 = diskPsiFullAvg60;
  }

  public String getDiskPsiFullAvg300() {
    return diskPsiFullAvg300;
  }

  public void setDiskPsiFullAvg300(String diskPsiFullAvg300) {
    this.diskPsiFullAvg300 = diskPsiFullAvg300;
  }

  public String getDiskPsiFullTotal() {
    return diskPsiFullTotal;
  }

  public void setDiskPsiFullTotal(String diskPsiFullTotal) {
    this.diskPsiFullTotal = diskPsiFullTotal;
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

  public String getConnections() {
    return connections;
  }

  public void setConnections(String connections) {
    this.connections = connections;
  }

  public List<KubernetesPod> getPods() {
    return pods;
  }

  public void setPods(List<KubernetesPod> pods) {
    this.pods = pods;
  }

  public Integer getPodsReady() {
    return podsReady;
  }

  public void setPodsReady(Integer podsReady) {
    this.podsReady = podsReady;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
