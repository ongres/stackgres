/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterConfigurations {

  private String sgPostgresConfig;

  private String sgPoolingConfig;

  @Deprecated(since = "1.3.0", forRemoval = true)
  private String sgBackupConfig;

  @Deprecated(since = "1.3.0", forRemoval = true)
  private String backupPath;

  @Valid
  private List<StackGresClusterBackupConfiguration> backups;

  @Valid
  private StackGresClusterPatroni patroni;

  @Valid
  private StackGresClusterCredentials credentials;

  @Valid
  private StackGresClusterServiceBinding binding;

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
    return sgPostgresConfig != null;
  }

  @JsonIgnore
  @AssertTrue(message = "backupPath can not be null when sgBackupConfig is set.",
      payload = { BackupPath.class })
  public boolean isBackupPathSetWhenSgBackupConfigIsSet() {
    return sgBackupConfig == null || backupPath != null;
  }

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

  public StackGresClusterServiceBinding getBinding() {
    return binding;
  }

  public void setBinding(
      StackGresClusterServiceBinding binding) {
    this.binding = binding;
  }

  @Override
  public int hashCode() {
    return Objects.hash(backupPath, backups, binding, credentials, patroni, sgBackupConfig,
        sgPoolingConfig, sgPostgresConfig);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterConfigurations)) {
      return false;
    }
    StackGresClusterConfigurations other = (StackGresClusterConfigurations) obj;
    return Objects.equals(backupPath, other.backupPath)
        && Objects.equals(backups, other.backups)
        && Objects.equals(binding, other.binding)
        && Objects.equals(credentials, other.credentials)
        && Objects.equals(patroni, other.patroni)
        && Objects.equals(sgBackupConfig, other.sgBackupConfig)
        && Objects.equals(sgPoolingConfig, other.sgPoolingConfig)
        && Objects.equals(sgPostgresConfig, other.sgPostgresConfig);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}
