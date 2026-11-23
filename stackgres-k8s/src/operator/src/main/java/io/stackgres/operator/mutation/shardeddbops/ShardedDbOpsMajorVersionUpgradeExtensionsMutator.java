/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardeddbops;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsMajorVersionUpgrade;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpec;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.validation.ValidationUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedDbOpsMajorVersionUpgradeExtensionsMutator
    extends AbstractShardedDbOpsVersionUpgradeExtensionsMutator {

  @Inject
  public ShardedDbOpsMajorVersionUpgradeExtensionsMutator(
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresShardedCluster> clusterFinder) {
    this(extensionMetadataManager, clusterFinder, ValidationUtil.SUPPORTED_POSTGRES_VERSIONS);
  }

  public ShardedDbOpsMajorVersionUpgradeExtensionsMutator(
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresShardedCluster> clusterFinder,
      Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions) {
    super(extensionMetadataManager, clusterFinder, supportedPostgresVersions);
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

  @Override
  protected void setToInstallExtensions(StackGresShardedDbOps resource,
      List<StackGresClusterInstalledExtension> toInstallExtensions) {
    resource.getSpec().getMajorVersionUpgrade().setToInstallPostgresExtensions(toInstallExtensions);
  }

}
