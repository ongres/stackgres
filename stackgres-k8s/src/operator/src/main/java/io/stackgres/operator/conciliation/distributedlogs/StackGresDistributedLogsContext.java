/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresDistributedLogsContext
    extends GenerationContext<StackGresDistributedLogs> {

  StackGresConfig getConfig();

  List<StackGresCluster> getConnectedClusters();

  Optional<StackGresProfile> getProfile();

  Optional<StackGresPostgresConfig> getPostgresConfig();

  Optional<StackGresCluster> getCluster();

  Optional<Secret> getDatabaseSecret();

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  public static class Builder extends ImmutableStackGresDistributedLogsContext.Builder {
  }

  public static Builder builder() {
    return new Builder();
  }

}
