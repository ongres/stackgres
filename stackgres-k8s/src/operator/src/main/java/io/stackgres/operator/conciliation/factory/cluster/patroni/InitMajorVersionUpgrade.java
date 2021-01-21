/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.common.ClusterStatefulSetEnvVars;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.cluster.factory.ClusterStatefulSetEnvironmentVariables;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V10)
@InitContainer(order = 3)
public class InitMajorVersionUpgrade implements ContainerFactory<StackGresClusterContext> {

  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;

  @Inject
  public InitMajorVersionUpgrade(
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables) {
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
  }

  @Override
  public boolean isActivated(StackGresClusterContext context) {
    return Optional.of(context.getSource())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade).isPresent();
  }

  @Override
  public Container getContainer(StackGresClusterContext context) {
    StackGresClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgradeStatus =
        Optional.of(context.getCluster())
            .map(StackGresCluster::getStatus)
            .map(StackGresClusterStatus::getDbOps)
            .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
            .orElseThrow();
    String primaryInstance = majorVersionUpgradeStatus.getPrimaryInstance();
    String targetVersion = majorVersionUpgradeStatus.getTargetPostgresVersion();
    String sourceVersion = majorVersionUpgradeStatus.getSourcePostgresVersion();
    String sourceMajorVersion = StackGresComponent.POSTGRESQL.findMajorVersion(sourceVersion);
    ImmutableMap<String, String> sourceEnvVars = ImmutableMap.of(
        ClusterStatefulSetEnvVars.POSTGRES_VERSION.name(), sourceVersion,
        ClusterStatefulSetEnvVars.POSTGRES_MAJOR_VERSION.name(), sourceMajorVersion);
    String locale = majorVersionUpgradeStatus.getLocale();
    String encoding = majorVersionUpgradeStatus.getEncoding();
    String dataChecksum = majorVersionUpgradeStatus.getDataChecksum().toString();
    String link = majorVersionUpgradeStatus.getLink().toString();
    String clone = majorVersionUpgradeStatus.getClone().toString();
    String check = majorVersionUpgradeStatus.getCheck().toString();

    final String targetPatroniImageName = StackGresComponent.PATRONI.findImageName(
        StackGresComponent.LATEST,
        ImmutableMap.of(StackGresComponent.POSTGRESQL,
            targetVersion));

    return
        new ContainerBuilder()
            .withName(StackgresClusterContainers.MAJOR_VERSION_UPGRADE)
            .withImage(targetPatroniImageName)
            .withImagePullPolicy("IfNotPresent")
            .withCommand("/bin/sh", "-ex",
                ClusterStatefulSetPath.TEMPLATES_PATH.path()
                    + "/"
                    + ClusterStatefulSetPath.LOCAL_BIN_MAJOR_VERSION_UPGRADE_SH_PATH.filename())
            .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context.getSource()))
            .addToEnv(
                new EnvVarBuilder()
                    .withName("PRIMARY_INSTANCE")
                    .withValue(primaryInstance)
                    .build(),
                new EnvVarBuilder()
                    .withName("TARGET_VERSION")
                    .withValue(targetVersion)
                    .build(),
                new EnvVarBuilder()
                    .withName("SOURCE_VERSION")
                    .withValue(sourceVersion)
                    .build(),
                new EnvVarBuilder()
                    .withName("LOCALE")
                    .withValue(locale)
                    .build(),
                new EnvVarBuilder()
                    .withName("ENCODING")
                    .withValue(encoding)
                    .build(),
                new EnvVarBuilder()
                    .withName("DATA_CHECKSUM")
                    .withValue(dataChecksum)
                    .build(),
                new EnvVarBuilder()
                    .withName("LINK")
                    .withValue(link)
                    .build(),
                new EnvVarBuilder()
                    .withName("CLONE")
                    .withValue(clone)
                    .build(),
                new EnvVarBuilder()
                    .withName("CHECK")
                    .withValue(check)
                    .build(),
                new EnvVarBuilder()
                    .withName("POD_NAME")
                    .withValueFrom(new EnvVarSourceBuilder()
                        .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                        .build())
                    .build())
            .withVolumeMounts(ClusterStatefulSetVolumeConfig.DATA.volumeMount(context),
                ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context))
            .addAllToVolumeMounts(ClusterStatefulSetVolumeConfig.USER.volumeMounts(context))
            .addToVolumeMounts(
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_LIB64_PATH
                            .subPath(context, ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(ClusterStatefulSetPath.PG_LIB64_PATH.path(context))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_LIB_PATH.subPath(context,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(ClusterStatefulSetPath.PG_LIB_PATH.path(context))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_BIN_PATH.subPath(context,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(ClusterStatefulSetPath.PG_BIN_PATH.path(context))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_SHARE_PATH.subPath(context,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(ClusterStatefulSetPath.PG_SHARE_PATH.path(context))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH
                            .subPath(context, ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(ClusterStatefulSetPath.PG_EXTENSION_PATH.path(context))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_BIN_PATH.subPath(context,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(
                            ClusterStatefulSetPath.PG_EXTRA_BIN_PATH.path(context))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_LIB64_PATH
                            .subPath(context, ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(ClusterStatefulSetPath.PG_EXTRA_LIB_PATH.path(context))))
            .addToVolumeMounts(
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_LIB_PATH.subPath(
                            context, sourceEnvVars,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(ClusterStatefulSetPath.PG_LIB_PATH
                            .path(context, sourceEnvVars))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_BIN_PATH.subPath(
                            context, sourceEnvVars,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(ClusterStatefulSetPath.PG_BIN_PATH
                            .path(context, sourceEnvVars))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_SHARE_PATH.subPath(
                            context, sourceEnvVars,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(ClusterStatefulSetPath.PG_SHARE_PATH.
                            path(context, sourceEnvVars))),
                ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                    context,
                    volumeMountBuilder -> volumeMountBuilder
                        .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH.subPath(
                            context, sourceEnvVars,
                            ClusterStatefulSetPath.PG_BASE_PATH))
                        .withMountPath(ClusterStatefulSetPath.PG_EXTENSION_PATH.path(
                            context, sourceEnvVars))))
            .build();
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContext context) {
    return Map.of();
  }

  @Override
  public List<Volume> getVolumes(StackGresClusterContext context) {
    return List.of();
  }
}
