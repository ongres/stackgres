/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.cluster;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.validation.AbstractExtensionsValidator;
import io.stackgres.operator.validation.ExtensionReview;
import io.stackgres.operator.validation.ImmutableExtensionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ExtensionsValidator extends AbstractExtensionsValidator<StackGresClusterReview>
    implements ClusterValidator {

  private final ExtensionMetadataManager extensionMetadataManager;

  @Inject
  public ExtensionsValidator(ExtensionMetadataManager extensionMetadataManager) {
    super();
    this.extensionMetadataManager = extensionMetadataManager;
  }

  @Override
  protected ExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected ExtensionReview getExtensionReview(StackGresClusterReview review) {
    return ImmutableExtensionReview.builder()
        .postgresVersion(getPostgresVersion(review))
        .postgresFlavor(getPostgresFlavor(review))
        .arch(getArch(review))
        .os(getOs(review))
        .stackGresVersion(StackGresVersion.getStackGresVersion(review.getRequest().getObject()))
        .defaultExtensions(getDefaultExtensions(review))
        .requiredExtensions(getRequiredExtensions(review))
        .toInstallExtensions(getToInstallExtensions(review))
        .build();
  }

  protected List<ExtensionTuple> getDefaultExtensions(
      StackGresClusterReview review) {
    StackGresCluster cluster = getCluster(review);
    String pgVersion = cluster.getSpec().getPostgres().getVersion();
    StackGresComponent flavor = StackGresUtil.getPostgresFlavorComponent(cluster);
    StackGresVersion stackGresVersion = StackGresVersion.getStackGresVersion(cluster);
    return StackGresUtil.getDefaultClusterExtensions(pgVersion, flavor, stackGresVersion);
  }

  protected StackGresCluster getCluster(StackGresClusterReview review) {
    return review.getRequest().getObject();
  }

  protected Optional<String> getArch(StackGresClusterReview review) {
    return Optional.of(review.getRequest().getObject())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getArch);
  }

  protected Optional<String> getOs(StackGresClusterReview review) {
    return Optional.of(review.getRequest().getObject())
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getOs);
  }

  protected String getPostgresVersion(StackGresClusterReview review) {
    return getCluster(review).getSpec().getPostgres().getVersion();
  }

  protected StackGresComponent getPostgresFlavor(StackGresClusterReview review) {
    return StackGresUtil.getPostgresFlavorComponent(
        getCluster(review)
    );
  }

  protected List<StackGresClusterExtension> getRequiredExtensions(
      StackGresClusterReview review) {
    return Optional.ofNullable(review.getRequest().getObject().getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions)
        .orElse(List.of());
  }

  protected List<StackGresClusterInstalledExtension> getToInstallExtensions(
      StackGresClusterReview review) {
    return Optional.ofNullable(review.getRequest().getObject().getSpec())
        .map(StackGresClusterSpec::getToInstallPostgresExtensions)
        .orElse(List.of());
  }

  @Override
  protected void failValidation(String reason, String message) throws ValidationFailed {
    fail(reason, message);
  }

}
