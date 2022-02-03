/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

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

  @JsonProperty("backups")
  private ClusterBackupsConfiguration backups;

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

  public ClusterBackupsConfiguration getBackups() {
    return backups;
  }

  public void setBackups(ClusterBackupsConfiguration backups) {
    this.backups = backups;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterConfiguration that = (ClusterConfiguration) o;
    return Objects.equals(sgPostgresConfig, that.sgPostgresConfig)
        && Objects.equals(sgPoolingConfig, that.sgPoolingConfig)
        && Objects.equals(sgBackupConfig, that.sgBackupConfig)
        && Objects.equals(backups, that.backups);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sgPostgresConfig, sgPoolingConfig, sgBackupConfig, backups);
  }
}
