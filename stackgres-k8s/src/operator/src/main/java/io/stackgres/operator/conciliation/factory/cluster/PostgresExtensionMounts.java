/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

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
import io.stackgres.operator.conciliation.factory.ContainerUserOverrideMounts;
import io.stackgres.operator.conciliation.factory.PostgresDataMounts;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import org.jooq.lambda.Seq;

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
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_LIB64_PATH.path(clusterContext))
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_LIB64_PATH
                    .subPath(clusterContext, ClusterStatefulSetPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_BIN_PATH.path(clusterContext))
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_BIN_PATH
                    .subPath(clusterContext, ClusterStatefulSetPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_LIB_PATH.path(clusterContext))
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_LIB_PATH
                    .subPath(clusterContext, ClusterStatefulSetPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_SHARE_PATH.path(clusterContext))
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_SHARE_PATH
                    .subPath(clusterContext, ClusterStatefulSetPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_EXTENSION_PATH.path(clusterContext))
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH
                    .subPath(clusterContext, ClusterStatefulSetPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_EXTRA_BIN_PATH.path(clusterContext))
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_BIN_PATH
                    .subPath(clusterContext, ClusterStatefulSetPath.PG_BASE_PATH))
                .build(),
            new VolumeMountBuilder()
                .withName(context.getDataVolumeName())
                .withMountPath(ClusterStatefulSetPath.PG_EXTRA_LIB_PATH.path(clusterContext))
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_LIB64_PATH
                    .subPath(clusterContext, ClusterStatefulSetPath.PG_BASE_PATH))
                .build()
        )
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

  @Override
  public List<EnvVar> getDerivedEnvVars(ClusterContainerContext context) {
    final ClusterContext clusterContext = context.getClusterContext();

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
                .withValue(Seq.of(
                    ClusterStatefulSetPath.PG_BIN_PATH.path(clusterContext),
                    ClusterStatefulSetPath.PG_EXTRA_BIN_PATH.path(clusterContext),
                    "/usr/local/sbin",
                    "/usr/local/bin",
                    "/usr/sbin",
                    "/usr/bin",
                    "/sbin",
                    "/bin")
                    .toString(":"))
                .build(),
            new EnvVarBuilder()
                .withName("LD_LIBRARY_PATH")
                .withValue(ClusterStatefulSetPath.PG_EXTRA_LIB_PATH.path(clusterContext))
                .build()
        )
        .build();
  }
}

