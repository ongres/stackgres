/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardedcluster;

import static io.stackgres.common.StackGresShardedClusterForCitusUtil.getCoordinatorCluster;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.validation.AbstractExtensionsValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class ExtensionsValidator
    extends AbstractExtensionsValidator<StackGresShardedCluster, StackGresShardedClusterReview>
    implements ShardedClusterValidator {

  private final ExtensionMetadataManager extensionMetadataManager;

  private final CustomResourceScanner<StackGresCluster> clusterScanner;

  private final LabelFactoryForShardedCluster labelFactory;

  @Inject
  public ExtensionsValidator(ExtensionMetadataManager extensionMetadataManager,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      LabelFactoryForShardedCluster labelFactory) {
    this.extensionMetadataManager = extensionMetadataManager;
    this.clusterScanner = clusterScanner;
    this.labelFactory = labelFactory;
  }

  @Override
  protected ExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected List<ExtensionTuple> getDefaultExtensions(
      StackGresShardedCluster resource) {
    String pgVersion = resource.getSpec().getPostgres().getVersion();
    StackGresVersion operatorVersion = StackGresVersion.getStackGresVersion(resource);
    return StackGresUtil.getDefaultShardedClusterExtensions(pgVersion, operatorVersion);
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(
      StackGresShardedCluster resource) {
    return Optional.ofNullable(resource.getSpec())
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions)
        .orElse(List.of());
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresShardedCluster resource) {
    return Optional.ofNullable(resource.getStatus())
        .map(StackGresShardedClusterStatus::getToInstallPostgresExtensions);
  }

  @Override
  protected StackGresCluster getCluster(StackGresShardedCluster resource) {
    return new StackGresClusterBuilder(getCoordinatorCluster(resource))
        .withStatus(clusterScanner.getResourcesWithLabels(
            resource.getMetadata().getNamespace(),
            labelFactory.coordinatorLabels(resource))
            .stream()
            .map(StackGresCluster::getStatus)
            .findAny()
            .orElse(null))
        .build();
  }

  @Override
  protected void failValidation(String reason, String message) throws ValidationFailed {
    fail(reason, message);
  }

}
