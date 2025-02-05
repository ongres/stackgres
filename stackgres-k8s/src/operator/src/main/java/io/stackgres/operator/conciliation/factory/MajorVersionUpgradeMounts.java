/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.PostgresExtensionMounts;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MajorVersionUpgradeMounts implements VolumeMountsProvider<ClusterContainerContext> {

  @Inject
  PostgresExtensionMounts postgresExtensionMounts;

  @Inject
  UserOverrideMounts userOverrideMounts;

  @Override
  public List<VolumeMount> getVolumeMounts(ClusterContainerContext context) {
    if (Optional.of(context.getClusterContext().getSource())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .map(StackGresClusterDbOpsMajorVersionUpgradeStatus::getRollback)
        .orElse(false)) {
      return ImmutableList.<VolumeMount>builder()
          .addAll(postgresExtensionMounts.getVolumeMounts(context))
          .addAll(userOverrideMounts.getVolumeMounts(context))
          .build();
    }

    final var oldClusterContext = getOldClusterContext(context);

    return ImmutableList.<VolumeMount>builder()
        .addAll(postgresExtensionMounts.getVolumeMounts(context))
        .addAll(userOverrideMounts.getVolumeMounts(context))
        .add(
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterPath.PG_BIN_PATH.path(oldClusterContext))
                .withSubPath(ClusterPath.PG_RELOCATED_BIN_PATH
                    .subPath(oldClusterContext, ClusterPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterPath.PG_EXTRA_LIB_PATH.path(oldClusterContext))
                .withSubPath(ClusterPath.PG_EXTENSIONS_LIB64_PATH
                    .subPath(oldClusterContext, ClusterPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterPath.PG_LIB_PATH.path(oldClusterContext))
                .withSubPath(ClusterPath.PG_RELOCATED_LIB_PATH
                    .subPath(oldClusterContext, ClusterPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterPath.PG_SHARE_PATH.path(oldClusterContext))
                .withSubPath(ClusterPath.PG_RELOCATED_SHARE_PATH
                    .subPath(oldClusterContext, ClusterPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterPath.PG_EXTENSION_PATH.path(oldClusterContext))
                .withSubPath(ClusterPath.PG_EXTENSIONS_EXTENSION_PATH
                    .subPath(oldClusterContext, ClusterPath.PG_BASE_PATH))
                .build()
        ).build();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ClusterContainerContext context) {
    if (Optional.of(context.getClusterContext().getSource())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .map(StackGresClusterDbOpsMajorVersionUpgradeStatus::getRollback)
        .orElse(false)) {
      return ImmutableList.<EnvVar>builder()
          .addAll(postgresExtensionMounts.getDerivedEnvVars(context))
          .build();
    }

    final ClusterContext clusterContext = context.getClusterContext();
    final var oldClusterContext = getOldClusterContext(context);

    return ImmutableList.<EnvVar>builder()
        .addAll(postgresExtensionMounts.getDerivedEnvVars(context))
        .add(new EnvVarBuilder()
                .withName("TARGET_PG_LIB_PATH")
                .withValue(ClusterPath.PG_LIB_PATH.path(clusterContext))
                .build(),
            new EnvVarBuilder()
                .withName("TARGET_PG_LIB64_PATH")
                .withValue(ClusterPath.PG_RELOCATED_LIB64_PATH.path(clusterContext))
                .build(),
            new EnvVarBuilder()
                .withName("TARGET_PG_EXTRA_LIB_PATH")
                .withValue(ClusterPath.PG_EXTRA_LIB_PATH.path(clusterContext))
                .build(),
            new EnvVarBuilder()
                .withName("SOURCE_PG_LIB_PATH")
                .withValue(ClusterPath.PG_LIB_PATH.path(oldClusterContext))
                .build(),
            new EnvVarBuilder()
                .withName("SOURCE_PG_LIB64_PATH")
                .withValue(ClusterPath.PG_RELOCATED_LIB64_PATH.path(oldClusterContext))
                .build(),
            new EnvVarBuilder()
                .withName("SOURCE_PG_EXTRA_LIB_PATH")
                .withValue(ClusterPath.PG_EXTRA_LIB_PATH.path(oldClusterContext))
                .build())
        .build();
  }

  private ClusterContext getOldClusterContext(ClusterContainerContext context) {
    final StackGresCluster cluster = context.getClusterContext().getCluster();
    final StackGresCluster oldCluster = new StackGresClusterBuilder(cluster)
        .withSpec(new StackGresClusterSpecBuilder(cluster.getSpec())
            .withPostgres(new StackGresClusterPostgresBuilder(cluster.getSpec().getPostgres())
                .withVersion(context.getOldPostgresVersion().orElseThrow())
                .build())
            .build())
        .build();
    return () -> oldCluster;
  }

}
