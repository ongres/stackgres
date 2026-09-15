/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops.context;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMajorVersionUpgrade;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.operator.conciliation.AbstractExtensionsContextAppender;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

/**
 * Fills {@code .spec.majorVersionUpgrade.toInstallPostgresExtensions} of the SGDbOps with the
 * extensions that will be installed on the target SGCluster, failing the reconciliation when any
 * of them is not available for the target Postgres version.
 */
@ApplicationScoped
public class DbOpsMajorVersionUpgradeExtensionsContextAppender
    extends AbstractExtensionsContextAppender<
        DbOpsMajorVersionUpgradeExtensionsContextAppender.DbOpsTargetCluster, Builder> {

  /**
   * The SGDbOps being reconciled and a copy of the SGCluster with the Postgres version of the
   * major version upgrade, that is the version the extensions have to be resolved for.
   */
  record DbOpsTargetCluster(StackGresDbOps dbOps, StackGresCluster targetCluster) {
  }

  private final StackGresContext context;
  private final ExtensionMetadataManager extensionMetadataManager;

  @Inject
  public DbOpsMajorVersionUpgradeExtensionsContextAppender(
      StackGresContext context,
      ExtensionMetadataManager extensionMetadataManager) {
    this.context = context;
    this.extensionMetadataManager = extensionMetadataManager;
  }

  public void appendContext(
      StackGresDbOps dbOps,
      StackGresCluster cluster,
      Builder contextBuilder) {
    final String targetPostgresVersion =
        dbOps.getSpec().getMajorVersionUpgrade().getPostgresVersion();
    final StackGresCluster targetCluster = new StackGresClusterBuilder(cluster)
        .editSpec()
        .editPostgres()
        .withVersion(targetPostgresVersion)
        .endPostgres()
        .endSpec()
        .build();
    final String version = getPostgresFlavorComponent(targetCluster)
        .get(targetCluster)
        .getVersion(context, targetPostgresVersion);
    final String buildVersion = getPostgresFlavorComponent(targetCluster)
        .get(targetCluster)
        .getBuildVersion(context, targetPostgresVersion);
    appendContext(
        new DbOpsTargetCluster(dbOps, targetCluster),
        contextBuilder,
        version,
        buildVersion,
        Optional.empty(),
        Optional.empty(),
        targetCluster);
  }

  @Override
  protected StackGresContext getContext() {
    return context;
  }

  @Override
  protected ExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(
      DbOpsTargetCluster inputContext, String version, String buildVersion) {
    final List<StackGresClusterExtension> dbOpsExtensions =
        Optional.of(inputContext.dbOps().getSpec().getMajorVersionUpgrade())
        .map(StackGresDbOpsMajorVersionUpgrade::getPostgresExtensions)
        .orElse(List.of());
    return Seq.seq(Optional.of(inputContext.targetCluster())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions)
        .orElse(List.of()))
        .filter(extension -> dbOpsExtensions.stream()
            .map(StackGresClusterExtension::getName)
            .noneMatch(extension.getName()::equals))
        .append(dbOpsExtensions)
        .toList();
  }

  @Override
  protected List<ExtensionTuple> getDefaultExtensions(
      DbOpsTargetCluster inputContext, String version, String buildVersion) {
    return StackGresUtil.getDefaultClusterExtensions(context, inputContext.targetCluster());
  }

  @Override
  protected void setToInstallExtensions(
      DbOpsTargetCluster inputContext,
      List<StackGresClusterInstalledExtension> toInstallExtensions) {
    inputContext.dbOps().getSpec().getMajorVersionUpgrade()
        .setToInstallPostgresExtensions(toInstallExtensions);
  }

}
