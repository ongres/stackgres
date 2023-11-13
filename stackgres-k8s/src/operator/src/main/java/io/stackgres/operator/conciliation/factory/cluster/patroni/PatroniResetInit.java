/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

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
import io.stackgres.common.ClusterPath;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.PostgresDataMounts;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.factory.ScriptTemplatesVolumeMounts;
import io.stackgres.operator.conciliation.factory.cluster.ClusterContainerContext;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainer.RESET_PATRONI)
public class PatroniResetInit implements ContainerFactory<ClusterContainerContext> {

  private final PatroniServices patroniServices;

  private final PostgresDataMounts postgresDataMounts;

  private final ScriptTemplatesVolumeMounts templateMounts;

  private final ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables;

  @Inject
  KubectlUtil kubectl;

  @Inject
  public PatroniResetInit(
      @OperatorVersionBinder
      PatroniServices patroniServices,
      PostgresDataMounts postgresDataMounts,
      ScriptTemplatesVolumeMounts templateMounts,
      ResourceFactory<StackGresClusterContext, List<EnvVar>> patroniEnvironmentVariables) {
    this.patroniServices = patroniServices;
    this.postgresDataMounts = postgresDataMounts;
    this.templateMounts = templateMounts;
    this.patroniEnvironmentVariables = patroniEnvironmentVariables;
  }

  @Override
  public boolean isActivated(ClusterContainerContext context) {
    return Optional.of(context.getClusterContext().getSource())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .filter(status -> !Boolean.TRUE.equals(status.getCheck()))
        .filter(status -> !Boolean.TRUE.equals(status.getRollback()))
        .isPresent();
  }

  @Override
  public Container getContainer(ClusterContainerContext context) {

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
            .withName(StackGresInitContainer.RESET_PATRONI.getName())
            .withImage(kubectl.getImageName(clusterContext.getCluster()))
            .withImagePullPolicy(getDefaultPullPolicy())
            .withCommand("/bin/sh", "-ex",
                ClusterPath.TEMPLATES_PATH.path()
                    + "/"
                    + ClusterPath.LOCAL_BIN_RESET_PATRONI_SH_PATH.filename())
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
                ClusterPath.PG_UPGRADE_PATH.envVar(),
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
                    .build(),
                new EnvVarBuilder()
                    .withName("HOME")
                    .withValue("/tmp")
                    .build())
            .addAllToEnv(patroniEnvironmentVariables.createResource(clusterContext))
            .withVolumeMounts(new VolumeMountBuilder()
                .withName(StackGresVolume.USER.getName())
                .withMountPath("/etc/passwd")
                .withSubPath("etc/passwd")
                .withReadOnly(true)
                .build())
            .addAllToVolumeMounts(templateMounts.getVolumeMounts(context))
            .addToVolumeMounts(new VolumeMountBuilder()
                .withName(StackGresVolume.LOCAL_BIN.getName())
                .withMountPath(
                    "/usr/local/bin/dbops/major-version-upgrade/reset-patroni.sh")
                .withSubPath("reset-patroni.sh")
                .withReadOnly(true)
                .build())
            .addAllToVolumeMounts(postgresDataMounts.getVolumeMounts(context))
            .build();
  }

  @Override
  public Map<String, String> getComponentVersions(ClusterContainerContext context) {
    return Map.of();
  }
}
