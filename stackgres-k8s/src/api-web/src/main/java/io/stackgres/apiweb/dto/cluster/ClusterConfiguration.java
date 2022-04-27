/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;
import java.util.Objects;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterConfiguration {

  @JsonProperty("sgPostgresConfig")
  @NotBlank(message = "You need to associate a Postgres configuration to this cluster")
  private String sgPostgresConfig;

  @JsonProperty("sgPoolingConfig")
  private String sgPoolingConfig;

  @JsonProperty("sgBackupConfig")
  private String sgBackupConfig;

  @JsonProperty("backupPath")
  private String backupPath;

  @JsonProperty("backups")
  private List<ClusterBackupsConfiguration> backups;

  public String getSgPostgresConfig() {
    return sgPostgresConfig;
  }

  public void setSgPostgresConfig(String sgPostgresConfig) {
    this.sgPostgresConfig = sgPostgresConfig;
  }

  public String getSgPoolingConfig() {
    return sgPoolingConfig;
  }

  public void setSgPoolingConfig(String sgPoolingConfig) {
    this.sgPoolingConfig = sgPoolingConfig;
  }

  public String getSgBackupConfig() {
    return sgBackupConfig;
  }

  public void setSgBackupConfig(String sgBackupConfig) {
    this.sgBackupConfig = sgBackupConfig;
  }

  public String getBackupPath() {
    return backupPath;
  }

  public void setBackupPath(String backupPath) {
    this.backupPath = backupPath;
  }

  public List<ClusterBackupsConfiguration> getBackups() {
    return backups;
  }

  public void setBackups(List<ClusterBackupsConfiguration> backups) {
    this.backups = backups;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ClusterConfiguration)) {
      return false;
    }
    ClusterConfiguration other = (ClusterConfiguration) obj;
    return Objects.equals(backupPath, other.backupPath) && Objects.equals(backups, other.backups)
        && Objects.equals(sgBackupConfig, other.sgBackupConfig)
        && Objects.equals(sgPoolingConfig, other.sgPoolingConfig)
        && Objects.equals(sgPostgresConfig, other.sgPostgresConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(backupPath, backups, sgBackupConfig, sgPoolingConfig, sgPostgresConfig);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
