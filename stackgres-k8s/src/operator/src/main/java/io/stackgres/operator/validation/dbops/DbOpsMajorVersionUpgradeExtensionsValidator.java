/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMajorVersionUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class DbOpsMajorVersionUpgradeExtensionsValidator
    extends AbstractDbOpsVersionUpgradeExtensionsValidator {

  @Inject
  public DbOpsMajorVersionUpgradeExtensionsValidator(
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    super(extensionMetadataManager, clusterFinder);
  }

  @Override
  protected boolean isVersionUpgrade(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getMajorVersionUpgrade)
        .isPresent();
  }

  @Override
  protected String getTargetPostgresVersion(StackGresDbOps dbOps) {
    return dbOps.getSpec().getMajorVersionUpgrade().getPostgresVersion();
  }

  @Override
  protected List<StackGresClusterExtension> getPostgresExtensions(StackGresDbOps dbOps) {
    return dbOps.getSpec().getMajorVersionUpgrade().getPostgresExtensions();
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresDbOps resource) {
    return Optional.of(resource)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getMajorVersionUpgrade)
        .map(StackGresDbOpsMajorVersionUpgrade::getToInstallPostgresExtensions);
  }

}
