/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.extension.ExtensionUtil;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;
import org.jooq.lambda.Seq;

public abstract class AbstractExtensionsMutator<T extends CustomResource<?, ?>,
    R extends AdmissionReview<T>> implements JsonPatchMutator<R> {

  protected static final ObjectNode EMPTY_OBJECT = FACTORY.objectNode();

  static final JsonPointer CONFIG_POINTER = JsonPointer.of("spec");
  static final JsonPointer TO_INSTALL_EXTENSIONS_POINTER =
      CONFIG_POINTER.append("toInstallPostgresExtensions");

  protected abstract ClusterExtensionMetadataManager getExtensionMetadataManager();

  protected abstract ObjectMapper getObjectMapper();

  @Override
  public List<JsonPatchOperation> mutate(R review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      return Optional.of(review)
          .map(R::getRequest)
          .map(AdmissionRequest<T>::getObject)
          .map(this::mutateExtensions)
          .orElse(ImmutableList.of());
    }

    return ImmutableList.of();
  }

  private ImmutableList<JsonPatchOperation> mutateExtensions(T customResource) {
    final ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
    final StackGresCluster cluster = getCluster(customResource);
    List<StackGresClusterExtension> extensions = getExtensions(customResource);
    List<StackGresClusterInstalledExtension> missingDefaultExtensions = Seq.seq(
        getDefaultExtensions(getCluster(customResource)))
        .filter(defaultExtension -> extensions.stream()
            .noneMatch(extension -> extension.getName()
                .equals(defaultExtension.getName())))
        .collect(ImmutableList.toImmutableList());
    final List<StackGresClusterInstalledExtension> toInstallExtensions =
        Seq.seq(extensions)
        .map(extension -> getToInstallExtension(cluster, extension))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .append(missingDefaultExtensions)
        .collect(ImmutableList.toImmutableList());
    final ArrayNode toInstallExtensionsNode = getObjectMapper().valueToTree(toInstallExtensions);
    if (getToInstallExtensions(customResource).orElse(null) == null) {
      operations.add(new AddOperation(TO_INSTALL_EXTENSIONS_POINTER,
          toInstallExtensionsNode));
    } else if (getToInstallExtensions(customResource)
        .map(previousToInstallExtensions -> toInstallExtensionsHasChanged(
            toInstallExtensions, previousToInstallExtensions))
        .orElse(true)) {
      operations.add(new ReplaceOperation(TO_INSTALL_EXTENSIONS_POINTER,
          toInstallExtensionsNode));
    }
    Seq.seq(extensions)
        .zipWithIndex()
        .forEach(extension -> toInstallExtensions.stream()
            .filter(toInstallExtension -> toInstallExtension.getName()
                .equals(extension.v1.getName()))
            .findFirst()
            .ifPresent(installedExtension -> onExtensionToInstall(
                operations, extension.v1, extension.v2.intValue(), installedExtension)));
    return operations.build();
  }

  private boolean toInstallExtensionsHasChanged(
      List<StackGresClusterInstalledExtension> toInstallExtensions,
      List<StackGresClusterInstalledExtension> previousToInstallExtensions) {
    return previousToInstallExtensions.size() != toInstallExtensions.size()
        || !previousToInstallExtensions.stream()
            .allMatch(previousToInstallExtension -> toInstallExtensions.stream()
                .anyMatch(previousToInstallExtension::equals));
  }

  protected abstract Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      T customResource);

  protected abstract StackGresCluster getCluster(T customResource);

  protected abstract List<StackGresClusterExtension> getExtensions(T customResource);

  protected abstract List<StackGresClusterInstalledExtension> getDefaultExtensions(
      StackGresCluster cluster);

  protected void onExtensionToInstall(ImmutableList.Builder<JsonPatchOperation> operations,
      final StackGresClusterExtension extension, final int index,
      final StackGresClusterInstalledExtension installedExtension) {
    final JsonPointer extensionVersionPointer =
        TO_INSTALL_EXTENSIONS_POINTER.append(index).append("version");
    final TextNode extensionVersion = new TextNode(installedExtension.getVersion());
    if (extension.getVersion() == null) {
      operations.add(new AddOperation(extensionVersionPointer, extensionVersion));
    } else if (!installedExtension.getVersion().equals(extension.getVersion())) {
      operations.add(new ReplaceOperation(extensionVersionPointer, extensionVersion));
    }
  }

  protected Optional<StackGresClusterInstalledExtension> getExtension(StackGresCluster cluster,
      String extensionName) {
    StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setName(extensionName);
    return getExtensionMetadataManager()
        .findExtensionCandidateAnyVersion(cluster, extension, false)
        .map(extensionMetadata -> ExtensionUtil.getInstalledExtension(
            extension, extensionMetadata));
  }

  protected Optional<StackGresClusterInstalledExtension> getExtension(StackGresCluster cluster,
      String extensionName, String extensionVersion) {
    StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setName(extensionName);
    extension.setVersion(extensionVersion);
    return getExtensionMetadataManager()
        .findExtensionCandidateSameMajorBuild(cluster, extension, false)
        .map(extensionMetadata -> ExtensionUtil.getInstalledExtension(
            extension, extensionMetadata));
  }

  private Optional<StackGresClusterInstalledExtension> getToInstallExtension(
      StackGresCluster cluster, StackGresClusterExtension extension) {
    return getExtensionMetadataManager()
        .findExtensionCandidateSameMajorBuild(cluster, extension, false)
        .map(extensionMetadata -> ExtensionUtil.getInstalledExtension(
            extension, extensionMetadata));
  }

}
