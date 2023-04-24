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

import com.google.common.collect.ImmutableList;
import io.stackgres.common.StackGresComponent;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.mutation.AbstractExtensionsMutator;
import io.stackgres.operator.validation.ValidationUtil;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

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
    if (postgresVersion != null && supportedPostgresVersions
        .get(getPostgresFlavorComponent(resource))
        .get(StackGresVersion.getStackGresVersion(resource))
        .contains(postgresVersion)) {
      mutateExtensionChannels(resource);
      return super.mutate(review, resource);
    }

    return resource;
  }

  private void mutateExtensionChannels(StackGresCluster resource) {
    if (resource != null) {
      Optional.of(resource)
          .map(StackGresCluster::getSpec)
          .map(StackGresClusterSpec::getPostgres)
          .map(StackGresClusterPostgres::getExtensions)
          .stream()
          .flatMap(List::stream)
          .forEach(Unchecked.consumer(extension -> {
            getToInstallExtensionMetadata(resource, extension)
                .ifPresent(toInstallExtension -> {
                  leaveOrAddOrReplaceExtensionVersion(extension, toInstallExtension);
                });
          }));
    }
  }

  private Optional<StackGresExtensionMetadata> getToInstallExtensionMetadata(
      StackGresCluster cluster, StackGresClusterExtension extension) {
    Optional<StackGresExtensionMetadata> exactCandidateExtension =
        extensionMetadataManager
        .findExtensionCandidateSameMajorBuild(cluster, extension, false);
    if (exactCandidateExtension.isEmpty()) {
      List<StackGresExtensionMetadata> candidateExtensionMetadatas =
          extensionMetadataManager.getExtensionsAnyVersion(cluster, extension, false);
      if (candidateExtensionMetadatas.size() == 1) {
        return Optional.of(candidateExtensionMetadatas.get(0));
      }
      return Optional.empty();
    }
    return exactCandidateExtension;
  }

  private void leaveOrAddOrReplaceExtensionVersion(
      StackGresClusterExtension extension,
      StackGresExtensionMetadata extensionMetadata) {
    if (extension.getVersion() == null
        || Optional.ofNullable(extensionMetadata.getExtension().getChannels())
        .map(map -> map.containsKey(extension.getVersion()))
        .orElse(false)) {
      extension.setVersion(extensionMetadata.getVersion().getVersion());
    }
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
  protected StackGresCluster getCluster(StackGresCluster cluster) {
    return cluster;
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions)
        .orElse(List.of());
  }

  @Override
  protected ImmutableList<StackGresClusterInstalledExtension> getDefaultExtensions(
      StackGresCluster cluster) {
    return Seq.seq(StackGresUtil.getDefaultClusterExtensions(cluster))
        .map(t -> t.extensionVersion()
        .map(version -> getExtension(cluster, t.extensionName(), version))
            .orElseGet(() -> getExtension(cluster, t.extensionName())))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  protected void setToInstallExtensions(StackGresCluster resource,
      List<StackGresClusterInstalledExtension> toInstallExtensions) {
    resource.getSpec().setToInstallPostgresExtensions(toInstallExtensions);
  }

}
