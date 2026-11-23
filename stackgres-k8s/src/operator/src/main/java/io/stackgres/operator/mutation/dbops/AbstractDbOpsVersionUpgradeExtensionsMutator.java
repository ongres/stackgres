/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.dbops;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.stackgres.common.CdiUtil;
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
import io.stackgres.operator.mutation.AbstractExtensionsMutator;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;

/**
 * Set the extensions to install for the target postgres version of an SGDbOps that changes the
 * postgres version of an SGCluster. The extensions are those of the SGCluster, where the
 * extensions set in the SGDbOps replace those with the same name.
 */
public abstract class AbstractDbOpsVersionUpgradeExtensionsMutator
    extends AbstractExtensionsMutator<StackGresDbOps, StackGresDbOpsReview>
    implements DbOpsMutator {

  private final ExtensionMetadataManager extensionMetadataManager;
  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      supportedPostgresVersions;

  protected AbstractDbOpsVersionUpgradeExtensionsMutator(
      ExtensionMetadataManager extensionMetadataManager,
      CustomResourceFinder<StackGresCluster> clusterFinder,
      Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions) {
    this.extensionMetadataManager = extensionMetadataManager;
    this.clusterFinder = clusterFinder;
    this.supportedPostgresVersions = supportedPostgresVersions;
  }

  public AbstractDbOpsVersionUpgradeExtensionsMutator() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.extensionMetadataManager = null;
    this.clusterFinder = null;
    this.supportedPostgresVersions = null;
  }

  /**
   * Return true when the SGDbOps is of the version upgrade handled by this mutator.
   */
  protected abstract boolean isVersionUpgrade(StackGresDbOps dbOps);

  protected abstract String getTargetPostgresVersion(StackGresDbOps dbOps);

  protected abstract List<StackGresClusterExtension> getPostgresExtensions(StackGresDbOps dbOps);

  @Override
  public StackGresDbOps mutate(StackGresDbOpsReview review, StackGresDbOps resource) {
    if (review.getRequest().getOperation() != Operation.CREATE) {
      return resource;
    }
    if (review.getRequest().getObject().getSpec().getSgCluster() == null
        || !isVersionUpgrade(review.getRequest().getObject())) {
      return resource;
    }
    return super.mutate(review, resource);
  }

  @Override
  protected boolean extensionsChanged(
      StackGresDbOpsReview review,
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
  protected StackGresCluster getCluster(StackGresDbOpsReview review) {
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
        getTargetPostgresVersion(review.getRequest().getObject()));
    return cluster;
  }

  @Override
  protected StackGresCluster getOldCluster(StackGresDbOpsReview review) {
    return null;
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
  protected List<ExtensionTuple> getDefaultExtensions(
      StackGresDbOps resource, StackGresCluster cluster) {
    return StackGresUtil.getDefaultClusterExtensions(cluster);
  }

}
