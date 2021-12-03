/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.extension.ExtensionUtil;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operator.mutation.AbstractExtensionsMutator;
import io.stackgres.operator.mutation.ClusterExtensionMetadataManager;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ExtensionsMutator
    extends AbstractExtensionsMutator<StackGresCluster, StackGresClusterReview>
    implements ClusterMutator {

  private final ClusterExtensionMetadataManager extensionMetadataManager;
  private final ObjectMapper objectMapper;

  @Inject
  public ExtensionsMutator(
      ClusterExtensionMetadataManager extensionMetadataManager,
      ObjectMapper objectMapper) {
    this.extensionMetadataManager = extensionMetadataManager;
    this.objectMapper = objectMapper;
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    return new ImmutableList.Builder<JsonPatchOperation>()
        .addAll(mutateExtensionChannels(review))
        .addAll(super.mutate(review))
        .build();
  }

  private List<JsonPatchOperation> mutateExtensionChannels(StackGresClusterReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      Optional.of(review)
          .map(StackGresClusterReview::getRequest)
          .map(AdmissionRequest<StackGresCluster>::getObject)
          .ifPresent(cluster -> {
            Optional.of(cluster)
                .map(StackGresCluster::getSpec)
                .map(StackGresClusterSpec::getPostgres)
                .map(StackGresClusterPostgres::getExtensions)
                .stream()
                .flatMap(extensions -> Seq.seq(extensions).zipWithIndex())
                .forEach(Unchecked.consumer(extension -> {
                  final JsonPointer extensionVersionPointer =
                      CLUSTER_CONFIG_POINTER.append("postgres").append("extensions")
                          .append(extension.v2.intValue()).append("version");
                  getToInstallExtension(cluster, extension.v1)
                      .ifPresent(toInstallExtension -> {
                        leaveOrAddOrReplaceExtensionVersion(operations, extension,
                            extensionVersionPointer, toInstallExtension);
                        extension.v1.setVersion(toInstallExtension.getVersion());
                      });
                }));
          });
      return operations.build();
    }

    return ImmutableList.of();
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
      ImmutableList.Builder<JsonPatchOperation> operations,
      Tuple2<StackGresClusterExtension, Long> extension,
      final JsonPointer extensionVersionPointer,
      StackGresClusterInstalledExtension installedExtension) {
    final TextNode extensionVersion = new TextNode(installedExtension.getVersion());
    if (extension.v1.getVersion() == null) {
      operations.add(new AddOperation(extensionVersionPointer, extensionVersion));
    } else if (!installedExtension.getVersion().equals(extension.v1.getVersion())) {
      operations
          .add(new ReplaceOperation(extensionVersionPointer, extensionVersion));
    }
  }

  @Override
  protected ClusterExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected ObjectMapper getObjectMapper() {
    return objectMapper;
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
        .orElse(ImmutableList.of());
  }

  @Override
  protected ImmutableList<StackGresClusterInstalledExtension> getDefaultExtensions(
      StackGresCluster cluster) {
    return Seq.seq(StackGresUtil.getDefaultClusterExtensions(cluster))
        .map(t -> t.v2
        .map(version -> getExtension(cluster, t.v1, version))
            .orElseGet(() -> getExtension(cluster, t.v1)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  protected void onExtensionToInstall(Builder<JsonPatchOperation> operations,
      StackGresClusterExtension extension, int index,
      StackGresClusterInstalledExtension installedExtension) {
    final JsonPointer extensionVersionPointer =
        CLUSTER_CONFIG_POINTER.append("postgres").append("extensions")
        .append(index).append("version");
    final TextNode extensionVersion = new TextNode(installedExtension.getVersion());
    if (extension.getVersion() == null) {
      operations.add(new AddOperation(extensionVersionPointer, extensionVersion));
    } else if (!installedExtension.getVersion().equals(extension.getVersion())) {
      operations.add(new ReplaceOperation(extensionVersionPointer, extensionVersion));
    }
  }

}
