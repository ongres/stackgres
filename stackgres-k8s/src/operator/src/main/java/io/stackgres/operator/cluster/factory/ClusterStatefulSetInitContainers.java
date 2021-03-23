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
import io.stackgres.common.ClusterControllerProperty;
import io.stackgres.common.ClusterStatefulSetEnvVars;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.OperatorProperty;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresController;
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
  public Stream<Container> streamResources(StackGresClusterContext context) {
    return Seq.of(
        setupArbitraryUser(context),
        createSetupDataPathsContainer(context),
        setupScriptsContainer(context),
        relocateBinaries(context),
        reconciliationCycle(context))
        .append(setupMajorVersionUpgrade(context));
  }

  private Container createSetupDataPathsContainer(StackGresClusterContext context) {
    return new ContainerBuilder()
        .withName("setup-data-paths")
        .withImage(StackGresContext.BUSYBOX_IMAGE)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_DATA_PATHS_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(context),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(context))
        .build();
  }

  private Container setupArbitraryUser(StackGresClusterContext context) {
    return new ContainerBuilder()
        .withName("setup-arbitrary-user")
        .withImage(StackGresContext.BUSYBOX_IMAGE)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_ARBITRARY_USER_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(
            context, volumeMountBuilder -> volumeMountBuilder
                .withSubPath("etc")
                .withMountPath("/local/etc")
                .withReadOnly(false)))
        .build();
  }

  private Container setupScriptsContainer(StackGresClusterContext context) {
    return new ContainerBuilder()
        .withName("setup-scripts")
        .withImage(StackGresContext.BUSYBOX_IMAGE)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_SETUP_SCRIPTS_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(context),
            ClusterStatefulSetVolumeConfig.LOCAL_BIN.volumeMount(context))
        .build();
  }

  private Container relocateBinaries(StackGresClusterContext context) {
    final String patroniImageName = StackGresComponent.PATRONI.findImageName(
        StackGresComponent.LATEST,
        ImmutableMap.of(StackGresComponent.POSTGRESQL,
            context.getCluster().getSpec().getPostgresVersion()));
    return new ContainerBuilder()
        .withName("relocate-binaries")
        .withImage(patroniImageName)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_RELOCATE_BINARIES_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context))
        .withVolumeMounts(
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(context))
        .build();
  }

  private Container reconciliationCycle(StackGresClusterContext context) {
    return new ContainerBuilder()
        .withName("cluster-reconciliation-cycle")
        .withImage(StackGresController.CLUSTER_CONTROLLER.getImageName())
        .withImagePullPolicy("IfNotPresent")
        .withEnv(new EnvVarBuilder()
            .withName("COMMAND")
            .withValue("run-reconciliation-cycle")
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty.CLUSTER_NAME.getEnvironmentVariableName())
            .withValue(context
                .getCluster().getMetadata().getName())
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty.CLUSTER_NAMESPACE.getEnvironmentVariableName())
            .withValue(context
                .getCluster().getMetadata().getNamespace())
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty.CLUSTER_CONTROLLER_POD_NAME
                .getEnvironmentVariableName())
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                .build())
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty.CLUSTER_CONTROLLER_EXTENSIONS_REPOSITORY_URLS
                .getEnvironmentVariableName())
            .withValue(OperatorProperty.EXTENSIONS_REPOSITORY_URLS
                .getString())
            .build(),
            new EnvVarBuilder()
            .withName(ClusterControllerProperty
                .CLUSTER_CONTROLLER_SKIP_OVERWRITE_SHARED_LIBRARIES
                .getEnvironmentVariableName())
            .withValue(Boolean.FALSE.toString())
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_CONTROLLER_LOG_LEVEL")
            .withValue(System.getenv("OPERATOR_LOG_LEVEL"))
            .build(),
            new EnvVarBuilder()
            .withName("CLUSTER_CONTROLLER_SHOW_STACK_TRACES")
            .withValue(System.getenv("OPERATOR_SHOW_STACK_TRACES"))
            .build(),
            new EnvVarBuilder()
            .withName("DEBUG_CLUSTER_CONTROLLER")
            .withValue(System.getenv("DEBUG_OPERATOR"))
            .build(),
            new EnvVarBuilder()
            .withName("DEBUG_CLUSTER_CONTROLLER_SUSPEND")
            .withValue(System.getenv("DEBUG_OPERATOR_SUSPEND"))
            .build())
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.DATA.volumeMount(context))
        .build();
  }

  private Stream<Container> setupMajorVersionUpgrade(StackGresClusterContext context) {
    if (!Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .map(majorVersionUpgrade -> true)
        .orElse(false)) {
      return Stream.of();
    }
    StackGresClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgradeStatus =
        Optional.of(context.getCluster())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .get();
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
    return Stream.of(
        new ContainerBuilder()
        .withName(StackgresClusterContainers.MAJOR_VERSION_UPGRADE)
        .withImage(targetPatroniImageName)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_MAJOR_VERSION_UPGRADE_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context))
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
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(context))
        .addToVolumeMounts(
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_LIB64_PATH.subPath(context,
                    ClusterStatefulSetPath.PG_BASE_PATH))
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
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH.subPath(context,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(ClusterStatefulSetPath.PG_EXTENSION_PATH.path(context))),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_BIN_PATH.subPath(context,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(
                    ClusterStatefulSetPath.PG_EXTENSIONS_MOUNTED_BIN_PATH.path(context))),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_LIB64_PATH.subPath(context,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(
                    ClusterStatefulSetPath.PG_EXTENSIONS_MOUNTED_LIB64_PATH.path(context))))
        .addToVolumeMounts(
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_LIB_PATH.subPath(context, sourceEnvVars,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(ClusterStatefulSetPath.PG_LIB_PATH.path(context, sourceEnvVars))),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_BIN_PATH.subPath(context, sourceEnvVars,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(ClusterStatefulSetPath.PG_BIN_PATH.path(context, sourceEnvVars))),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_RELOCATED_SHARE_PATH.subPath(context, sourceEnvVars,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(ClusterStatefulSetPath.PG_SHARE_PATH.path(context, sourceEnvVars))),
            ClusterStatefulSetVolumeConfig.DATA.volumeMount(
                context,
                volumeMountBuilder -> volumeMountBuilder
                .withSubPath(ClusterStatefulSetPath.PG_EXTENSIONS_EXTENSION_PATH.subPath(context, sourceEnvVars,
                    ClusterStatefulSetPath.PG_BASE_PATH))
                .withMountPath(ClusterStatefulSetPath.PG_EXTENSION_PATH.path(context, sourceEnvVars))))
        .build(),
        new ContainerBuilder()
        .withName("reset-patroni-initialize")
        .withImage(StackGresContext.KUBECTL_IMAGE)
        .withImagePullPolicy("IfNotPresent")
        .withCommand("/bin/sh", "-ex",
            ClusterStatefulSetPath.TEMPLATES_PATH.path()
            + "/" + ClusterStatefulSetPath.LOCAL_BIN_RESET_PATRONI_INITIALIZE_SH_PATH.filename())
        .withEnv(clusterStatefulSetEnvironmentVariables.listResources(context))
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
            .withValue(context.getCluster().getMetadata().getNamespace())
            .build(),
            new EnvVarBuilder()
            .withName("PATRONI_ENDPOINT_NAME")
            .withValue(patroniServices.configName(context))
            .build())
        .withVolumeMounts(ClusterStatefulSetVolumeConfig.DATA.volumeMount(context),
            ClusterStatefulSetVolumeConfig.TEMPLATES.volumeMount(context),
            ClusterStatefulSetVolumeConfig.USER.volumeMount(context),
            ClusterStatefulSetVolumeConfig.LOCAL_BIN.volumeMount(context))
        .build());
  }

}
