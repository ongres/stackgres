/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackup;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresBackupInformation {

  private String hostname;
  private String pgData;
  private String postgresVersion;
  private String systemIdentifier;

  private StackgresBackupLsn lsn;
  private StackgresBackupSize size;

  private Map<String, String> controlData;
  private String startWalFile;

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getPgData() {
    return pgData;
  }

  public void setPgData(String pgData) {
    this.pgData = pgData;
  }

  public String getPostgresVersion() {
    return postgresVersion;
  }

  public void setPostgresVersion(String postgresVersion) {
    this.postgresVersion = postgresVersion;
  }

  public String getSystemIdentifier() {
    return systemIdentifier;
  }

  public void setSystemIdentifier(String systemIdentifier) {
    this.systemIdentifier = systemIdentifier;
  }

  public Map<String, String> getControlData() {
    return controlData;
  }

  public void setControlData(Map<String, String> controlData) {
    this.controlData = controlData;
  }

  public String getStartWalFile() {
    return startWalFile;
  }

  public void setStartWalFile(String startWalFile) {
    this.startWalFile = startWalFile;
  }

  public StackgresBackupLsn getLsn() {
    return lsn;
  }

  public void setLsn(StackgresBackupLsn lsn) {
    this.lsn = lsn;
  }

  public StackgresBackupSize getSize() {
    return size;
  }

  public void setSize(StackgresBackupSize size) {
    this.size = size;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("hostname", hostname)
        .add("pgData", pgData)
        .add("postgresVersion", postgresVersion)
        .add("systemIdentifier", systemIdentifier)
        .add("lsn", lsn)
        .add("size", size)
        .add("controlData", controlData)
        .add("startWalFile", startWalFile)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresBackupInformation that = (StackGresBackupInformation) o;
    return Objects.equals(hostname, that.hostname)
        && Objects.equals(pgData, that.pgData)
        && Objects.equals(postgresVersion, that.postgresVersion)
        && Objects.equals(systemIdentifier, that.systemIdentifier) && Objects.equals(lsn, that.lsn)
        && Objects.equals(size, that.size) && Objects.equals(controlData, that.controlData)
        && Objects.equals(startWalFile, that.startWalFile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hostname, pgData, postgresVersion, systemIdentifier,
        lsn, size, controlData, startWalFile);
  }
}
