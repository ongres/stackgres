/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.validation.shardeddbops;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.ErrorType;
import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsMajorVersionUpgrade;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsSpec;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresShardedDbOpsReview;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForUtil;
import io.stackgres.operator.validation.AbstractExtensionsValidator;
import io.stackgres.operatorframework.admissionwebhook.validating.ValidationFailed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jooq.lambda.Seq;

@Singleton
public class ShardedDbOpsMajorVersionUpgradeExtensionsValidator
    extends AbstractExtensionsValidator<StackGresShardedDbOps, StackGresShardedDbOpsReview>
    implements ShardedDbOpsValidator {

  private final StackGresContext context;
  private final ExtensionMetadataManager extensionMetadataManager;
  private final CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  @Inject
  public ShardedDbOpsMajorVersionUpgradeExtensionsValidator(
      StackGresContext context,
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresShardedCluster> clusterFinder) {
    this.context = context;
    this.extensionMetadataManager = extensionMetadataManager;
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void validate(StackGresShardedDbOpsReview review) throws ValidationFailed {
    switch (review.getRequest().getOperation()) {
      case CREATE, UPDATE:
        if (Optional.of(review.getRequest().getObject())
            .map(StackGresShardedDbOps::getSpec)
            .map(StackGresShardedDbOpsSpec::getMajorVersionUpgrade)
            .isPresent()) {
          super.validate(review);
        }
        break;
      default:
        break;
    }
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
  protected List<ExtensionTuple> getDefaultExtensions(
      StackGresShardedDbOps resource, StackGresCluster cluster) {
    String pgVersion = cluster.getSpec().getPostgres().getVersion();
    StackGresComponent flavor = StackGresUtil.getPostgresFlavorComponent(cluster);
    StackGresVersion stackGresVersion = StackGresVersion.getStackGresVersion(cluster);
    return StackGresUtil.getDefaultClusterExtensions(context, pgVersion, flavor, stackGresVersion,
        StackGresUtil.isRegistryEnabled(cluster));
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(
      StackGresShardedDbOps resource, StackGresCluster cluster) {
    List<StackGresClusterExtension> dbOpsExtensions =
        Optional.of(resource.getSpec().getMajorVersionUpgrade())
        .map(StackGresShardedDbOpsMajorVersionUpgrade::getPostgresExtensions)
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
      StackGresShardedDbOps resource) {
    return Optional.of(resource)
        .map(StackGresShardedDbOps::getSpec)
        .map(StackGresShardedDbOpsSpec::getMajorVersionUpgrade)
        .map(StackGresShardedDbOpsMajorVersionUpgrade::getToInstallPostgresExtensions);
  }

  @Override
  protected StackGresCluster getCluster(StackGresShardedDbOpsReview review) throws ValidationFailed {
    var cluster = getOldCluster(review);
    cluster.getSpec().getPostgres().setVersion(
        review.getRequest().getObject().getSpec().getMajorVersionUpgrade().getPostgresVersion());
    return cluster;
  }

  @Override
  protected StackGresCluster getOldCluster(StackGresShardedDbOpsReview review) throws ValidationFailed {
    var cluster = clusterFinder.findByNameAndNamespace(
        review.getRequest().getObject().getSpec().getSgShardedCluster(),
        review.getRequest().getObject().getMetadata().getNamespace())
        .orElse(null);
    if (cluster == null) {
      String errorTypeUri = ErrorType.getErrorTypeUri(ErrorType.EXTENSION_NOT_FOUND);
      fail(errorTypeUri, "SGShardedCluster " + review.getRequest().getObject().getSpec().getSgShardedCluster()
          + "." + review.getRequest().getObject().getMetadata().getNamespace()
          + " not found");
      throw new RuntimeException(
          "SGShardedCluster " + review.getRequest().getObject().getSpec().getSgShardedCluster()
          + "." + review.getRequest().getObject().getMetadata().getNamespace()
          + " not found");
    }
    return StackGresShardedClusterForUtil.getCoordinatorCluster(cluster, Optional.empty());
  }

  @Override
  protected void failValidation(String reason, String message) throws ValidationFailed {
    fail(reason, message);
  }

}
