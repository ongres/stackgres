/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsMinorVersionUpgrade;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpec;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
public class ShardedDbOpsMinorVersionUpgradeExtensionsValidator
    extends AbstractShardedDbOpsVersionUpgradeExtensionsValidator {

  @Inject
  public ShardedDbOpsMinorVersionUpgradeExtensionsValidator(
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresShardedCluster> clusterFinder) {
    super(extensionMetadataManager, clusterFinder);
  }

  @Override
  protected boolean isVersionUpgrade(StackGresShardedDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresShardedDbOps::getSpec)
        .map(StackGresShardedDbOpsSpec::getMinorVersionUpgrade)
        .isPresent();
  }

  @Override
  protected String getTargetPostgresVersion(StackGresShardedDbOps dbOps) {
    return dbOps.getSpec().getMinorVersionUpgrade().getPostgresVersion();
  }

  @Override
  protected List<StackGresClusterExtension> getPostgresExtensions(StackGresShardedDbOps dbOps) {
    return dbOps.getSpec().getMinorVersionUpgrade().getPostgresExtensions();
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresShardedDbOps resource) {
    return Optional.of(resource)
        .map(StackGresShardedDbOps::getSpec)
        .map(StackGresShardedDbOpsSpec::getMinorVersionUpgrade)
        .map(StackGresShardedDbOpsMinorVersionUpgrade::getToInstallPostgresExtensions);
  }

  /**
   * The SGClusters of an SGShardedCluster set the extension versions resolved for the current
   * postgres version, so check the extensions of the SGShardedCluster instead, including those it
   * installs by default, like the SGShardedCluster does.
   */
  @Override
  protected StackGresCluster getCoordinatorCluster(StackGresShardedCluster cluster) {
    final StackGresCluster coordinatorCluster = super.getCoordinatorCluster(cluster);
    final List<StackGresClusterExtension> extensions =
        Optional.ofNullable(cluster.getSpec().getPostgres().getExtensions())
        .orElse(List.of());
    if (StackGresUtil.getPostgresFlavorComponent(cluster).get(cluster)
        .findVersion(cluster.getSpec().getPostgres().getVersion()).isEmpty()) {
      coordinatorCluster.getSpec().getPostgres().setExtensions(extensions);
      return coordinatorCluster;
    }
    coordinatorCluster.getSpec().getPostgres().setExtensions(
        Seq.seq(extensions)
        .append(StackGresUtil.getShardedClusterExtensionsKeepingInstalledVersions(cluster)
            .stream()
            .filter(extension -> extensions.stream()
                .map(StackGresClusterExtension::getName)
                .noneMatch(extension.extensionName()::equals))
            .map(extension -> new StackGresClusterExtensionBuilder()
                .withName(extension.extensionName())
                .withVersion(extension.extensionVersion().orElse(null))
                .build()))
        .toList());
    return coordinatorCluster;
  }

}
