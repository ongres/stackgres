/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.dbops;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMinorVersionUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.validation.ValidationUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DbOpsMinorVersionUpgradeExtensionsMutator
    extends AbstractDbOpsVersionUpgradeExtensionsMutator {

  @Inject
  public DbOpsMinorVersionUpgradeExtensionsMutator(
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this(extensionMetadataManager, clusterFinder, ValidationUtil.SUPPORTED_POSTGRES_VERSIONS);
  }

  public DbOpsMinorVersionUpgradeExtensionsMutator(
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions) {
    super(extensionMetadataManager, clusterFinder, supportedPostgresVersions);
  }

  @Override
  protected boolean isVersionUpgrade(StackGresDbOps dbOps) {
    return Optional.of(dbOps)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getMinorVersionUpgrade)
        .isPresent();
  }

  @Override
  protected String getTargetPostgresVersion(StackGresDbOps dbOps) {
    return dbOps.getSpec().getMinorVersionUpgrade().getPostgresVersion();
  }

  @Override
  protected List<StackGresClusterExtension> getPostgresExtensions(StackGresDbOps dbOps) {
    return dbOps.getSpec().getMinorVersionUpgrade().getPostgresExtensions();
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresDbOps resource) {
    return Optional.of(resource)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getMinorVersionUpgrade)
        .map(StackGresDbOpsMinorVersionUpgrade::getToInstallPostgresExtensions);
  }

  @Override
  protected void setToInstallExtensions(StackGresDbOps resource,
      List<StackGresClusterInstalledExtension> toInstallExtensions) {
    resource.getSpec().getMinorVersionUpgrade().setToInstallPostgresExtensions(toInstallExtensions);
  }

}
