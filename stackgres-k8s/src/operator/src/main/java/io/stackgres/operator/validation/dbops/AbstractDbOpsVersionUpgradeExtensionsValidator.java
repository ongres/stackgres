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
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.validation.AbstractExtensionsValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import org.jooq.lambda.Seq;

/**
 * Check that every extension has a build for the target postgres version of an SGDbOps that
 * changes the postgres version of an SGCluster. The extensions are those of the SGCluster, where
 * the extensions set in the SGDbOps replace those with the same name.
 */
public abstract class AbstractDbOpsVersionUpgradeExtensionsValidator
    extends AbstractExtensionsValidator<StackGresDbOps, StackGresDbOpsReview>
    implements DbOpsValidator {

  private final ExtensionMetadataManager extensionMetadataManager;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  protected AbstractDbOpsVersionUpgradeExtensionsValidator(
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.extensionMetadataManager = extensionMetadataManager;
    this.clusterFinder = clusterFinder;
  }

  /**
   * Return true when the SGDbOps is of the version upgrade handled by this validator.
   */
  protected abstract boolean isVersionUpgrade(StackGresDbOps dbOps);

  protected abstract String getTargetPostgresVersion(StackGresDbOps dbOps);

  protected abstract List<StackGresClusterExtension> getPostgresExtensions(StackGresDbOps dbOps);

  @Override
  public void validate(StackGresDbOpsReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE, UPDATE:
        if (isVersionUpgrade(review.getRequest().getObject())) {
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
        Optional.ofNullable(getPostgresExtensions(resource))
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
  protected StackGresCluster getCluster(StackGresDbOpsReview review) throws ValidationFailed {
    var cluster = getOldCluster(review);
    cluster.getSpec().getPostgres().setVersion(
        getTargetPostgresVersion(review.getRequest().getObject()));
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
