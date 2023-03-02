/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.PostgresDataMounts;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;

@ApplicationScoped
public class PostgresExtensionMounts
    implements VolumeMountsProvider<DistributedLogsContainerContext> {

  @Inject
  PostgresDataMounts postgresData;

  @Inject
  ContainerUserOverrideMounts containerUserOverride;

  @Override
  public List<VolumeMount> getVolumeMounts(DistributedLogsContainerContext context) {
    final ClusterContext clusterContext = () -> StackGresDistributedLogsUtil
        .getStackGresClusterForDistributedLogs(context.getDistributedLogsContext().getSource());

    return ImmutableList.<VolumeMount>builder()
        .addAll(postgresData.getVolumeMounts(context))
        .addAll(containerUserOverride.getVolumeMounts(context))
        .add(
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterStatefulSetPath.PG_LIB64_PATH,
                ClusterStatefulSetPath.PG_RELOCATED_LIB64_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterStatefulSetPath.PG_BIN_PATH,
                ClusterStatefulSetPath.PG_RELOCATED_BIN_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterStatefulSetPath.PG_LIB_PATH,
                ClusterStatefulSetPath.PG_RELOCATED_LIB_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterStatefulSetPath.PG_SHARE_PATH,
                ClusterStatefulSetPath.PG_RELOCATED_SHARE_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterStatefulSetPath.PG_EXTENSION_PATH,
                ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterStatefulSetPath.PG_EXTRA_BIN_PATH,
                ClusterStatefulSetPath.PG_EXTENSIONS_BIN_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterStatefulSetPath.PG_EXTRA_LIB_PATH,
                ClusterStatefulSetPath.PG_EXTENSIONS_LIB64_PATH))
        .addAll(context.getInstalledExtensions()
            .stream()
            .flatMap(ie -> Optional.ofNullable(ie.getExtraMounts())
                .orElse(List.of())
                .stream())
            .distinct()
            .map(extraMount -> new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(extraMount)
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_PATH
                    .subPath(clusterContext, ClusterStatefulSetPath.PG_BASE_PATH) + extraMount)
                .build())
            .toList())
        .build();
  }

  private VolumeMount volumeMountForSubPathFromPostgresData(
      DistributedLogsContainerContext context,
      final ClusterContext clusterContext,
      final ClusterStatefulSetPath mountPath,
      final ClusterStatefulSetPath subPath) {
    return new VolumeMountBuilder()
        .withName(context.getDataVolumeName())
        .withMountPath(mountPath.path(clusterContext))
        .withSubPath(subPath
            .subPath(clusterContext, ClusterStatefulSetPath.PG_BASE_PATH))
        .build();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(DistributedLogsContainerContext context) {
    final ClusterContext clusterContext = () -> StackGresDistributedLogsUtil
        .getStackGresClusterForDistributedLogs(context.getDistributedLogsContext().getSource());

    return ImmutableList.<EnvVar>builder()
        .addAll(postgresData.getDerivedEnvVars(context))
        .addAll(containerUserOverride.getDerivedEnvVars(context))
        .add(
            ClusterStatefulSetPath.PG_EXTENSIONS_BASE_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_EXTENSIONS_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_EXTENSIONS_BINARIES_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_EXTENSIONS_BIN_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_EXTENSIONS_LIB_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_EXTENSIONS_SHARE_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_EXTENSIONS_LIB64_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_LIB64_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_BINARIES_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_BIN_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_LIB_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_EXTRA_BIN_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_EXTRA_LIB_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_SHARE_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_EXTENSION_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_RELOCATED_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_RELOCATED_LIB64_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_RELOCATED_BINARIES_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_RELOCATED_BIN_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_RELOCATED_LIB_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_RELOCATED_SHARE_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_RELOCATED_EXTENSION_PATH.envVar(clusterContext),
            ClusterStatefulSetPath.PG_UPGRADE_PATH.envVar(clusterContext),
            new EnvVarBuilder()
                .withName("PATH")
                .withValue(String.join(":",
                    ClusterStatefulSetPath.PG_BIN_PATH.path(clusterContext),
                    ClusterStatefulSetPath.PG_EXTRA_BIN_PATH.path(clusterContext),
                    "/usr/local/sbin",
                    "/usr/local/bin",
                    "/usr/sbin",
                    "/usr/bin",
                    "/sbin",
                    "/bin"))
                .build(),
            new EnvVarBuilder()
                .withName("LD_LIBRARY_PATH")
                .withValue(ClusterStatefulSetPath.PG_EXTRA_LIB_PATH.path(clusterContext))
                .build())
        .build();
  }
}

