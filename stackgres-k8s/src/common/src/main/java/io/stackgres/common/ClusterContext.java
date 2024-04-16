/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterProfile;
import org.immutables.value.Value;
import org.jooq.lambda.Seq;

public interface ClusterContext extends EnvVarContext<StackGresCluster> {

  StackGresCluster getCluster();

  @Value.Lazy
  @Override
  default StackGresCluster getResource() {
    return getCluster();
  }

  @Value.Lazy
  @Override
  default Map<String, String> getEnvironmentVariables() {
    return Seq.of(ClusterEnvVar.values())
        .map(clusterStatefulSetEnvVars -> clusterStatefulSetEnvVars.envVar(getCluster()))
        .toMap(EnvVar::getName, EnvVar::getValue);
  }

  @Value.Lazy
  default boolean calculateDisableClusterPodAntiAffinity() {
    return Optional.ofNullable(getCluster().getSpec().getNonProductionOptions())
        .map(StackGresClusterNonProduction::getDisableClusterPodAntiAffinity)
        .orElse(getClusterProfile()
            .spec().getNonProductionOptions().getDisableClusterPodAntiAffinity());
  }

  @Value.Lazy
  default boolean calculateDisablePatroniResourceRequirements() {
    return Optional.ofNullable(getCluster().getSpec().getNonProductionOptions())
        .map(StackGresClusterNonProduction::getDisablePatroniResourceRequirements)
        .orElse(getClusterProfile()
            .spec().getNonProductionOptions().getDisablePatroniResourceRequirements());
  }

  @Value.Lazy
  default boolean calculateDisableClusterResourceRequirements() {
    return Optional.ofNullable(getCluster().getSpec().getNonProductionOptions())
        .map(StackGresClusterNonProduction::getDisableClusterResourceRequirements)
        .orElse(getClusterProfile()
            .spec().getNonProductionOptions().getDisableClusterResourceRequirements());
  }

  @Value.Lazy
  default @Nonnull StackGresClusterProfile getClusterProfile() {
    return Optional.ofNullable(getCluster().getSpec().getProfile())
        .map(StackGresClusterProfile::fromString)
        .orElse(StackGresClusterProfile.PRODUCTION);
  }

}
