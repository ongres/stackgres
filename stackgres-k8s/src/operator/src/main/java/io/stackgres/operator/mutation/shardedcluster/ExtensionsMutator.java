/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.ArrayList;
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
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.operator.common.StackGresShardedClusterForCitusUtil;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.mutation.AbstractExtensionsMutator;
import io.stackgres.operator.validation.ValidationUtil;
import io.stackgres.operatorframework.admissionwebhook.Operation;

@ApplicationScoped
public class ExtensionsMutator
    extends AbstractExtensionsMutator<StackGresShardedCluster, StackGresShardedClusterReview>
    implements ShardedClusterMutator {

  private final ExtensionMetadataManager extensionMetadataManager;
  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      supportedPostgresVersions;

  @Inject
  public ExtensionsMutator(
      ExtensionMetadataManager extensionMetadataManager) {
    this(extensionMetadataManager,
        ValidationUtil.SUPPORTED_SHARDED_CLUSTER_POSTGRES_VERSIONS);
  }

  public ExtensionsMutator(
      ExtensionMetadataManager extensionMetadataManager,
      Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions) {
    this.extensionMetadataManager = extensionMetadataManager;
    this.supportedPostgresVersions = supportedPostgresVersions;
  }

  @Override
  public StackGresShardedCluster mutate(
      StackGresShardedClusterReview review, StackGresShardedCluster resource) {
    if (review.getRequest().getOperation() != Operation.CREATE
        && review.getRequest().getOperation() != Operation.UPDATE) {
      return resource;
    }

    String postgresVersion = Optional.of(resource.getSpec())
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getVersion)
        .flatMap(getPostgresFlavorComponent(resource).get(resource)::findVersion)
        .orElse(null);

    if (postgresVersion != null
        && supportedPostgresVersions
        .get(getPostgresFlavorComponent(resource))
        .get(StackGresVersion.getStackGresVersion(resource))
        .contains(postgresVersion)) {
      getDefaultExtensions(resource, null).stream()
          .filter(defaultExtension -> Optional.of(resource.getSpec())
              .map(StackGresShardedClusterSpec::getPostgres)
              .map(StackGresClusterPostgres::getExtensions)
              .stream()
              .flatMap(List::stream)
              .noneMatch(extension -> extension.getName().equals(defaultExtension.extensionName())
                  && extension.getVersion() != null))
          .forEach(defaultExtension -> setDefaultExtension(resource, defaultExtension));

      return super.mutate(review, resource);
    }

    return resource;
  }

  private void setDefaultExtension(
      StackGresShardedCluster resource, ExtensionTuple defaultExtension) {
    if (resource.getSpec().getPostgres() == null) {
      resource.getSpec().setPostgres(new StackGresClusterPostgres());
    }
    if (resource.getSpec().getPostgres().getExtensions() == null) {
      resource.getSpec().getPostgres().setExtensions(new ArrayList<>());
    }
    var foundExtension = Optional.of(resource.getSpec())
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions)
        .stream()
        .flatMap(List::stream)
        .filter(extension -> extension.getName()
            .equals(defaultExtension.extensionName()))
        .findFirst();
    final StackGresClusterExtension extension;
    if (foundExtension.isEmpty()) {
      extension = new StackGresClusterExtension();
      resource.getSpec().getPostgres().getExtensions().add(extension);
    } else {
      extension = foundExtension.get();
    }
    extension.setName(defaultExtension.extensionName());
    Optional.ofNullable(resource.getStatus())
        .filter(installedExtension -> foundExtension.isEmpty())
        .map(StackGresShardedClusterStatus::getToInstallPostgresExtensions)
        .stream()
        .flatMap(List::stream)
        .filter(installedExtension -> installedExtension.getName()
            .equals(defaultExtension.extensionName()))
        .map(StackGresClusterInstalledExtension::getVersion)
        .findFirst()
        .or(defaultExtension::extensionVersion)
        .ifPresent(extension::setVersion);
  }

  @Override
  protected ExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresShardedCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresShardedCluster::getStatus)
        .map(StackGresShardedClusterStatus::getToInstallPostgresExtensions);
  }

  @Override
  protected StackGresCluster getCluster(StackGresShardedClusterReview review) {
    return StackGresShardedClusterForCitusUtil
        .getCoordinatorCluster(review.getRequest().getObject());
  }

  @Override
  protected StackGresCluster getOldCluster(StackGresShardedClusterReview review) {
    return Optional.ofNullable(review.getRequest().getOldObject())
        .map(StackGresShardedClusterForCitusUtil::getCoordinatorCluster)
        .orElse(null);
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(
      StackGresShardedCluster resource, StackGresCluster cluster) {
    return Optional.of(resource)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions)
        .orElse(List.of());
  }

  @Override
  protected List<ExtensionTuple> getDefaultExtensions(
      StackGresShardedCluster resource, StackGresCluster cluster) {
    return StackGresUtil.getDefaultShardedClusterExtensions(resource);
  }

  @Override
  protected void setToInstallExtensions(StackGresShardedCluster resource,
      List<StackGresClusterInstalledExtension> toInstallExtensions) {
    if (resource.getStatus() == null) {
      resource.setStatus(new StackGresShardedClusterStatus());
    }
    resource.getStatus().setToInstallPostgresExtensions(toInstallExtensions);
  }

}
