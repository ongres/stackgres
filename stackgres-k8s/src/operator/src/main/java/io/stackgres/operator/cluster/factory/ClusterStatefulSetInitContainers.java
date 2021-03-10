/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackgresClusterContainers;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.patroni.factory.PatroniServices;
import io.stackgres.operatorframework.resource.factory.SubResourceStreamFactory;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterStatefulSetInitContainers
    implements SubResourceStreamFactory<Container, StackGresClusterContext> {

  private final ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables;
  private final PatroniServices patroniServices;

  public ClusterStatefulSetInitContainers(
      ClusterStatefulSetEnvironmentVariables clusterStatefulSetEnvironmentVariables,
      PatroniServices patroniServices) {
    this.clusterStatefulSetEnvironmentVariables = clusterStatefulSetEnvironmentVariables;
    this.patroniServices = patroniServices;
  }

  @Override
  public Stream<Container> streamResources(StackGresClusterContext config) {
    return Seq.of(
        Optional.of(setupArbitraryUser(config)),
        Optional.of(createSetupDataPathsContainer(config)),
        Optional.of(setupScriptsContainer(config)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .append(setupMajorVersionUpgrade(config));
  }

  private Container createSetupDataPathsContainer(StackGresClusterContext config) {
    return new ContainerBuilder()
        .withName("setup-data-paths")
        .withImage(StackGresContext.BUSYBOX_IMAGE)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_DATA_PATHS_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(config))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(config),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(config),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(config))
        .build();
  }

  private Container setupArbitraryUser(StackGresClusterContext config) {
    return new ContainerBuilder()
        .withName("setup-arbitrary-user")
        .withImage(StackGresContext.BUSYBOX_IMAGE)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(config))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(config),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(
            config, volumeMountBuilder -> volumeMountBuilder
                .withSubPath("etc")
                .withMountPath("/local/etc")
                .withReadOnly(false)))
        .build();
  }

  private Container setupScriptsContainer(StackGresClusterContext config) {
    return new ContainerBuilder()
        .withName("setup-scripts")
        .withImage(StackGresContext.BUSYBOX_IMAGE)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_SCRIPTS_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(config))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(config),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(config),
            ClusterStatefulSetVolumeConfig.LOCAL_BIN.volumeMount(config))
        .build();
  }

  private Stream<Container> setupMajorVersionUpgrade(StackGresClusterContext config) {
    if (!Optional.of(config.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .map(majorVersionUpgrade -> true)
        .orElse(false)) {
      return Stream.of();
    }
    StackGresClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgradeStatus =
        Optional.of(config.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .get();
    String primaryInstance = majorVersionUpgradeStatus.getPrimaryInstance();
    String targetVersion = majorVersionUpgradeStatus.getTargetPostgresVersion();
    String sourceVersion = majorVersionUpgradeStatus.getSourcePostgresVersion();
    String locale = majorVersionUpgradeStatus.getLocale();
    String encoding = majorVersionUpgradeStatus.getEncoding();
    String dataChecksum = majorVersionUpgradeStatus.getDataChecksum().toString();
    String link = majorVersionUpgradeStatus.getLink().toString();
    String clone = majorVersionUpgradeStatus.getClone().toString();
    String check = majorVersionUpgradeStatus.getCheck().toString();
    final String sourcePatroniImageName = StackGresComponent.PATRONI.findImageName(
        StackGresComponent.LATEST,
        ImmutableMap.of(StackGresComponent.POSTGRESQL,
            sourceVersion));
    final String targetPatroniImageName = StackGresComponent.PATRONI.findImageName(
        StackGresComponent.LATEST,
        ImmutableMap.of(StackGresComponent.POSTGRESQL,
            targetVersion));
    return Stream.of(
        new ContainerBuilder()
        .withName("copy-binaries")
        .withImage(sourcePatroniImageName)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_COPY_BINARIES_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(config))
        .addToEnv(
            new EnvVarBuilder()
            .withName("PRIMARY_INSTANCE")
            .withValue(primaryInstance)
            .build(),
            new EnvVarBuilder()
            .withName("SOURCE_VERSION")
            .withValue(sourceVersion)
            .build(),
            new EnvVarBuilder()
            .withName("POD_NAME")
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                .build())
            .build())
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.DATA.volumeMount(config),
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(config),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(config),
            ClusterStatefulSetVolumeConfig.LOCAL_BIN.volumeMount(config))
        .build(),
        new ContainerBuilder()
        .withName(StackgresClusterContainers.MAJOR_VERSION_UPGRADE)
        .withImage(targetPatroniImageName)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_MAJOR_VERSION_UPGRADE_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(config))
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
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.DATA.volumeMount(config),
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(config),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(config),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                config,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_UPGRADE_PATH.filename()
                    + "/" + sourceVersion + "/bin")
                .withMountPath("/usr/lib/postgresql/" + sourceVersion + "/bin")),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                config,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_UPGRADE_PATH.filename()
                    + "/" + sourceVersion + "/lib")
                .withMountPath("/usr/lib/postgresql/" + sourceVersion + "/lib")),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                config,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_UPGRADE_PATH.filename()
                    + "/" + sourceVersion + "/share")
                .withMountPath("/usr/share/postgresql/" + sourceVersion)))
        .build(),
        new ContainerBuilder()
        .withName("reset-patroni-initialize")
        .withImage(StackGresContext.KUBECTL_IMAGE)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_RESET_PATRONI_INITIALIZE_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(config))
        .addToEnv(
            new EnvVarBuilder()
            .withName("PRIMARY_INSTANCE")
            .withValue(primaryInstance)
            .build(),
            new EnvVarBuilder()
            .withName("POD_NAME")
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                .build())
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_NAMESPACE")
            .withValue(config.getCluster().getMetadata().getNamespace())
            .build(),
            new EnvVarBuilder()
            .withName("PATRONI_ENDPOINT_NAME")
            .withValue(patroniServices.configName(config))
            .build())
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.DATA.volumeMount(config),
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(config),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(config),
            ClusterStatefulSetVolumeConfig.LOCAL_BIN.volumeMount(config))
        .build());
  }

}
