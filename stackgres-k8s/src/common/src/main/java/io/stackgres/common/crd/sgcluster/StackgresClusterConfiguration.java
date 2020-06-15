/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackgresClusterConfiguration {

  @JsonProperty("sgPostgresConfig")
  @NotBlank(message = "You need to associate a Postgres configuration to this cluster")
  private String postgresConfig;

  @JsonProperty("sgPoolingConfig")
  private String connectionPoolingConfig;

  @JsonProperty("sgBackupConfig")
  private String backupConfig;

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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("postgresConfig", postgresConfig)
        .add("connectionPoolingConfig", connectionPoolingConfig)
        .add("backupConfig", backupConfig)
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
    StackgresClusterConfiguration that = (StackgresClusterConfiguration) o;
    return Objects.equals(postgresConfig, that.postgresConfig)
        && Objects.equals(connectionPoolingConfig, that.connectionPoolingConfig)
        && Objects.equals(backupConfig, that.backupConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(postgresConfig, connectionPoolingConfig, backupConfig);
  }
}
