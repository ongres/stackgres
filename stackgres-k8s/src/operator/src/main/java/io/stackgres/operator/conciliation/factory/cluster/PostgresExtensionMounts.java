/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterPathV2;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PostgresExtensionMounts implements VolumeMountsProvider<ClusterContainerContext> {

  @Override
  public List<VolumeMount> getVolumeMounts(ClusterContainerContext context) {
    final ClusterContext clusterContext = context.getClusterContext();

    return ImmutableList.<VolumeMount>builder()
        .add(
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPathV2.USR_BIN_PATH,
                ClusterPathV2.PG_RELOCATED_USR_BIN_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPathV2.PG_LIB64_PATH,
                ClusterPathV2.PG_RELOCATED_LIB64_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPathV2.PG_SYSTEM_LIB_PATH,
                ClusterPathV2.PG_RELOCATED_SYSTEM_LIB_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPathV2.PG_BIN_PATH,
                ClusterPathV2.PG_RELOCATED_BIN_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPathV2.PG_LIB_PATH,
                ClusterPathV2.PG_RELOCATED_LIB_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPathV2.PG_SHARE_PATH,
                ClusterPathV2.PG_RELOCATED_SHARE_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPathV2.PG_EXTENSION_PATH,
                ClusterPathV2.PG_EXTENSIONS_EXTENSION_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPathV2.PG_EXTRA_BIN_PATH,
                ClusterPathV2.PG_EXTENSIONS_BIN_PATH),
            volumeMountForSubPathFromPostgresData(context, clusterContext,
                ClusterPathV2.PG_EXTRA_LIB_PATH,
                ClusterPathV2.PG_EXTENSIONS_SYSTEM_LIB_PATH))
        .addAll(context.getInstalledExtensions()
            .stream()
            .map(StackGresClusterInstalledExtension::getExtraMounts)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .distinct()
            .map(extraMount -> new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(extraMount)
                .withSubPath(ClusterPathV2.PG_EXTENSIONS_PATH
                    .subPath(clusterContext, ClusterPathV2.PG_BASE_PATH) + extraMount)
                .build())
            .toList())
        .build();
  }

  private VolumeMount volumeMountForSubPathFromPostgresData(
      ClusterContainerContext context,
      final ClusterContext clusterContext,
      final ClusterPathV2 mountPath,
      final ClusterPathV2 subPath) {
    return new VolumeMountBuilder()
        .withName(context.getDataVolumeName())
        .withMountPath(mountPath.path(clusterContext))
        .withSubPath(subPath
            .subPath(clusterContext, ClusterPathV2.PG_BASE_PATH))
        .build();
  }

  @Override
  public List<EnvVar> getDerivedEnvVars(ClusterContainerContext context) {
    final ClusterContext clusterContext = context.getClusterContext();

    return ImmutableList.<EnvVar>builder()
        .add(
            ClusterPathV2.PG_EXTENSIONS_BASE_PATH.envVar(clusterContext),
            ClusterPathV2.PG_EXTENSIONS_PATH.envVar(clusterContext),
            ClusterPathV2.PG_EXTENSIONS_BINARIES_PATH.envVar(clusterContext),
            ClusterPathV2.PG_EXTENSIONS_BIN_PATH.envVar(clusterContext),
            ClusterPathV2.PG_EXTENSIONS_LIB_PATH.envVar(clusterContext),
            ClusterPathV2.PG_EXTENSIONS_SHARE_PATH.envVar(clusterContext),
            ClusterPathV2.PG_EXTENSIONS_EXTENSION_PATH.envVar(clusterContext),
            ClusterPathV2.PG_EXTENSIONS_LIB64_PATH.envVar(clusterContext),
            ClusterPathV2.PG_EXTENSIONS_SYSTEM_LIB_PATH.envVar(clusterContext),
            ClusterPathV2.USR_BIN_PATH.envVar(clusterContext),
            ClusterPathV2.PG_LIB64_PATH.envVar(clusterContext),
            ClusterPathV2.PG_SYSTEM_LIB_PATH.envVar(clusterContext),
            ClusterPathV2.PG_INSTALL_BASE_PATH.envVar(clusterContext),
            ClusterPathV2.PG_BINARIES_PATH.envVar(clusterContext),
            ClusterPathV2.PG_BIN_PATH.envVar(clusterContext),
            ClusterPathV2.PG_LIB_PATH.envVar(clusterContext),
            ClusterPathV2.PG_EXTRA_BIN_PATH.envVar(clusterContext),
            ClusterPathV2.PG_EXTRA_LIB_PATH.envVar(clusterContext),
            ClusterPathV2.PG_SHARE_PATH.envVar(clusterContext),
            ClusterPathV2.PG_EXTENSION_PATH.envVar(clusterContext),
            ClusterPathV2.PG_RELOCATED_BASE_PATH.envVar(clusterContext),
            ClusterPathV2.PG_RELOCATED_PATH.envVar(clusterContext),
            ClusterPathV2.PG_RELOCATED_USR_BIN_PATH.envVar(clusterContext),
            ClusterPathV2.PG_RELOCATED_LIB64_PATH.envVar(clusterContext),
            ClusterPathV2.PG_RELOCATED_SYSTEM_LIB_PATH.envVar(clusterContext),
            ClusterPathV2.PG_RELOCATED_PG_PATH.envVar(clusterContext),
            ClusterPathV2.PG_RELOCATED_BIN_PATH.envVar(clusterContext),
            ClusterPathV2.PG_RELOCATED_LIB_PATH.envVar(clusterContext),
            ClusterPathV2.PG_RELOCATED_SHARE_PATH.envVar(clusterContext),
            ClusterPathV2.PG_RELOCATED_EXTENSION_PATH.envVar(clusterContext),
            ClusterPathV2.PG_UPGRADE_PATH.envVar(clusterContext),
            ClusterPathV2.PATRONI_PATH.envVar(clusterContext),
            ClusterPathV2.WALG_PATH.envVar(clusterContext),
            ClusterPathV2.HDRHISTOGRAM_PATH.envVar(clusterContext),
            ClusterPathV2.PATRONI_BIN_PATH.envVar(clusterContext),
            ClusterPathV2.PATRONICTL_BIN_PATH.envVar(clusterContext),
            ClusterPathV2.WALG_BIN_PATH.envVar(clusterContext),
            ClusterPathV2.HDRHISTOGRAM_BIN_PATH.envVar(clusterContext),
            new EnvVarBuilder()
            .withName("PATH")
            .withValue(String.join(":",
                "/usr/local/sbin",
                "/usr/local/bin",
                ClusterPathV2.PG_BIN_PATH.path(clusterContext),
                ClusterPathV2.PG_EXTRA_BIN_PATH.path(clusterContext),
                ClusterPathV2.PATRONI_PATH.path(clusterContext),
                ClusterPathV2.WALG_PATH.path(clusterContext),
                ClusterPathV2.HDRHISTOGRAM_PATH.path(clusterContext),
                "/usr/sbin",
                "/usr/bin",
                "/sbin",
                "/bin"))
            .build(),
            new EnvVarBuilder()
            .withName("LD_LIBRARY_PATH")
            .withValue(ClusterPathV2.PG_EXTRA_LIB_PATH.path(clusterContext))
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

