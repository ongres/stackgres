/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class KubernetesPod {

  @JsonProperty("namespace")
  private String namespace;

  @JsonProperty("name")
  private String name;

  @JsonProperty("role")
  private String role;

  @JsonProperty("ip")
  private String ip;

  @JsonProperty("status")
  private String status;

  @JsonProperty("containers")
  private Integer containers;

  @JsonProperty("containersReady")
  private Integer containersReady;

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

  @JsonProperty("cpuPsiCgroupAvg10")
  private String cpuPsiCgroupAvg10;

  @JsonProperty("cpuPsiCgroupAvg60")
  private String cpuPsiCgroupAvg60;

  @JsonProperty("cpuPsiCgroupAvg300")
  private String cpuPsiCgroupAvg300;

  @JsonProperty("cpuPsiCgroupTotal")
  private String cpuPsiCgroupTotal;

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

  @JsonProperty("memoryPsiCgroupAvg10")
  private String memoryPsiCgroupAvg10;

  @JsonProperty("memoryPsiCgroupAvg60")
  private String memoryPsiCgroupAvg60;

  @JsonProperty("memoryPsiCgroupAvg300")
  private String memoryPsiCgroupAvg300;

  @JsonProperty("memoryPsiCgroupTotal")
  private String memoryPsiCgroupTotal;

  @JsonProperty("memoryPsiFullAvg10")
  private String memoryPsiFullAvg10;

  @JsonProperty("memoryPsiFullAvg60")
  private String memoryPsiFullAvg60;

  @JsonProperty("memoryPsiFullAvg300")
  private String memoryPsiFullAvg300;

  @JsonProperty("memoryPsiFullTotal")
  private String memoryPsiFullTotal;

  @JsonProperty("memoryPsiFullCgroupAvg10")
  private String memoryPsiFullCgroupAvg10;

  @JsonProperty("memoryPsiFullCgroupAvg60")
  private String memoryPsiFullCgroupAvg60;

  @JsonProperty("memoryPsiFullCgroupAvg300")
  private String memoryPsiFullCgroupAvg300;

  @JsonProperty("memoryPsiFullCgroupTotal")
  private String memoryPsiFullCgroupTotal;

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

  @JsonProperty("diskPsiCgroupAvg10")
  private String diskPsiCgroupAvg10;

  @JsonProperty("diskPsiCgroupAvg60")
  private String diskPsiCgroupAvg60;

  @JsonProperty("diskPsiCgroupAvg300")
  private String diskPsiCgroupAvg300;

  @JsonProperty("diskPsiCgroupTotal")
  private String diskPsiCgroupTotal;

  @JsonProperty("diskPsiFullAvg10")
  private String diskPsiFullAvg10;

  @JsonProperty("diskPsiFullAvg60")
  private String diskPsiFullAvg60;

  @JsonProperty("diskPsiFullAvg300")
  private String diskPsiFullAvg300;

  @JsonProperty("diskPsiFullTotal")
  private String diskPsiFullTotal;

  @JsonProperty("diskPsiFullCgroupAvg10")
  private String diskPsiFullCgroupAvg10;

  @JsonProperty("diskPsiFullCgroupAvg60")
  private String diskPsiFullCgroupAvg60;

  @JsonProperty("diskPsiFullCgroupAvg300")
  private String diskPsiFullCgroupAvg300;

  @JsonProperty("diskPsiFullCgroupTotal")
  private String diskPsiFullCgroupTotal;

  @JsonProperty("averageLoad1m")
  private String averageLoad1m;

  @JsonProperty("averageLoad5m")
  private String averageLoad5m;

  @JsonProperty("averageLoad10m")
  private String averageLoad10m;

  @JsonProperty("connections")
  private String connections;

  @JsonProperty("componentVersions")
  private Map<String, String> componentVersions;

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getIp() {
    return ip;
  }

  public void setIp(String ip) {
    this.ip = ip;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getContainers() {
    return containers;
  }

  public void setContainers(Integer containers) {
    this.containers = containers;
  }

  public Integer getContainersReady() {
    return containersReady;
  }

  public void setContainersReady(Integer containersReady) {
    this.containersReady = containersReady;
  }

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

  public String getCpuPsiCgroupAvg10() {
    return cpuPsiCgroupAvg10;
  }

  public void setCpuPsiCgroupAvg10(String cpuPsiCgroupAvg10) {
    this.cpuPsiCgroupAvg10 = cpuPsiCgroupAvg10;
  }

  public String getCpuPsiCgroupAvg60() {
    return cpuPsiCgroupAvg60;
  }

  public void setCpuPsiCgroupAvg60(String cpuPsiCgroupAvg60) {
    this.cpuPsiCgroupAvg60 = cpuPsiCgroupAvg60;
  }

  public String getCpuPsiCgroupAvg300() {
    return cpuPsiCgroupAvg300;
  }

  public void setCpuPsiCgroupAvg300(String cpuPsiCgroupAvg300) {
    this.cpuPsiCgroupAvg300 = cpuPsiCgroupAvg300;
  }

  public String getCpuPsiCgroupTotal() {
    return cpuPsiCgroupTotal;
  }

  public void setCpuPsiCgroupTotal(String cpuPsiCgroupTotal) {
    this.cpuPsiCgroupTotal = cpuPsiCgroupTotal;
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

  public String getMemoryPsiCgroupAvg10() {
    return memoryPsiCgroupAvg10;
  }

  public void setMemoryPsiCgroupAvg10(String memoryPsiCgroupAvg10) {
    this.memoryPsiCgroupAvg10 = memoryPsiCgroupAvg10;
  }

  public String getMemoryPsiCgroupAvg60() {
    return memoryPsiCgroupAvg60;
  }

  public void setMemoryPsiCgroupAvg60(String memoryPsiCgroupAvg60) {
    this.memoryPsiCgroupAvg60 = memoryPsiCgroupAvg60;
  }

  public String getMemoryPsiCgroupAvg300() {
    return memoryPsiCgroupAvg300;
  }

  public void setMemoryPsiCgroupAvg300(String memoryPsiCgroupAvg300) {
    this.memoryPsiCgroupAvg300 = memoryPsiCgroupAvg300;
  }

  public String getMemoryPsiCgroupTotal() {
    return memoryPsiCgroupTotal;
  }

  public void setMemoryPsiCgroupTotal(String memoryPsiCgroupTotal) {
    this.memoryPsiCgroupTotal = memoryPsiCgroupTotal;
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

  public String getMemoryPsiFullCgroupAvg10() {
    return memoryPsiFullCgroupAvg10;
  }

  public void setMemoryPsiFullCgroupAvg10(String memoryPsiFullCgroupAvg10) {
    this.memoryPsiFullCgroupAvg10 = memoryPsiFullCgroupAvg10;
  }

  public String getMemoryPsiFullCgroupAvg60() {
    return memoryPsiFullCgroupAvg60;
  }

  public void setMemoryPsiFullCgroupAvg60(String memoryPsiFullCgroupAvg60) {
    this.memoryPsiFullCgroupAvg60 = memoryPsiFullCgroupAvg60;
  }

  public String getMemoryPsiFullCgroupAvg300() {
    return memoryPsiFullCgroupAvg300;
  }

  public void setMemoryPsiFullCgroupAvg300(String memoryPsiFullCgroupAvg300) {
    this.memoryPsiFullCgroupAvg300 = memoryPsiFullCgroupAvg300;
  }

  public String getMemoryPsiFullCgroupTotal() {
    return memoryPsiFullCgroupTotal;
  }

  public void setMemoryPsiFullCgroupTotal(String memoryPsiFullCgroupTotal) {
    this.memoryPsiFullCgroupTotal = memoryPsiFullCgroupTotal;
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

  public String getDiskPsiCgroupAvg10() {
    return diskPsiCgroupAvg10;
  }

  public void setDiskPsiCgroupAvg10(String diskPsiCgroupAvg10) {
    this.diskPsiCgroupAvg10 = diskPsiCgroupAvg10;
  }

  public String getDiskPsiCgroupAvg60() {
    return diskPsiCgroupAvg60;
  }

  public void setDiskPsiCgroupAvg60(String diskPsiCgroupAvg60) {
    this.diskPsiCgroupAvg60 = diskPsiCgroupAvg60;
  }

  public String getDiskPsiCgroupAvg300() {
    return diskPsiCgroupAvg300;
  }

  public void setDiskPsiCgroupAvg300(String diskPsiCgroupAvg300) {
    this.diskPsiCgroupAvg300 = diskPsiCgroupAvg300;
  }

  public String getDiskPsiCgroupTotal() {
    return diskPsiCgroupTotal;
  }

  public void setDiskPsiCgroupTotal(String diskPsiCgroupTotal) {
    this.diskPsiCgroupTotal = diskPsiCgroupTotal;
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

  public String getDiskPsiFullCgroupAvg10() {
    return diskPsiFullCgroupAvg10;
  }

  public void setDiskPsiFullCgroupAvg10(String diskPsiFullCgroupAvg10) {
    this.diskPsiFullCgroupAvg10 = diskPsiFullCgroupAvg10;
  }

  public String getDiskPsiFullCgroupAvg60() {
    return diskPsiFullCgroupAvg60;
  }

  public void setDiskPsiFullCgroupAvg60(String diskPsiFullCgroupAvg60) {
    this.diskPsiFullCgroupAvg60 = diskPsiFullCgroupAvg60;
  }

  public String getDiskPsiFullCgroupAvg300() {
    return diskPsiFullCgroupAvg300;
  }

  public void setDiskPsiFullCgroupAvg300(String diskPsiFullCgroupAvg300) {
    this.diskPsiFullCgroupAvg300 = diskPsiFullCgroupAvg300;
  }

  public String getDiskPsiFullCgroupTotal() {
    return diskPsiFullCgroupTotal;
  }

  public void setDiskPsiFullCgroupTotal(String diskPsiFullCgroupTotal) {
    this.diskPsiFullCgroupTotal = diskPsiFullCgroupTotal;
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

  public Map<String, String> getComponentVersions() {
    return componentVersions;
  }

  public void setComponentVersions(Map<String, String> componentVersions) {
    this.componentVersions = componentVersions;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
