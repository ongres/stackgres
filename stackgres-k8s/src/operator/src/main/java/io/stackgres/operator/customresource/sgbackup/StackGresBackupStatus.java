/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackup;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresBackupStatus implements KubernetesResource {

  private static final long serialVersionUID = 4124027524757318245L;

  private String phase;
  private String pod;
  private String failureReason;
  private String name;
  private String lastModified;
  private String walSegmentBackupStart;
  private String startTime;
  private String finishTime;
  private String hostname;
  private String dataDir;
  private String pgVersion;
  private String startLsn;
  private String finishLsn;
  private Long size;
  private Map<String, String> controlData;
  private Boolean tested;

  public String getPhase() {
    return phase;
  }

  public void setPhase(String phase) {
    this.phase = phase;
  }

  public String getPod() {
    return pod;
  }

  public void setPod(String pod) {
    this.pod = pod;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLastModified() {
    return lastModified;
  }

  public void setLastModified(String lastModified) {
    this.lastModified = lastModified;
  }

  public String getWalSegmentBackupStart() {
    return walSegmentBackupStart;
  }

  public void setWalSegmentBackupStart(String walSegmentBackupStart) {
    this.walSegmentBackupStart = walSegmentBackupStart;
  }

  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  public String getFinishTime() {
    return finishTime;
  }

  public void setFinishTime(String finishTime) {
    this.finishTime = finishTime;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getDataDir() {
    return dataDir;
  }

  public void setDataDir(String dataDir) {
    this.dataDir = dataDir;
  }

  public String getPgVersion() {
    return pgVersion;
  }

  public void setPgVersion(String pgVersion) {
    this.pgVersion = pgVersion;
  }

  public String getStartLsn() {
    return startLsn;
  }

  public void setStartLsn(String startLsn) {
    this.startLsn = startLsn;
  }

  public String getFinishLsn() {
    return finishLsn;
  }

  public void setFinishLsn(String finishLsn) {
    this.finishLsn = finishLsn;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public Map<String, String> getControlData() {
    return controlData;
  }

  public void setControlData(Map<String, String> controlData) {
    this.controlData = controlData;
  }

  public Boolean getTested() {
    return tested;
  }

  public void setTested(Boolean tested) {
    this.tested = tested;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("phase", phase)
        .add("pod", pod)
        .add("failureReason", failureReason)
        .add("name", name)
        .add("lastModified", lastModified)
        .add("walSegmentBackupStart", walSegmentBackupStart)
        .add("startTime", startTime)
        .add("finishTime", finishTime)
        .add("hostname", hostname)
        .add("dataDir", dataDir)
        .add("pgVersion", pgVersion)
        .add("startLsn", startLsn)
        .add("finishLsn", finishLsn)
        .add("size", size)
        .add("controlData", controlData)
        .add("tested", tested)
        .toString();
  }

}
