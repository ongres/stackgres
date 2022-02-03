/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterConfiguration {

  @JsonProperty("sgPostgresConfig")
  @NotBlank(message = "You need to associate a Postgres configuration to this cluster")
  private String postgresConfig;

  @JsonProperty("sgPoolingConfig")
  private String connectionPoolingConfig;

  @JsonProperty("sgBackupConfig")
  private String backupConfig;

  @JsonProperty("backups")
  @Valid
  private List<StackGresClusterBackupConfiguration> backups;

  public String getPostgresConfig() {
    return postgresConfig;
  }

  public void setPostgresConfig(String postgresConfig) {
    this.postgresConfig = postgresConfig;
  }

  public String getConnectionPoolingConfig() {
    return connectionPoolingConfig;
  }

  public void setConnectionPoolingConfig(String connectionPoolingConfig) {
    this.connectionPoolingConfig = connectionPoolingConfig;
  }

  public String getBackupConfig() {
    return backupConfig;
  }

  public void setBackupConfig(String backupConfig) {
    this.backupConfig = backupConfig;
  }

  public List<StackGresClusterBackupConfiguration> getBackups() {
    return backups;
  }

  public void setBackups(List<StackGresClusterBackupConfiguration> backups) {
    this.backups = backups;
  }

  @Override
  public int hashCode() {
    return Objects.hash(backupConfig, connectionPoolingConfig, postgresConfig, backups);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterConfiguration)) {
      return false;
    }
    StackGresClusterConfiguration other = (StackGresClusterConfiguration) obj;
    return Objects.equals(backupConfig, other.backupConfig)
        && Objects.equals(connectionPoolingConfig, other.connectionPoolingConfig)
        && Objects.equals(postgresConfig, other.postgresConfig)
        && Objects.equals(backups, other.backups);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
