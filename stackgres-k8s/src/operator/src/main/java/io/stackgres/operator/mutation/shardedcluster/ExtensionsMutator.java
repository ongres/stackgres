/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.shardedcluster;

import static io.stackgres.common.StackGresShardedClusterUtil.getCoordinatorCluster;
import static io.stackgres.common.StackGresUtil.getPostgresFlavorComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresShardedClusterUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.extension.ExtensionUtil;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.operator.common.OperatorExtensionMetadataManager;
import io.stackgres.operator.common.StackGresShardedClusterReview;
import io.stackgres.operator.mutation.AbstractExtensionsMutator;
import io.stackgres.operator.validation.ValidationUtil;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class ExtensionsMutator
    extends AbstractExtensionsMutator<StackGresShardedCluster, StackGresShardedClusterReview>
    implements ShardedClusterMutator {

  private final OperatorExtensionMetadataManager extensionMetadataManager;
  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      supportedPostgresVersions;

  @Inject
  public ExtensionsMutator(
      OperatorExtensionMetadataManager extensionMetadataManager) {
    this(extensionMetadataManager,
        ValidationUtil.SUPPORTED_SHARDED_CLUSTER_POSTGRES_VERSIONS);
  }

  public ExtensionsMutator(
      OperatorExtensionMetadataManager extensionMetadataManager,
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
    if (postgresVersion != null && supportedPostgresVersions
        .get(getPostgresFlavorComponent(resource))
        .get(StackGresVersion.getStackGresVersion(resource))
        .contains(postgresVersion)) {
      mutateExtensionChannels(resource);
      return super.mutate(review, resource);
    }

    return resource;
  }

  private void mutateExtensionChannels(StackGresShardedCluster resource) {
    if (resource != null) {
      StackGresCluster coordinatorCluster = getCoordinatorCluster(resource);
      Optional.of(resource)
          .map(StackGresShardedCluster::getSpec)
          .map(StackGresShardedClusterSpec::getPostgres)
          .map(StackGresClusterPostgres::getExtensions)
          .stream()
          .flatMap(extensions -> Seq.seq(extensions))
          .forEach(Unchecked.consumer(extension -> {
            getToInstallExtension(coordinatorCluster, extension)
                .ifPresent(toInstallExtension -> {
                  leaveOrAddOrReplaceExtensionVersion(extension, toInstallExtension);
                });
          }));
    }
  }

  private Optional<StackGresClusterInstalledExtension> getToInstallExtension(
      StackGresCluster cluster, StackGresClusterExtension extension) {
    Optional<StackGresClusterInstalledExtension> exactCandidateExtension =
        extensionMetadataManager
        .findExtensionCandidateSameMajorBuild(cluster, extension, false)
        .map(extensionMetadata -> ExtensionUtil.getInstalledExtension(
            cluster, extension, extensionMetadata, false));
    if (exactCandidateExtension.isEmpty()) {
      List<StackGresExtensionMetadata> candidateExtensionMetadatas =
          extensionMetadataManager.getExtensionsAnyVersion(cluster, extension, false);
      if (candidateExtensionMetadatas.size() == 1) {
        return Optional.of(ExtensionUtil.getInstalledExtension(
            cluster, extension, candidateExtensionMetadatas.get(0), false));
      }
      return Optional.empty();
    }
    return exactCandidateExtension;
  }

  private void leaveOrAddOrReplaceExtensionVersion(
      StackGresClusterExtension extension,
      StackGresClusterInstalledExtension installedExtension) {
    if (extension.getVersion() == null
        || !installedExtension.getVersion().equals(extension.getVersion())) {
      extension.setVersion(installedExtension.getVersion());
    }
  }

  @Override
  protected OperatorExtensionMetadataManager getExtensionMetadataManager() {
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
  protected StackGresCluster getCluster(StackGresShardedCluster cluster) {
    return StackGresShardedClusterUtil.getCoordinatorCluster(cluster);
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(StackGresShardedCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresShardedCluster::getSpec)
        .map(StackGresShardedClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions)
        .orElse(List.of());
  }

  @Override
  protected List<StackGresClusterInstalledExtension> getDefaultExtensions(
      StackGresCluster cluster) {
    return Seq.seq(StackGresUtil.getDefaultShardedClusterExtensions(cluster))
        .map(t -> t.v2
        .map(version -> getExtension(cluster, t.v1, version))
            .orElseGet(() -> getExtension(cluster, t.v1)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
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
