/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

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
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.mutation.AbstractExtensionsMutator;
import io.stackgres.operator.validation.ValidationUtil;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class ExtensionsMutator
    extends AbstractExtensionsMutator<StackGresCluster, StackGresClusterReview>
    implements ClusterMutator {

  private final ExtensionMetadataManager extensionMetadataManager;

  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      supportedPostgresVersions;

  @Inject
  public ExtensionsMutator(
      ExtensionMetadataManager extensionMetadataManager) {
    this(extensionMetadataManager, ValidationUtil.SUPPORTED_POSTGRES_VERSIONS);
  }

  public ExtensionsMutator(
      ExtensionMetadataManager extensionMetadataManager,
      Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions) {
    this.extensionMetadataManager = extensionMetadataManager;
    this.supportedPostgresVersions = supportedPostgresVersions;
  }

  @Override
  public StackGresCluster mutate(StackGresClusterReview review, StackGresCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }
    String postgresVersion = Optional.of(resource.getSpec())
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .flatMap(getPostgresFlavorComponent(resource).get(resource)::findVersion)
        .orElse(null);
    if (postgresVersion != null
        && supportedPostgresVersions
        .get(getPostgresFlavorComponent(resource))
        .get(StackGresVersion.getStackGresVersion(resource))
        .contains(postgresVersion)) {
      return super.mutate(review, resource);
    }

    return resource;
  }

  @Override
  protected ExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getToInstallPostgresExtensions);
  }

  @Override
  protected StackGresCluster getCluster(StackGresClusterReview review) {
    return review.getRequest().getObject();
  }

  @Override
  protected StackGresCluster getOldCluster(StackGresClusterReview review) {
    return review.getRequest().getOldObject();
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(
      StackGresCluster resource, StackGresCluster cluster) {
    return Optional.of(resource)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions)
        .orElse(List.of());
  }

  @Override
  protected List<ExtensionTuple> getDefaultExtensions(
      StackGresCluster resource, StackGresCluster cluster) {
    return StackGresUtil.getDefaultClusterExtensions(resource);
  }

  @Override
  protected void setToInstallExtensions(StackGresCluster resource,
      List<StackGresClusterInstalledExtension> toInstallExtensions) {
    resource.getSpec().setToInstallPostgresExtensions(toInstallExtensions);
  }

}
