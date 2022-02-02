/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.VolumeMountProviderName;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ClusterInitContainer;
import io.stackgres.operator.conciliation.factory.ContainerContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.PatroniStaticVolume;
import io.stackgres.operator.conciliation.factory.ProviderName;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.VolumeMountsProvider;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V10A1, stopAt = StackGresVersion.V12)
@InitContainer(ClusterInitContainer.RESET_PATRONI_INIT)
public class PatroniResetInit implements ContainerFactory<StackGresClusterContainerContext> {

  private final PatroniServices patroniServices;

  private final VolumeMountsProvider<ContainerContext> postgresDataMounts;

  private final VolumeMountsProvider<ContainerContext> templateMounts;

  private final ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables;

  @Inject
  public PatroniResetInit(
      @OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V12)
          PatroniServices patroniServices,
      @ProviderName(VolumeMountProviderName.POSTGRES_DATA)
          VolumeMountsProvider<ContainerContext> postgresDataMounts,
      @ProviderName(VolumeMountProviderName.SCRIPT_TEMPLATES)
          VolumeMountsProvider<ContainerContext> templateMounts,
      ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables) {
    this.patroniServices = patroniServices;
    this.postgresDataMounts = postgresDataMounts;
    this.templateMounts = templateMounts;
    this.patroniEnvironmentVariables = patroniEnvironmentVariables;
  }

  @Override
  public boolean isActivated(StackGresClusterContainerContext context) {
    return Optional.of(context.getClusterContext().getSource())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade).isPresent();
  }

  @Override
  public Container getContainer(StackGresClusterContainerContext context) {

    final StackGresClusterContext clusterContext = context.getClusterContext();
    StackGresClusterDbOpsMajorVersionUpgradeStatus majorVersionUpgradeStatus =
        Optional.of(clusterContext.getSource())
            .map(StackGresCluster::getStatus)
            .map(StackGresClusterStatus::getDbOps)
            .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
            .orElseThrow();
    String postgresVersion = clusterContext.getSource().getSpec().getPostgres().getVersion();
    String primaryInstance = majorVersionUpgradeStatus.getPrimaryInstance();
    String targetVersion = majorVersionUpgradeStatus.getTargetPostgresVersion();
    String sourceVersion = majorVersionUpgradeStatus.getSourcePostgresVersion();

    return
        new ContainerBuilder()
            .withName("reset-patroni")
            .withImage(StackGresComponent.KUBECTL.get(clusterContext.getCluster())
                .findLatestImageName())
            .withImagePullPolicy("IfNotPresent")
            .withCommand("/bin/sh", "-ex",
                ClusterStatefulSetPath.TEMPLATES_PATH.path()
                    + "/"
                    + ClusterStatefulSetPath.LOCAL_BIN_RESET_PATRONI_SH_PATH.filename())
            .addToEnv(
                new EnvVarBuilder()
                    .withName("PRIMARY_INSTANCE")
                    .withValue(primaryInstance)
                    .build(),
                new EnvVarBuilder()
                    .withName("POSTGRES_VERSION")
                    .withValue(postgresVersion)
                    .build(),
                new EnvVarBuilder()
                    .withName("TARGET_VERSION")
                    .withValue(targetVersion)
                    .build(),
                new EnvVarBuilder()
                    .withName("SOURCE_VERSION")
                    .withValue(sourceVersion)
                    .build(),
                ClusterStatefulSetPath.PG_UPGRADE_PATH.envVar(),
                new EnvVarBuilder()
                    .withName("POD_NAME")
                    .withValueFrom(new EnvVarSourceBuilder()
                        .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                        .build())
                    .build(),
                new EnvVarBuilder()
                    .withName("CLUSTER_NAMESPACE")
                    .withValue(clusterContext.getCluster().getMetadata().getNamespace())
                    .build(),
                new EnvVarBuilder()
                    .withName("PATRONI_ENDPOINT_NAME")
                    .withValue(patroniServices.configName(clusterContext))
                    .build())
            .addAllToEnv(patroniEnvironmentVariables.createResource(clusterContext))
            .withVolumeMounts(new VolumeMountBuilder()
                .withName(PatroniStaticVolume.USER.getVolumeName())
                .withMountPath("/etc/passwd")
                .withSubPath("etc/passwd")
                .withReadOnly(true)
                .build())
            .addAllToVolumeMounts(templateMounts.getVolumeMounts(context))
            .addToVolumeMounts(new VolumeMountBuilder()
                .withName(PatroniStaticVolume.LOCAL_BIN.getVolumeName())
                .withMountPath(
                    "/usr/local/bin/dbops/major-version-upgrade/reset-patroni.sh")
                .withSubPath("reset-patroni.sh")
                .withReadOnly(true)
                .build())
            .addAllToVolumeMounts(postgresDataMounts.getVolumeMounts(context))
            .build();
  }

  @Override
  public Map<String, String> getComponentVersions(StackGresClusterContainerContext context) {
    return Map.of();
  }
}
