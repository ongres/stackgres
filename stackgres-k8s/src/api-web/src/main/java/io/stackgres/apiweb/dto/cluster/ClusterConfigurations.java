/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ClusterConfigurations {

  private String sgPostgresConfig;

  private String sgPoolingConfig;

  @Deprecated(since = "1.3.0", forRemoval = true)
  private String sgBackupConfig;

  @Deprecated(since = "1.3.0", forRemoval = true)
  private String backupPath;

  private List<ClusterBackupsConfiguration> backups;

  private ClusterPatroni patroni;

  private ClusterCredentials credentials;

  private ClusterServiceBinding binding;

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

  @Deprecated(since = "1.3.0", forRemoval = true)
  public String getSgBackupConfig() {
    return sgBackupConfig;
  }

  @Deprecated(since = "1.3.0", forRemoval = true)
  public void setSgBackupConfig(String sgBackupConfig) {
    this.sgBackupConfig = sgBackupConfig;
  }

  @Deprecated(since = "1.3.0", forRemoval = true)
  public String getBackupPath() {
    return backupPath;
  }

  @Deprecated(since = "1.3.0", forRemoval = true)
  public void setBackupPath(String backupPath) {
    this.backupPath = backupPath;
  }

  public List<ClusterBackupsConfiguration> getBackups() {
    return backups;
  }

  public void setBackups(List<ClusterBackupsConfiguration> backups) {
    this.backups = backups;
  }

  public ClusterPatroni getPatroni() {
    return patroni;
  }

  public void setPatroni(ClusterPatroni patroni) {
    this.patroni = patroni;
  }

  public ClusterCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(ClusterCredentials credentials) {
    this.credentials = credentials;
  }

  public ClusterServiceBinding getBinding() {
    return binding;
  }

  public void setBinding(ClusterServiceBinding binding) {
    this.binding = binding;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
