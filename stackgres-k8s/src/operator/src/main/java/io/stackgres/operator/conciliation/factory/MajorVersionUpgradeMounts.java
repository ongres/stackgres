/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgresBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecBuilder;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;
import io.stackgres.operator.conciliation.factory.cluster.PostgresExtensionMounts;

@ApplicationScoped
public class MajorVersionUpgradeMounts implements VolumeMountsProvider<ClusterContainerContext> {

  @Inject
  PostgresExtensionMounts postgresExtensionMounts;

  @Override
  public List<VolumeMount> getVolumeMounts(ClusterContainerContext context) {
    final var oldClusterContext = getOldClusterContext(context);

    return ImmutableList.<VolumeMount>builder()
        .addAll(postgresExtensionMounts.getVolumeMounts(context))
        .add(
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_BIN_PATH.path(oldClusterContext))
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_BIN_PATH
                    .subPath(oldClusterContext, ClusterStatefulSetPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_EXTRA_LIB_PATH.path(oldClusterContext))
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_LIB64_PATH
                    .subPath(oldClusterContext, ClusterStatefulSetPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_LIB_PATH.path(oldClusterContext))
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_LIB_PATH
                    .subPath(oldClusterContext, ClusterStatefulSetPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_SHARE_PATH.path(oldClusterContext))
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_SHARE_PATH
                    .subPath(oldClusterContext, ClusterStatefulSetPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_EXTENSION_PATH.path(oldClusterContext))
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH
                    .subPath(oldClusterContext, ClusterStatefulSetPath.PG_BASE_PATH))
                .build()
        ).build();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ClusterContainerContext context) {
    final ClusterContext clusterContext = context.getClusterContext();
    final var oldClusterContext = getOldClusterContext(context);

    return ImmutableList.<EnvVar>builder()
        .addAll(postgresExtensionMounts.getDerivedEnvVars(context))
        .add(new EnvVarBuilder()
                .withName("TARGET_PG_LIB_PATH")
                .withValue(ClusterStatefulSetPath.PG_LIB_PATH.path(clusterContext))
                .build(),
            new EnvVarBuilder()
                .withName("TARGET_PG_EXTRA_LIB_PATH")
                .withValue(ClusterStatefulSetPath.PG_EXTRA_LIB_PATH.path(clusterContext))
                .build(),
            new EnvVarBuilder()
                .withName("SOURCE_PG_LIB_PATH")
                .withValue(ClusterStatefulSetPath.PG_LIB_PATH.path(oldClusterContext))
                .build(),
            new EnvVarBuilder()
                .withName("SOURCE_PG_EXTRA_LIB_PATH")
                .withValue(ClusterStatefulSetPath.PG_EXTRA_LIB_PATH.path(oldClusterContext))
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
