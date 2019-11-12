/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.util.Optional;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;

public class StackGresClusterConfig {

  private final StackGresCluster cluster;
  private final Optional<StackGresPostgresConfig> postgresConfig;
  private final Optional<StackGresBackupConfig> backupConfig;
  private final Optional<StackGresProfile> profile;
  private final ImmutableList<SidecarEntry<?>> sidecars;

  private StackGresClusterConfig(Builder builder) {
    this.cluster = builder.cluster;
    this.postgresConfig = builder.postgresConfig;
    this.backupConfig = builder.backupConfig;
    this.profile = builder.profile;
    this.sidecars = builder.sidecars;
  }

  public StackGresCluster getCluster() {
    return cluster;
  }

  public Optional<StackGresPostgresConfig> getPostgresConfig() {
    return postgresConfig;
  }

  public Optional<StackGresBackupConfig> getBackupConfig() {
    return backupConfig;
  }

  public Optional<StackGresProfile> getProfile() {
    return profile;
  }

  public ImmutableList<SidecarEntry<?>> getSidecars() {
    return sidecars;
  }

  /**
   * Return a sidecar config if present.
   */
  @SuppressWarnings("unchecked")
  public <T extends CustomResource, S extends StackGresSidecarTransformer<T>>
      Optional<T> getSidecarConfig(S sidecar) {
    for (SidecarEntry<?> entry : sidecars) {
      if (entry.getSidecar() == sidecar) {
        return entry.getConfig().map(config -> (T) config);
      }
    }
    throw new IllegalStateException("Sidecar " + sidecar.getClass()
      + " not found in cluster configuration");
  }

  /**
   * Creates builder to build {@link StackGresClusterConfig}.
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link StackGresClusterConfig}.
   */
  public static final class Builder {
    private StackGresCluster cluster;
    private Optional<StackGresPostgresConfig> postgresConfig;
    private Optional<StackGresBackupConfig> backupConfig;
    private Optional<StackGresProfile> profile;
    private ImmutableList<SidecarEntry<?>> sidecars;

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

    public Builder withBackupConfig(Optional<StackGresBackupConfig> backupConfig) {
      this.backupConfig = backupConfig;
      return this;
    }

    public Builder withProfile(Optional<StackGresProfile> profile) {
      this.profile = profile;
      return this;
    }

    public Builder withSidecars(ImmutableList<SidecarEntry<?>> sidecars) {
      this.sidecars = sidecars;
      return this;
    }

    public StackGresClusterConfig build() {
      return new StackGresClusterConfig(this);
    }
  }

}
