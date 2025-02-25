/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static io.stackgres.common.StackGresUtil.getDefaultPullPolicy;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ConfigMapEnvSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvFromSourceBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelector;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresInitContainer;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsMajorVersionUpgradeStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ContainerFactory;
import io.stackgres.operator.conciliation.factory.InitContainer;
import io.stackgres.operator.conciliation.factory.MajorVersionUpgradeMounts;
import io.stackgres.operator.conciliation.factory.TemplatesMounts;
import io.stackgres.operator.conciliation.factory.cluster.patroni.PatroniConfigMap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
@InitContainer(StackGresInitContainer.MAJOR_VERSION_UPGRADE)
public class MajorVersionUpgrade implements ContainerFactory<ClusterContainerContext> {

  private final MajorVersionUpgradeMounts majorVersionUpgradeMounts;
  private final TemplatesMounts templateMounts;

  @Inject
  public MajorVersionUpgrade(
      MajorVersionUpgradeMounts majorVersionUpgradeMounts,
      TemplatesMounts templateMounts) {
    this.majorVersionUpgradeMounts = majorVersionUpgradeMounts;
    this.templateMounts = templateMounts;
  }

  @Override
  public boolean isActivated(ClusterContainerContext context) {
    return Optional.of(context.getClusterContext().getSource())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .isPresent()
        && (Optional.of(context.getClusterContext().getSource())
            .map(StackGresCluster::getStatus)
            .map(StackGresClusterStatus::getDbOps)
            .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
            .map(StackGresClusterDbOpsMajorVersionUpgradeStatus::getSourcePostgresVersion)
            .map(context.getClusterContext().getCluster()
                .getSpec().getPostgres().getVersion()::equals)
            .map(equals -> !equals)
            .orElse(false)
            || Optional.of(context.getClusterContext().getSource())
            .map(StackGresCluster::getStatus)
            .map(StackGresClusterStatus::getDbOps)
            .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
            .map(StackGresClusterDbOpsMajorVersionUpgradeStatus::getRollback)
            .orElse(false));
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
    String locale = majorVersionUpgradeStatus.getLocale();
    String encoding = majorVersionUpgradeStatus.getEncoding();
    String dataChecksum = majorVersionUpgradeStatus.getDataChecksum().toString();
    String link = majorVersionUpgradeStatus.getLink().toString();
    String clone = majorVersionUpgradeStatus.getClone().toString();
    String check = majorVersionUpgradeStatus.getCheck().toString();
    String rollback = Optional.ofNullable(majorVersionUpgradeStatus.getRollback())
        .map(Object::toString)
        .orElse(Boolean.FALSE.toString());

    final String targetPatroniImageName = StackGresUtil.getPatroniImageName(
        clusterContext.getCluster(), targetVersion);

    final ClusterContainerContext majorVersoinUpgradeContainerContext =
        ImmutableClusterContainerContext.builder()
            .from(context)
            .oldPostgresVersion(sourceVersion)
            .build();
    return
        new ContainerBuilder()
            .withName(StackGresInitContainer.MAJOR_VERSION_UPGRADE.getName())
            .withImage(targetPatroniImageName)
            .withImagePullPolicy(getDefaultPullPolicy())
            .withCommand("/bin/sh", "-ex",
                ClusterPath.TEMPLATES_PATH.path()
                    + "/"
                    + ClusterPath.LOCAL_BIN_MAJOR_VERSION_UPGRADE_SH_PATH.filename())
            .withEnvFrom(new EnvFromSourceBuilder()
                .withConfigMapRef(new ConfigMapEnvSourceBuilder()
                    .withName(PatroniConfigMap.name(clusterContext)).build())
                .build())
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
                    .withName("ROLLBACK")
                    .withValue(rollback)
                    .build(),
                new EnvVarBuilder()
                    .withName("POD_NAME")
                    .withValueFrom(new EnvVarSourceBuilder()
                        .withFieldRef(new ObjectFieldSelector("v1", "metadata.name"))
                        .build())
                    .build(),
                ClusterPath.ETC_POSTGRES_PATH.envVar())
            .addAllToEnv(majorVersionUpgradeMounts
                .getDerivedEnvVars(majorVersoinUpgradeContainerContext))
            .withVolumeMounts(templateMounts.getVolumeMounts(context))
            .addAllToVolumeMounts(
                majorVersionUpgradeMounts.getVolumeMounts(majorVersoinUpgradeContainerContext)
            )
            .addToVolumeMounts(new VolumeMountBuilder()
                .withName(StackGresVolume.DSHM.getName())
                .withMountPath(ClusterPath.SHARED_MEMORY_PATH.path())
                .build())
            .addToVolumeMounts(new VolumeMountBuilder()
                .withName(StackGresVolume.LOG.getName())
                .withMountPath(ClusterPath.PG_LOG_PATH.path())
                .build())
            .addToVolumeMounts(new VolumeMountBuilder()
                .withName(StackGresVolume.POSTGRES_CONFIG.getName())
                .withMountPath(ClusterPath.ETC_POSTGRES_PATH.path())
                .build())
            .addToVolumeMounts(
                new VolumeMountBuilder()
                .withName(StackGresVolume.POSTGRES_SSL.getName())
                .withMountPath(ClusterPath.SSL_PATH.path())
                .build())
            .build();
  }

}
