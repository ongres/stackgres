/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterConfiguration {

  @JsonProperty("sgPostgresConfig")
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

  @JsonProperty("patroni")
  @Valid
  private StackGresClusterPatroni patroni;

  @JsonProperty("credentials")
  @Valid
  private StackGresClusterCredentials credentials;

  private StackGresClusterConfigurationServiceBinding binding;

  @ReferencedField("sgPostgresConfig")
  interface SgPostgresConfig extends FieldReference {
  }

  @ReferencedField("backupPath")
  interface BackupPath extends FieldReference {
  }

  @JsonIgnore
  @AssertTrue(message = "sgPostgresConfig is required",
      payload = { SgPostgresConfig.class })
  public boolean isSgPostgresConfigPresent() {
    return postgresConfig != null;
  }

  @JsonIgnore
  @AssertTrue(message = "backupPath can not be null when sgBackupConfig is set.",
      payload = { BackupPath.class })
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

  public StackGresClusterPatroni getPatroni() {
    return patroni;
  }

  public void setPatroni(StackGresClusterPatroni patroni) {
    this.patroni = patroni;
  }

  public StackGresClusterCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(StackGresClusterCredentials credentials) {
    this.credentials = credentials;
  }

  public StackGresClusterConfigurationServiceBinding getBinding() {
    return binding;
  }

  public void setBinding(
      StackGresClusterConfigurationServiceBinding binding) {
    this.binding = binding;
  }

  @Override
  public int hashCode() {
    return Objects.hash(backupConfig, backupPath, backups, connectionPoolingConfig, credentials,
        patroni, postgresConfig, binding);
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
        && Objects.equals(backupPath, other.backupPath)
        && Objects.equals(backups, other.backups)
        && Objects.equals(connectionPoolingConfig, other.connectionPoolingConfig)
        && Objects.equals(credentials, other.credentials) && Objects.equals(patroni, other.patroni)
        && Objects.equals(postgresConfig, other.postgresConfig)
        && Objects.equals(binding, other.binding);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
