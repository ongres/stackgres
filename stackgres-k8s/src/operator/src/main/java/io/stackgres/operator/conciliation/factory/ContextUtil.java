/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDbOpsStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.cluster.StackGresClusterContainerContext;
import io.stackgres.operator.conciliation.factory.distributedlogs.DistributedLogsContainerContext;

public class ContextUtil {

  public static PostgresContainerContext toPostgresContext(
      StackGresClusterContainerContext context) {
    final StackGresClusterContext clusterContext = context.getClusterContext();
    final StackGresCluster cluster = clusterContext.getSource();

    ImmutablePostgresContainerContext.Builder contextBuilder = Optional
        .of(clusterContext.getSource())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getDbOps)
        .map(StackGresClusterDbOpsStatus::getMajorVersionUpgrade)
        .map(majorVersionUpgradeStatus -> {
          String targetVersion = majorVersionUpgradeStatus.getTargetPostgresVersion();
          String sourceVersion = majorVersionUpgradeStatus.getSourcePostgresVersion();
          String sourceMajorVersion = getPostgresFlavorComponent(cluster)
              .findMajorVersion(sourceVersion);
          return ImmutablePostgresContainerContext.builder()
              .from(context)
              .postgresMajorVersion(getPostgresFlavorComponent(cluster)
                  .findMajorVersion(targetVersion))
              .oldMajorVersion(sourceMajorVersion)
              .imageBuildMajorVersion(getPostgresFlavorComponent(cluster)
                  .findBuildMajorVersion(targetVersion))
              .oldImageBuildMajorVersion(getPostgresFlavorComponent(cluster)
                  .findBuildMajorVersion(sourceVersion))
              .postgresVersion(targetVersion)
              .oldPostgresVersion(sourceVersion);
        })
        .orElseGet(() -> {
          final String postgresVersion = cluster.getSpec().getPostgres().getVersion();
          final String majorVersion = getPostgresFlavorComponent(cluster)
              .findMajorVersion(postgresVersion);
          final String buildMajorVersion = getPostgresFlavorComponent(cluster)
              .findBuildMajorVersion(postgresVersion);
          return ImmutablePostgresContainerContext.builder()
              .from(context)
              .postgresVersion(postgresVersion)
              .imageBuildMajorVersion(buildMajorVersion)
              .postgresMajorVersion(majorVersion);
        });

    final List<StackGresClusterInstalledExtension> installedExtensions = Optional
        .ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getToInstallPostgresExtensions)
        .stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableList());
    contextBuilder.addAllInstalledExtensions(installedExtensions);

    return contextBuilder.build();

  }

  public static PostgresContainerContext toPostgresContext(
      DistributedLogsContainerContext context) {

    StackGresDistributedLogs distributedLogs = context.getDistributedLogsContext().getSource();

    List<StackGresClusterInstalledExtension> installedExtensions = Optional
        .ofNullable(distributedLogs.getSpec())
        .map(StackGresDistributedLogsSpec::getToInstallPostgresExtensions)
        .stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableList());

    return ImmutablePostgresContainerContext.builder()
        .from(context)
        .postgresMajorVersion(StackGresDistributedLogsUtil.getPostgresMajorVersion())
        .postgresVersion(StackGresDistributedLogsUtil.getPostgresVersion())
        .imageBuildMajorVersion(StackGresDistributedLogsUtil.getPostgresBuildMajorVersion())
        .addAllInstalledExtensions(installedExtensions)
        .build();
  }
}
