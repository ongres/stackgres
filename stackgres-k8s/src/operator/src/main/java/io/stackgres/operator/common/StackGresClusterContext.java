/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Optional;

import com.google.common.collect.ImmutableList;

import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;

public class StackGresClusterContext {

  private final StackGresCluster cluster;
  private final Optional<StackGresPostgresConfig> postgresConfig;
  private final Optional<StackGresBackupContext> backupContext;
  private final Optional<StackGresProfile> profile;
  private final ImmutableList<SidecarEntry<?, StackGresClusterContext>> sidecars;
  private final ImmutableList<StackGresBackup> backups;
  private final Optional<Prometheus> prometheus;
  private final Optional<StackGresRestoreContext> restoreContext;

  private StackGresClusterContext(Builder builder) {
    this.cluster = builder.cluster;
    this.postgresConfig = builder.postgresConfig;
    this.backupContext = builder.backupContext;
    this.profile = builder.profile;
    this.sidecars = builder.sidecars;
    this.backups = builder.backups;
    this.prometheus = builder.prometheus;
    this.restoreContext = builder.restoreContext;
  }

  public StackGresCluster getCluster() {
    return cluster;
  }

  public Optional<StackGresPostgresConfig> getPostgresConfig() {
    return postgresConfig;
  }

  public Optional<StackGresBackupContext> getBackupContext() {
    return backupContext;
  }

  public Optional<StackGresRestoreContext> getRestoreContext() {
    return restoreContext;
  }

  public Optional<StackGresProfile> getProfile() {
    return profile;
  }

  public ImmutableList<SidecarEntry<?, StackGresClusterContext>> getSidecars() {
    return sidecars;
  }

  public ImmutableList<StackGresBackup> getBackups() {
    return backups;
  }

  public Optional<Prometheus> getPrometheus() {
    return prometheus;
  }

  /**
   * Return a sidecar config if present.
   */
  @SuppressWarnings("unchecked")
  public <T, C, S extends StackGresSidecarTransformer<T, C>>
        Optional<T> getSidecarConfig(S sidecar) {
    for (SidecarEntry<?, ?> entry : sidecars) {
      if (entry.getSidecar() == sidecar) {
        return entry.getConfig().map(config -> (T) config);
      }
    }
    throw new IllegalStateException("Sidecar " + sidecar.getClass()
        + " not found in cluster configuration");
  }

  /**
   * Creates builder to build {@link StackGresClusterContext}.
   *
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link StackGresClusterContext}.
   */
  public static final class Builder {
    private StackGresCluster cluster;
    private Optional<StackGresPostgresConfig> postgresConfig;
    private Optional<StackGresBackupContext> backupContext;
    private Optional<StackGresProfile> profile;
    private ImmutableList<SidecarEntry<?, StackGresClusterContext>> sidecars;
    private ImmutableList<StackGresBackup> backups;
    private Optional<Prometheus> prometheus;
    private Optional<StackGresRestoreContext> restoreContext;

    private Builder() {
    }

    public Builder withCluster(StackGresCluster cluster) {
      this.cluster = cluster;
      return this;
    }

    public Builder withPostgresConfig(Optional<StackGresPostgresConfig> postgresConfig) {
      this.postgresConfig = postgresConfig;
      return this;
    }

    public Builder withBackupContext(Optional<StackGresBackupContext> backupContext) {
      this.backupContext = backupContext;
      return this;
    }

    public Builder withProfile(Optional<StackGresProfile> profile) {
      this.profile = profile;
      return this;
    }

    public Builder withSidecars(ImmutableList<SidecarEntry<?, StackGresClusterContext>> sidecars) {
      this.sidecars = sidecars;
      return this;
    }

    public Builder withBackups(ImmutableList<StackGresBackup> backups) {
      this.backups = backups;
      return this;
    }

    public Builder withPrometheus(Optional<Prometheus> prometheus) {
      this.prometheus = prometheus;
      return this;
    }

    public Builder withRestoreContext(
        Optional<StackGresRestoreContext> restoreContext) {
      this.restoreContext = restoreContext;
      return this;
    }

    public StackGresClusterContext build() {
      return new StackGresClusterContext(this);
    }
  }

}
