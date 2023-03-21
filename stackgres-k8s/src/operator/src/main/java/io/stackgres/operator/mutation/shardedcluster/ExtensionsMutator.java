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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
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
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class ExtensionsMutator
    extends AbstractExtensionsMutator<StackGresShardedCluster, StackGresShardedClusterReview>
    implements ShardedClusterMutator {

  static final JsonPointer STATUS_POINTER = JsonPointer.of("status");
  static final JsonPointer TO_INSTALL_EXTENSIONS_POINTER =
      STATUS_POINTER.append("toInstallPostgresExtensions");

  private final OperatorExtensionMetadataManager extensionMetadataManager;
  private final ObjectMapper objectMapper;
  private final Map<StackGresComponent, Map<StackGresVersion, List<String>>>
      supportedPostgresVersions;

  @Inject
  public ExtensionsMutator(
      OperatorExtensionMetadataManager extensionMetadataManager,
      ObjectMapper objectMapper) {
    this(extensionMetadataManager, objectMapper,
        ValidationUtil.SUPPORTED_SHARDED_CLUSTER_POSTGRES_VERSIONS);
  }

  public ExtensionsMutator(
      OperatorExtensionMetadataManager extensionMetadataManager,
      ObjectMapper objectMapper,
      Map<StackGresComponent, Map<StackGresVersion, List<String>>> supportedPostgresVersions) {
    this.extensionMetadataManager = extensionMetadataManager;
    this.objectMapper = objectMapper;
    this.supportedPostgresVersions = supportedPostgresVersions;
  }

  @Override
  protected JsonPointer getToInstallExtensionsPointer() {
    return TO_INSTALL_EXTENSIONS_POINTER;
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresShardedClusterReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      StackGresShardedCluster cluster = review.getRequest().getObject();
      String postgresVersion = Optional.of(cluster.getSpec())
          .map(StackGresShardedClusterSpec::getPostgres)
          .map(StackGresClusterPostgres::getVersion)
          .flatMap(getPostgresFlavorComponent(cluster).get(cluster)::findVersion)
          .orElse(null);
      if (postgresVersion != null && supportedPostgresVersions
          .get(getPostgresFlavorComponent(cluster))
          .get(StackGresVersion.getStackGresVersion(cluster))
          .contains(postgresVersion)) {
        return new ImmutableList.Builder<JsonPatchOperation>()
            .addAll(mutateExtensionChannels(review))
            .addAll(super.mutate(review))
            .build();
      }
    }

    return List.of();
  }

  private List<JsonPatchOperation> mutateExtensionChannels(StackGresShardedClusterReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      Optional.of(review)
          .map(StackGresShardedClusterReview::getRequest)
          .map(AdmissionRequest<StackGresShardedCluster>::getObject)
          .ifPresent(cluster -> {
            if (cluster.getStatus() == null) {
              cluster.setStatus(new StackGresShardedClusterStatus());
              operations.add(new AddOperation(STATUS_POINTER, EMPTY_OBJECT));
            }
            StackGresCluster coordinatorCluster = getCoordinatorCluster(cluster);
            Optional.of(cluster)
                .map(StackGresShardedCluster::getSpec)
                .map(StackGresShardedClusterSpec::getPostgres)
                .map(StackGresClusterPostgres::getExtensions)
                .stream()
                .flatMap(extensions -> Seq.seq(extensions).zipWithIndex())
                .forEach(Unchecked.consumer(extension -> {
                  final JsonPointer extensionVersionPointer =
                      SPEC_POINTER.append("postgres").append("extensions")
                          .append(extension.v2.intValue()).append("version");
                  getToInstallExtension(coordinatorCluster, extension.v1)
                      .ifPresent(toInstallExtension -> {
                        leaveOrAddOrReplaceExtensionVersion(operations, extension,
                            extensionVersionPointer, toInstallExtension);
                        extension.v1.setVersion(toInstallExtension.getVersion());
                      });
                }));
          });
      return operations.build();
    }

    return List.of();
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
  protected OperatorExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected ObjectMapper getObjectMapper() {
    return objectMapper;
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
  protected ImmutableList<StackGresClusterInstalledExtension> getDefaultExtensions(
      StackGresCluster cluster) {
    return Seq.seq(StackGresUtil.getDefaultShardedClusterExtensions(cluster))
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
        SPEC_POINTER.append("postgres").append("extensions")
        .append(index).append("version");
    final TextNode extensionVersion = new TextNode(installedExtension.getVersion());
    if (extension.getVersion() == null) {
      operations.add(new AddOperation(extensionVersionPointer, extensionVersion));
    } else if (!installedExtension.getVersion().equals(extension.getVersion())) {
      operations.add(new ReplaceOperation(extensionVersionPointer, extensionVersion));
    }
  }

}
