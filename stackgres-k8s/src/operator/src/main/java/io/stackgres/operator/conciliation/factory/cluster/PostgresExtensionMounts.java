/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.PostgresDataMounts;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PostgresExtensionMounts implements VolumeMountsProvider<ClusterContainerContext> {

  @Inject
  PostgresDataMounts postgresData;

  @Inject
  ContainerUserOverrideMounts containerUserOverride;

  @Override
  public List<VolumeMount> getVolumeMounts(ClusterContainerContext context) {
    final ClusterContext clusterContext = context.getClusterContext();

    return ImmutableList.<VolumeMount>builder()
        .addAll(postgresData.getVolumeMounts(context))
        .addAll(containerUserOverride.getVolumeMounts(context))
        .add(
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPath.PG_LIB64_PATH,
                ClusterPath.PG_RELOCATED_LIB64_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPath.PG_BIN_PATH,
                ClusterPath.PG_RELOCATED_BIN_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPath.PG_LIB_PATH,
                ClusterPath.PG_RELOCATED_LIB_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPath.PG_SHARE_PATH,
                ClusterPath.PG_RELOCATED_SHARE_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPath.PG_EXTENSION_PATH,
                ClusterPath.PG_EXTENSIONS_EXTENSION_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPath.PG_EXTRA_BIN_PATH,
                ClusterPath.PG_EXTENSIONS_BIN_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPath.PG_EXTRA_LIB_PATH,
                ClusterPath.PG_EXTENSIONS_LIB64_PATH))
        .addAll(context.getInstalledExtensions()
            .stream()
            .map(StackGresClusterInstalledExtension::getExtraMounts)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .distinct()
            .map(extraMount -> new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(extraMount)
                .withSubPath(ClusterPath.PG_EXTENSIONS_PATH
                    .subPath(clusterContext, ClusterPath.PG_BASE_PATH) + extraMount)
                .build())
            .toList())
        .build();
  }

  private VolumeMount volumeMountForSubPathFromPostgresData(
      ClusterContainerContext context,
      final ClusterContext clusterContext,
      final ClusterPath mountPath,
      final ClusterPath subPath) {
    return new VolumeMountBuilder()
        .withName(context.getDataVolumeName())
        .withMountPath(mountPath.path(clusterContext))
        .withSubPath(subPath
            .subPath(clusterContext, ClusterPath.PG_BASE_PATH))
        .build();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ClusterContainerContext context) {
    final ClusterContext clusterContext = context.getClusterContext();

    return ImmutableList.<EnvVar>builder()
        .addAll(postgresData.getDerivedEnvVars(context))
        .addAll(containerUserOverride.getDerivedEnvVars(context))
        .add(
            ClusterPath.PG_EXTENSIONS_BASE_PATH.envVar(clusterContext),
            ClusterPath.PG_EXTENSIONS_PATH.envVar(clusterContext),
            ClusterPath.PG_EXTENSIONS_BINARIES_PATH.envVar(clusterContext),
            ClusterPath.PG_EXTENSIONS_BIN_PATH.envVar(clusterContext),
            ClusterPath.PG_EXTENSIONS_LIB_PATH.envVar(clusterContext),
            ClusterPath.PG_EXTENSIONS_SHARE_PATH.envVar(clusterContext),
            ClusterPath.PG_EXTENSIONS_EXTENSION_PATH.envVar(clusterContext),
            ClusterPath.PG_EXTENSIONS_LIB64_PATH.envVar(clusterContext),
            ClusterPath.PG_LIB64_PATH.envVar(clusterContext),
            ClusterPath.PG_BINARIES_PATH.envVar(clusterContext),
            ClusterPath.PG_BIN_PATH.envVar(clusterContext),
            ClusterPath.PG_LIB_PATH.envVar(clusterContext),
            ClusterPath.PG_EXTRA_BIN_PATH.envVar(clusterContext),
            ClusterPath.PG_EXTRA_LIB_PATH.envVar(clusterContext),
            ClusterPath.PG_SHARE_PATH.envVar(clusterContext),
            ClusterPath.PG_EXTENSION_PATH.envVar(clusterContext),
            ClusterPath.PG_RELOCATED_BASE_PATH.envVar(clusterContext),
            ClusterPath.PG_RELOCATED_PATH.envVar(clusterContext),
            ClusterPath.PG_RELOCATED_LIB64_PATH.envVar(clusterContext),
            ClusterPath.PG_RELOCATED_BINARIES_PATH.envVar(clusterContext),
            ClusterPath.PG_RELOCATED_BIN_PATH.envVar(clusterContext),
            ClusterPath.PG_RELOCATED_LIB_PATH.envVar(clusterContext),
            ClusterPath.PG_RELOCATED_SHARE_PATH.envVar(clusterContext),
            ClusterPath.PG_RELOCATED_EXTENSION_PATH.envVar(clusterContext),
            ClusterPath.PG_UPGRADE_PATH.envVar(clusterContext),
            new EnvVarBuilder()
            .withName("PATH")
            .withValue(String.join(":",
                "/usr/local/sbin",
                "/usr/local/bin",
                ClusterPath.PG_BIN_PATH.path(clusterContext),
                ClusterPath.PG_EXTRA_BIN_PATH.path(clusterContext),
                "/usr/sbin",
                "/usr/bin",
                "/sbin",
                "/bin"))
            .build(),
            new EnvVarBuilder()
            .withName("LD_LIBRARY_PATH")
            .withValue(ClusterPath.PG_EXTRA_LIB_PATH.path(clusterContext))
            .build(),
            new EnvVarBuilder()
            .withName("EXTRA_MOUNTS")
            .withValue(context.getInstalledExtensions()
                .stream()
                .map(StackGresClusterInstalledExtension::getExtraMounts)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.joining(" ")))
            .build())
        .build();
  }
}

