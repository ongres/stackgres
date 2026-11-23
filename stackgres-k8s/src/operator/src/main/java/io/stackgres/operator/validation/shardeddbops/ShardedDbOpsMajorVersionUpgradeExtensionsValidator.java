/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsMajorVersionUpgrade;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpec;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ShardedDbOpsMajorVersionUpgradeExtensionsValidator
    extends AbstractShardedDbOpsVersionUpgradeExtensionsValidator {

  @Inject
  public ShardedDbOpsMajorVersionUpgradeExtensionsValidator(
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresShardedCluster> clusterFinder) {
    super(extensionMetadataManager, clusterFinder);
  }

  @Override
  protected boolean isVersionUpgrade(StackGresShardedDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresShardedDbOps::getSpec)
        .map(StackGresShardedDbOpsSpec::getMajorVersionUpgrade)
        .isPresent();
  }

  @Override
  protected String getTargetPostgresVersion(StackGresShardedDbOps dbOps) {
    return dbOps.getSpec().getMajorVersionUpgrade().getPostgresVersion();
  }

  @Override
  protected List<StackGresClusterExtension> getPostgresExtensions(StackGresShardedDbOps dbOps) {
    return dbOps.getSpec().getMajorVersionUpgrade().getPostgresExtensions();
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresShardedDbOps resource) {
    return Optional.of(resource)
        .map(StackGresShardedDbOps::getSpec)
        .map(StackGresShardedDbOpsSpec::getMajorVersionUpgrade)
        .map(StackGresShardedDbOpsMajorVersionUpgrade::getToInstallPostgresExtensions);
  }

}
