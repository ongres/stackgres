/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.dbops;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.stackgres.common.ErrorType;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresDbOpsReview;
import io.stackgres.operator.mutation.ClusterExtensionMetadataManager;
import io.stackgres.operator.validation.AbstractExtensionsValidator;
import io.stackgres.operator.validation.ExtensionReview;
import io.stackgres.operator.validation.ImmutableExtensionReview;
import io.stackgres.operator.validation.ValidationType;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;

@Singleton
@ValidationType(ErrorType.EXTENSION_NOT_FOUND)
public class DbOpsMajorVersionUpgradeExtensionValidator
    extends AbstractExtensionsValidator<StackGresDbOpsReview>
    implements DbOpsValidator {

  private final ClusterExtensionMetadataManager extensionMetadataManager;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  @Inject
  public DbOpsMajorVersionUpgradeExtensionValidator(
      ClusterExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.extensionMetadataManager = extensionMetadataManager;
    this.clusterFinder = clusterFinder;
  }

  @Override
  protected ClusterExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  public void validate(StackGresDbOpsReview review) throws ValidationFailed {
    if (review.getRequest().getOperation() == Operation.CREATE
        && review.getRequest().getObject().getSpec().isOpMajorVersionUpgrade()) {
      validateExtensions(review);
    }
  }

  @Override
  protected ExtensionReview getExtensionReview(StackGresDbOpsReview review) {
    StackGresCluster cluster = getCluster(review);
    String pgVersion = review.getRequest().getObject().getSpec().getMajorVersionUpgrade()
        .getPostgresVersion();
    StackGresComponent flavor = StackGresUtil.getPostgresFlavorComponent(cluster);

    var stackGresVersion = StackGresVersion.getStackGresVersion(cluster);
    return ImmutableExtensionReview.builder()
        .defaultExtensions(
            StackGresUtil.getDefaultClusterExtensions(pgVersion, flavor, stackGresVersion)
        ).arch(getArch(cluster))
        .os(getOs(cluster))
        .postgresVersion(pgVersion)
        .postgresFlavor(flavor)
        .requiredExtensions(getRequiredExtensions(cluster))
        .toInstallExtensions(getToInstallExtensions(cluster))
        .stackGresVersion(StackGresVersion.getStackGresVersion(cluster))
        .build();
  }

  private StackGresCluster getCluster(StackGresDbOpsReview review) {
    String clusterName = review.getRequest().getObject().getSpec().getSgCluster();
    String namespace = review.getRequest().getObject().getMetadata().getNamespace();
    return clusterFinder.findByNameAndNamespace(clusterName, namespace)
        .orElseThrow();
  }

  protected Optional<String> getArch(StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getArch);
  }

  protected Optional<String> getOs(StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getOs);
  }

  protected List<StackGresClusterExtension> getRequiredExtensions(StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions)
        .orElse(List.of());
  }

  protected List<StackGresClusterInstalledExtension> getToInstallExtensions(
      StackGresCluster cluster) {
    return Optional.ofNullable(cluster.getSpec())
        .map(StackGresClusterSpec::getToInstallPostgresExtensions)
        .orElse(List.of());
  }

  @Override
  protected void failValidation(String reason, String message) throws ValidationFailed {
    fail(reason, message);
  }
}
