/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;

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
  @Deprecated(since = "1.3.0", forRemoval = true)
  private String backupConfig;

  @JsonProperty("backupPath")
  @Deprecated(since = "1.3.0", forRemoval = true)
  private String backupPath;

  @JsonProperty("backups")
  @Valid
  private List<StackGresClusterBackupConfiguration> backups;

  @ReferencedField("backupPath")
  interface BackupPath extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "backupPath can not be null when sgBackupConfig is set.",
      payload = {BackupPath.class})
  public boolean isBackupPathSetWhenSgBackupConfigIsSet() {
    return backupConfig == null || backupPath != null;
  }

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

  public String getBackupPath() {
    return backupPath;
  }

  public void setBackupPath(String backupPath) {
    this.backupPath = backupPath;
  }

  public List<StackGresClusterBackupConfiguration> getBackups() {
    return backups;
  }

  public void setBackups(List<StackGresClusterBackupConfiguration> backups) {
    this.backups = backups;
  }

  @Override
  public int hashCode() {
    return Objects.hash(backupConfig, backupPath, backups, connectionPoolingConfig, postgresConfig);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof StackGresClusterConfiguration other
        && Objects.equals(backupConfig, other.backupConfig)
        && Objects.equals(backupPath, other.backupPath)
        && Objects.equals(backups, other.backups)
        && Objects.equals(connectionPoolingConfig, other.connectionPoolingConfig)
        && Objects.equals(postgresConfig, other.postgresConfig);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
