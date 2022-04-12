/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.backup;

import java.util.Map;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class BackupInformation {

  private String startWalFile;

  private String hostname;
  private String pgData;
  private String postgresVersion;

  @Valid
  private BackupLsn lsn;
  private String systemIdentifier;

  @Valid
  private BackupSize size;
  private Map<String, String> controlData;

  private String timeline;
  private String sourcePod;

  public void setControlData(Map<String, String> controlData) {
    this.controlData = controlData;
  }

  public Map<String, String> getControlData() {
    return controlData;
  }

  public void setSystemIdentifier(String systemIdentifier) {
    this.systemIdentifier = systemIdentifier;
  }

  public String getSystemIdentifier() {
    return systemIdentifier;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPgData(String pgData) {
    this.pgData = pgData;
  }

  public String getPgData() {
    return pgData;
  }

  @Deprecated
  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  @Deprecated
  public String getHostname() {
    return hostname;
  }

  public void setStartWalFile(String startWalFile) {
    this.startWalFile = startWalFile;
  }

  public String getStartWalFile() {
    return startWalFile;
  }

  public BackupSize getSize() {
    return size;
  }

  public void setSize(BackupSize size) {
    this.size = size;
  }

  public BackupLsn getLsn() {
    return lsn;
  }

  public void setLsn(BackupLsn lsn) {
    this.lsn = lsn;
  }

  public String getTimeline() {
    return timeline;
  }

  public void setTimeline(String timeline) {
    this.timeline = timeline;
  }

  public String getSourcePod() {
    return sourcePod;
  }

  public void setSourcePod(String sourcePod) {
    this.sourcePod = sourcePod;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
