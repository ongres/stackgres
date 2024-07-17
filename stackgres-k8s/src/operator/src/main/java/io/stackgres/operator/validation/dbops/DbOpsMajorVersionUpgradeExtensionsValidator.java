/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.ErrorType;
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
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.validation.AbstractExtensionsValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
public class DbOpsMajorVersionUpgradeExtensionsValidator
    extends AbstractExtensionsValidator<StackGresDbOps, StackGresDbOpsReview>
    implements DbOpsValidator {

  private final ExtensionMetadataManager extensionMetadataManager;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  public DbOpsMajorVersionUpgradeExtensionsValidator(
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.extensionMetadataManager = extensionMetadataManager;
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void validate(StackGresDbOpsReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE, UPDATE:
        if (Optional.of(review.getRequest().getObject())
            .map(StackGresDbOps::getSpec)
            .map(StackGresDbOpsSpec::getMajorVersionUpgrade)
            .isPresent()) {
          super.validate(review);
        }
        break;
      default:
        break;
    }
  }

  @Override
  protected ExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected List<ExtensionTuple> getDefaultExtensions(
      StackGresDbOps resource, StackGresCluster cluster) {
    String pgVersion = cluster.getSpec().getPostgres().getVersion();
    StackGresComponent flavor = StackGresUtil.getPostgresFlavorComponent(cluster);
    StackGresVersion stackGresVersion = StackGresVersion.getStackGresVersion(cluster);
    return StackGresUtil.getDefaultClusterExtensions(pgVersion, flavor, stackGresVersion);
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

  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresDbOps resource) {
    return Optional.of(resource)
        .map(StackGresDbOps::getSpec)
        .map(StackGresDbOpsSpec::getMajorVersionUpgrade)
        .map(StackGresDbOpsMajorVersionUpgrade::getToInstallPostgresExtensions);
  }

  @Override
  protected StackGresCluster getCluster(StackGresDbOpsReview review) throws ValidationFailed {
    var cluster = getOldCluster(review);
    cluster.getSpec().getPostgres().setVersion(
        review.getRequest().getObject().getSpec().getMajorVersionUpgrade().getPostgresVersion());
    return cluster;
  }

  @Override
  protected StackGresCluster getOldCluster(StackGresDbOpsReview review) throws ValidationFailed {
    var cluster = clusterFinder.findByNameAndNamespace(
        review.getRequest().getObject().getSpec().getSgCluster(),
        review.getRequest().getObject().getMetadata().getNamespace())
        .orElse(null);
    if (cluster == null) {
      String errorTypeUri = ErrorType.getErrorTypeUri(ErrorType.EXTENSION_NOT_FOUND);
      fail(errorTypeUri, "SGCluster " + review.getRequest().getObject().getSpec().getSgCluster()
          + "." + review.getRequest().getObject().getMetadata().getNamespace()
          + " not found");
      throw new RuntimeException(
          "SGCluster " + review.getRequest().getObject().getSpec().getSgCluster()
          + "." + review.getRequest().getObject().getMetadata().getNamespace()
          + " not found");
    }
    return cluster;
  }

  @Override
  protected void failValidation(String reason, String message) throws ValidationFailed {
    fail(reason, message);
  }

}
