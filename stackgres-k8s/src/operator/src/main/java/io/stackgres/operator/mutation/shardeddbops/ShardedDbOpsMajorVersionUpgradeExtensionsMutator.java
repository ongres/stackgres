/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardeddbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresUtil;
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
import io.stackgres.operator.common.StackGresVersionUtil;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForUtil;
import io.stackgres.operator.mutation.AbstractExtensionsMutator;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ShardedDbOpsMajorVersionUpgradeExtensionsMutator
    extends AbstractExtensionsMutator<StackGresShardedDbOps, StackGresShardedDbOpsReview>
    implements ShardedDbOpsMutator {

  private final StackGresContext context;
  private final ExtensionMetadataManager extensionMetadataManager;
  private final CustomResourceFinder<StackGresShardedCluster> clusterFinder;

  @Inject
  public ShardedDbOpsMajorVersionUpgradeExtensionsMutator(
      StackGresContext context,
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresShardedCluster> clusterFinder) {
    this.context = context;
    this.extensionMetadataManager = extensionMetadataManager;
    this.clusterFinder = clusterFinder;
  }

  @Override
  public StackGresShardedDbOps mutate(StackGresShardedDbOpsReview review, StackGresShardedDbOps resource) {
    if (review.getRequest().getOperation() != Operation.CREATE) {
      return resource;
    }
    if (review.getRequest().getObject().getSpec().getSgShardedCluster() == null
        || Optional.of(review.getRequest().getObject())
        .map(StackGresShardedDbOps::getSpec)
        .map(StackGresShardedDbOpsSpec::getMajorVersionUpgrade)
        .isEmpty()) {
      return resource;
    }
    Optional<StackGresShardedCluster> foundCluster = clusterFinder.findByNameAndNamespace(
        review.getRequest().getObject().getSpec().getSgShardedCluster(),
        review.getRequest().getObject().getMetadata().getNamespace());
    String postgresVersion = foundCluster
        .flatMap(cluster -> Optional.of(review.getRequest().getObject())
            .map(StackGresShardedDbOps::getSpec)
            .map(StackGresShardedDbOpsSpec::getMajorVersionUpgrade)
            .map(StackGresShardedDbOpsMajorVersionUpgrade::getPostgresVersion)
            .flatMap(version -> getPostgresFlavorComponent(cluster).get(cluster).findVersion(context, version)))
        .orElse(null);
    if (postgresVersion != null
        && StackGresVersionUtil.getSupportedPostgresVersions(context, foundCluster.get())
        .contains(postgresVersion)) {
      return super.mutate(review, resource);
    }
    return resource;
  }

  @Override
  protected boolean extensionsChanged(
      StackGresShardedDbOpsReview review,
      StackGresCluster cluster,
      StackGresCluster oldCluster) {
    String postgresVersion = Optional.of(cluster.getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .flatMap(version -> getPostgresFlavorComponent(cluster).get(cluster).findVersion(context, version))
        .orElse(null);
    if (postgresVersion == null
        || !StackGresVersionUtil.getSupportedPostgresVersions(context, cluster)
        .contains(postgresVersion)) {
      return false;
    }
    return super.extensionsChanged(review, cluster, oldCluster);
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
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresShardedDbOps resource) {
    return Optional.of(resource)
        .map(StackGresShardedDbOps::getSpec)
        .map(StackGresShardedDbOpsSpec::getMajorVersionUpgrade)
        .map(StackGresShardedDbOpsMajorVersionUpgrade::getToInstallPostgresExtensions);
  }

  @Override
  protected StackGresCluster getCluster(StackGresShardedDbOpsReview review) {
    var cluster = clusterFinder.findByNameAndNamespace(
        review.getRequest().getObject().getSpec().getSgShardedCluster(),
        review.getRequest().getObject().getMetadata().getNamespace())
        .orElse(null);
    if (cluster == null) {
      throw new RuntimeException(
          "SGCluster " + review.getRequest().getObject().getSpec().getSgShardedCluster()
          + "." + review.getRequest().getObject().getMetadata().getNamespace()
          + " not found");
    }
    cluster.getSpec().getPostgres().setVersion(
        review.getRequest().getObject().getSpec().getMajorVersionUpgrade().getPostgresVersion());
    return StackGresShardedClusterForUtil.getCoordinatorCluster(cluster, Optional.empty());
  }

  @Override
  protected StackGresCluster getOldCluster(StackGresShardedDbOpsReview review) {
    return null;
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

  @Override
  protected List<ExtensionTuple> getDefaultExtensions(
      StackGresShardedDbOps resource, StackGresCluster cluster) {
    return StackGresUtil.getDefaultClusterExtensions(context, cluster);
  }

  @Override
  protected void setToInstallExtensions(StackGresShardedDbOps resource,
      List<StackGresClusterInstalledExtension> toInstallExtensions) {
    resource.getSpec().getMajorVersionUpgrade().setToInstallPostgresExtensions(toInstallExtensions);
  }

}
