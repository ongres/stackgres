/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.dbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsMajorVersionUpgrade;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsSpec;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.DbOpsReview;
import io.stackgres.operator.mutation.AbstractExtensionsMutator;
import io.stackgres.operator.validation.ValidationUtil;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DbOpsMajorVersionUpgradeExtensionsMutator
    extends AbstractExtensionsMutator<StackGresDbOps, DbOpsReview>
    implements DbOpsMutator {

  private final ExtensionMetadataManager extensionMetadataManager;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      supportedPostgresVersions;

  @Inject
  public DbOpsMajorVersionUpgradeExtensionsMutator(
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this(extensionMetadataManager, clusterFinder, ValidationUtil.SUPPORTED_POSTGRES_VERSIONS);
  }

  public DbOpsMajorVersionUpgradeExtensionsMutator(
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions) {
    this.extensionMetadataManager = extensionMetadataManager;
    this.clusterFinder = clusterFinder;
    this.supportedPostgresVersions = supportedPostgresVersions;
  }

  @Override
  public StackGresDbOps mutate(DbOpsReview review, StackGresDbOps resource) {
    if (review.getRequest().getOperation() != Operation.CREATE) {
      return resource;
    }
    if (Optional.of(review.getRequest().getObject())
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getMajorVersionUpgrade)
        .isEmpty()) {
      return resource;
    }
    return super.mutate(review, resource);
  }

  @Override
  protected boolean extensionsChanged(
      DbOpsReview review,
      StackGresCluster cluster,
      StackGresCluster oldCluster) {
    String postgresVersion = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .flatMap(getPostgresFlavorComponent(cluster).get(cluster)::findVersion)
        .orElse(null);
    if (postgresVersion == null
        || !supportedPostgresVersions
        .get(getPostgresFlavorComponent(cluster))
        .get(StackGresVersion.getStackGresVersion(cluster))
        .contains(postgresVersion)) {
      return false;
    }
    return super.extensionsChanged(review, cluster, oldCluster);
  }

  @Override
  protected ExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresDbOps resource) {
    return Optional.of(resource)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getMajorVersionUpgrade)
        .map(StackGresDbOpsMajorVersionUpgrade::getToInstallPostgresExtensions);
  }

  @Override
  protected StackGresCluster getCluster(DbOpsReview review) {
    var cluster = clusterFinder.findByNameAndNamespace(
        review.getRequest().getObject().getSpec().getSgCluster(),
        review.getRequest().getObject().getMetadata().getNamespace())
        .orElse(null);
    if (cluster == null) {
      throw new RuntimeException(
          "SGCluster " + review.getRequest().getObject().getSpec().getSgCluster()
          + "." + review.getRequest().getObject().getMetadata().getNamespace()
          + " not found");
    }
    cluster.getSpec().getPostgres().setVersion(
        review.getRequest().getObject().getSpec().getMajorVersionUpgrade().getPostgresVersion());
    return cluster;
  }

  @Override
  protected StackGresCluster getOldCluster(DbOpsReview review) {
    return null;
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(
      StackGresDbOps resource, StackGresCluster cluster) {
    List<StackGresClusterExtension> dbOpsExtensions =
        Optional.of(resource.getSpec().getMajorVersionUpgrade())
        .map(StackGresDbOpsMajorVersionUpgrade::getPostgresExtensions)
        .orElse(List.of());
    return Seq.seq(Optional.of(cluster)
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
      StackGresDbOps resource, StackGresCluster cluster) {
    return StackGresUtil.getDefaultClusterExtensions(cluster);
  }

  @Override
  protected void setToInstallExtensions(StackGresDbOps resource,
      List<StackGresClusterInstalledExtension> toInstallExtensions) {
    resource.getSpec().getMajorVersionUpgrade().setToInstallPostgresExtensions(toInstallExtensions);
  }

}
