/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterProfile;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsNonProduction;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.operator.conciliation.GenerationContext;
import org.immutables.value.Value;

@Value.Immutable
public interface StackGresDistributedLogsContext
    extends GenerationContext<StackGresDistributedLogs> {

  StackGresConfig getConfig();

  StackGresPostgresConfig getPostgresConfig();

  StackGresProfile getProfile();

  List<StackGresCluster> getConnectedClusters();

  Optional<Secret> getDatabaseCredentials();

  @Override
  @Value.Derived
  default StackGresVersion getVersion() {
    return StackGresVersion.getStackGresVersion(getSource());
  }

  @Value.Lazy
  default boolean calculateDisableClusterPodAntiAffinity() {
    return Optional.ofNullable(getSource().getSpec().getNonProductionOptions())
        .map(StackGresDistributedLogsNonProduction::getDisableClusterPodAntiAffinity)
        .orElse(getClusterProfile()
            .spec().getNonProductionOptions().getDisableClusterPodAntiAffinity());
  }

  @Value.Lazy
  default boolean calculateDisablePatroniResourceRequirements() {
    return Optional.ofNullable(getSource().getSpec().getNonProductionOptions())
        .map(StackGresDistributedLogsNonProduction::getDisablePatroniResourceRequirements)
        .orElse(getClusterProfile()
            .spec().getNonProductionOptions().getDisablePatroniResourceRequirements());
  }

  @Value.Lazy
  default boolean calculateDisableClusterResourceRequirements() {
    return Optional.ofNullable(getSource().getSpec().getNonProductionOptions())
        .map(StackGresDistributedLogsNonProduction::getDisableClusterResourceRequirements)
        .orElse(getClusterProfile()
            .spec().getNonProductionOptions().getDisableClusterResourceRequirements());
  }

  @Value.Lazy
  default @Nonnull StackGresClusterProfile getClusterProfile() {
    return Optional.ofNullable(getSource().getSpec().getProfile())
        .map(StackGresClusterProfile::fromString)
        .orElse(StackGresClusterProfile.PRODUCTION);
  }

}
