/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

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
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.AbstractExtensionsValidator;
import io.stackgres.operator.validation.ExtensionReview;
import io.stackgres.operator.validation.ImmutableExtensionReview;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ExtensionsValidator extends AbstractExtensionsValidator<StackGresShardedClusterReview>
    implements ShardedClusterValidator {

  private final ExtensionMetadataManager extensionMetadataManager;

  private final CustomResourceScanner<StackGresCluster> clusterScanner;

  private final LabelFactoryForShardedCluster labelFactory;

  @Inject
  public ExtensionsValidator(ExtensionMetadataManager extensionMetadataManager,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      LabelFactoryForShardedCluster labelFactory) {
    super();
    this.extensionMetadataManager = extensionMetadataManager;
    this.clusterScanner = clusterScanner;
    this.labelFactory = labelFactory;
  }

  @Override
  protected ExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected ExtensionReview getExtensionReview(StackGresShardedClusterReview review) {
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
      StackGresShardedClusterReview review) {
    StackGresShardedCluster cluster = getCluster(review);
    String pgVersion = cluster.getSpec().getPostgres().getVersion();
    StackGresVersion stackGresVersion = StackGresVersion.getStackGresVersion(cluster);
    return StackGresUtil.getDefaultShardedClusterExtensions(pgVersion, stackGresVersion);
  }

  protected StackGresShardedCluster getCluster(StackGresShardedClusterReview review) {
    return review.getRequest().getObject();
  }

  protected Optional<String> getArch(StackGresShardedClusterReview review) {
    return clusterScanner.getResourcesWithLabels(
        review.getRequest().getObject().getMetadata().getNamespace(),
        labelFactory.coordinatorLabels(review.getRequest().getObject()))
        .stream()
        .map(Optional::of)
        .map(cluster -> cluster
            .map(StackGresCluster::getStatus)
            .map(StackGresClusterStatus::getArch))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  protected Optional<String> getOs(StackGresShardedClusterReview review) {
    return clusterScanner.getResourcesWithLabels(
        review.getRequest().getObject().getMetadata().getNamespace(),
        labelFactory.coordinatorLabels(review.getRequest().getObject()))
        .stream()
        .map(Optional::of)
        .map(cluster -> cluster
            .map(StackGresCluster::getStatus)
            .map(StackGresClusterStatus::getOs))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }

  protected String getPostgresVersion(StackGresShardedClusterReview review) {
    return getCluster(review).getSpec().getPostgres().getVersion();
  }

  protected StackGresComponent getPostgresFlavor(StackGresShardedClusterReview review) {
    return StackGresUtil.getPostgresFlavorComponent(
        getCluster(review)
    );
  }

  protected List<StackGresClusterExtension> getRequiredExtensions(
      StackGresShardedClusterReview review) {
    return Optional.ofNullable(review.getRequest().getObject().getSpec())
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions)
        .orElse(List.of());
  }

  protected List<StackGresClusterInstalledExtension> getToInstallExtensions(
      StackGresShardedClusterReview review) {
    return Optional.ofNullable(review.getRequest().getObject().getStatus())
        .map(StackGresShardedClusterStatus::getToInstallPostgresExtensions)
        .orElse(List.of());
  }

  @Override
  protected void failValidation(String reason, String message) throws ValidationFailed {
    fail(reason, message);
  }

}
