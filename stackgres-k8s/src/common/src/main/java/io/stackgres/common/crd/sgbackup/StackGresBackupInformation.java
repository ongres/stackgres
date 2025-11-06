/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackup;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresBackupInformation {

  private String hostname;
  private String pgData;
  private String postgresVersion;
  private String systemIdentifier;

  @Valid
  private StackGresBackupLsn lsn;
  @Valid
  private StackGresBackupSize size;

  private Map<String, String> controlData;
  private String startWalFile;
  private String timeline;
  private String sourcePod;

  @JsonIgnore
  public String getPostgresMajorVersion() {
    return Optional.ofNullable(postgresVersion)
        .filter(version -> version.length() == 6)
        .map(version -> version.substring(0, 2))
        .orElse(null);
  }

  @Deprecated
  public String getHostname() {
    return hostname;
  }

  @Deprecated
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

  public StackGresBackupLsn getLsn() {
    return lsn;
  }

  public void setLsn(StackGresBackupLsn lsn) {
    this.lsn = lsn;
  }

  public StackGresBackupSize getSize() {
    return size;
  }

  public void setSize(StackGresBackupSize size) {
    this.size = size;
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
  public int hashCode() {
    return Objects.hash(controlData, hostname, lsn, pgData, postgresVersion, size, sourcePod,
        startWalFile, systemIdentifier, timeline);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresBackupInformation)) {
      return false;
    }
    StackGresBackupInformation other = (StackGresBackupInformation) obj;
    return Objects.equals(controlData, other.controlData)
        && Objects.equals(hostname, other.hostname) && Objects.equals(lsn, other.lsn)
        && Objects.equals(pgData, other.pgData)
        && Objects.equals(postgresVersion, other.postgresVersion)
        && Objects.equals(size, other.size) && Objects.equals(sourcePod, other.sourcePod)
        && Objects.equals(startWalFile, other.startWalFile)
        && Objects.equals(systemIdentifier, other.systemIdentifier)
        && Objects.equals(timeline, other.timeline);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
