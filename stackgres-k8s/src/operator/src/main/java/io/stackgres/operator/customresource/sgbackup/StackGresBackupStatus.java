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
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigSpec;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresBackupStatus implements KubernetesResource {

  private static final long serialVersionUID = 4124027524757318245L;

  private StackGresBackupConfigSpec backupConfig;
  private String phase;
  private String pod;
  private String failureReason;
  private String name;
  private String time;
  private String walFileName;
  private String startTime;
  private String finishTime;
  private String hostname;
  private String dataDir;
  private String pgVersion;
  private String startLsn;
  private String finishLsn;
  private Boolean isPermanent;
  private String systemIdentifier;
  private Long uncompressedSize;
  private Long compressedSize;
  private Map<String, String> controlData;
  private Boolean tested;

  public StackGresBackupConfigSpec getBackupConfig() {
    return backupConfig;
  }

  public void setBackupConfig(StackGresBackupConfigSpec backupConfig) {
    this.backupConfig = backupConfig;
  }

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

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public String getWalFileName() {
    return walFileName;
  }

  public void setWalFileName(String walFileName) {
    this.walFileName = walFileName;
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

  public Boolean getIsPermanent() {
    return isPermanent;
  }

  public void setIsPermanent(Boolean isPermanent) {
    this.isPermanent = isPermanent;
  }

  public String getSystemIdentifier() {
    return systemIdentifier;
  }

  public void setSystemIdentifier(String systemIdentifier) {
    this.systemIdentifier = systemIdentifier;
  }

  public Long getUncompressedSize() {
    return uncompressedSize;
  }

  public void setUncompressedSize(Long uncompressedSize) {
    this.uncompressedSize = uncompressedSize;
  }

  public Long getCompressedSize() {
    return compressedSize;
  }

  public void setCompressedSize(Long compressedSize) {
    this.compressedSize = compressedSize;
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
        .add("backupConfig", backupConfig)
        .add("phase", phase)
        .add("pod", pod)
        .add("failureReason", failureReason)
        .add("name", name)
        .add("time", time)
        .add("walFileName", walFileName)
        .add("startTime", startTime)
        .add("finishTime", finishTime)
        .add("hostname", hostname)
        .add("dataDir", dataDir)
        .add("pgVersion", pgVersion)
        .add("startLsn", startLsn)
        .add("finishLsn", finishLsn)
        .add("isPermanent", isPermanent)
        .add("systemIdentifier", systemIdentifier)
        .add("uncompressedSize", uncompressedSize)
        .add("compressedSize", compressedSize)
        .add("controlData", controlData)
        .add("tested", tested)
        .toString();
  }

}
