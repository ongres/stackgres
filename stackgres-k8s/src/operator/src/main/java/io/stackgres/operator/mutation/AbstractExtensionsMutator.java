/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.RemoveOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.client.CustomResource;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.extension.ExtensionUtil;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.AdmissionReview;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import io.stackgres.operatorframework.admissionwebhook.mutating.JsonPatchMutator;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractExtensionsMutator<T extends CustomResource<?, ?>,
    R extends AdmissionReview<T>> implements JsonPatchMutator<R> {

  protected static final ObjectNode EMPTY_OBJECT = FACTORY.objectNode();
  protected static final ArrayNode EMPTY_ARRAY = FACTORY.arrayNode();

  static final JsonPointer CONFIG_POINTER = JsonPointer.of("spec");
  static final JsonPointer STATUS_POINTER = JsonPointer.of("status");
  static final JsonPointer TO_INSTALL_EXTENSIONS_POINTER =
      STATUS_POINTER.append("toInstallPostgresExtensions");
  private final ClusterExtensionMetadataManager extensionMetadataManager;

  public AbstractExtensionsMutator(
      ClusterExtensionMetadataManager extensionMetadataManager) {
    this.extensionMetadataManager = extensionMetadataManager;
  }

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
    final List<StackGresClusterExtension> allExtensions =
        getExtensionsWithDefaults(customResource);
    final List<StackGresClusterInstalledExtension> toInstallExtensions =
        new ArrayList<>(getToInstallExtensions(customResource));
    if (customResource.getStatus() == null) {
      operations.add(new AddOperation(STATUS_POINTER, EMPTY_OBJECT));
      operations.add(new AddOperation(TO_INSTALL_EXTENSIONS_POINTER,
          EMPTY_ARRAY));
    } else if (getCluster(customResource).getStatus().getToInstallPostgresExtensions() == null) {
      operations.add(new AddOperation(TO_INSTALL_EXTENSIONS_POINTER,
          EMPTY_ARRAY));
    }
    List<Integer> toRemoveFromToInstallPostgresExtensions = new ArrayList<>(
        Seq.seq(toInstallExtensions)
        .zipWithIndex()
        .filter(anInstalledExtension -> Seq.seq(allExtensions)
            .noneMatch(anInstalledExtension.v1::same))
        .map(Tuple2::v2)
        .map(Long::intValue)
        .collect(ImmutableList.toImmutableList()));
    Seq.seq(allExtensions)
        .zipWithIndex()
        .forEach(Unchecked.consumer(extension -> {
          final StackGresClusterInstalledExtension installedExtension =
              getToInstallExtension(getCluster(customResource), extension);
          onExtension(operations, extension.v1, extension.v2.intValue(), installedExtension);
          addOrReplaceToInstallExtension(operations, toInstallExtensions,
              toRemoveFromToInstallPostgresExtensions, installedExtension);
        }));
    for (int toRemoveFromToInstallPostgresExtension : toRemoveFromToInstallPostgresExtensions) {
      final JsonPointer installedExtensionPointer =
          TO_INSTALL_EXTENSIONS_POINTER.append(toRemoveFromToInstallPostgresExtension);
      operations.add(new RemoveOperation(installedExtensionPointer));
    }
    return operations.build();
  }

  protected abstract List<StackGresClusterInstalledExtension> getToInstallExtensions(
      T customResource);

  private List<StackGresClusterExtension> getExtensionsWithDefaults(T customResource) {
    List<StackGresClusterExtension> extensions = getExtensions(customResource);
    List<StackGresClusterExtension> missingDefaultExtensions = Seq.seq(
        getDefaultExtensions(getCluster(customResource)))
        .filter(defaultExtension -> extensions.stream()
            .noneMatch(extension -> extension.getName()
                .equals(defaultExtension.getName())))
        .collect(ImmutableList.toImmutableList());
    return Seq.seq(extensions)
        .append(missingDefaultExtensions)
        .collect(ImmutableList.toImmutableList());
  }

  protected abstract StackGresCluster getCluster(T customResource);

  protected abstract List<StackGresClusterExtension> getExtensions(T customResource);

  protected abstract List<StackGresClusterExtension> getDefaultExtensions(StackGresCluster cluster);

  protected void onExtension(ImmutableList.Builder<JsonPatchOperation> operations,
      final StackGresClusterExtension extension, final int index,
      final StackGresClusterInstalledExtension installedExtension) {
    final JsonPointer extensionVersionPointer =
        CONFIG_POINTER.append("postgresExtensions")
        .append(index).append("version");
    final TextNode extensionVersion = new TextNode(installedExtension.getVersion());
    if (extension.getVersion() == null) {
      operations.add(new AddOperation(extensionVersionPointer, extensionVersion));
    } else if (!installedExtension.getVersion().equals(extension.getVersion())) {
      operations.add(new ReplaceOperation(extensionVersionPointer, extensionVersion));
    }
  }

  protected StackGresClusterExtension getExtension(StackGresCluster cluster, String extensionName) {
    StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setName(extensionName);
    extension.setVersion(Unchecked.supplier(
        () -> extensionMetadataManager.getExtensionCandidateAnyVersion(
            cluster, extension)).get().getVersion().getVersion());
    return extension;
  }

  protected StackGresClusterExtension getExtension(StackGresCluster cluster, String extensionName,
      String extensionVersion) {
    StackGresClusterExtension extension = new StackGresClusterExtension();
    extension.setName(extensionName);
    extension.setVersion(extensionVersion);
    extension.setVersion(Unchecked.supplier(
        () -> extensionMetadataManager.getExtensionCandidateSameMajorBuild(
            cluster, extension)).get().getVersion().getVersion());
    return extension;
  }

  private StackGresClusterInstalledExtension getToInstallExtension(StackGresCluster cluster,
      Tuple2<StackGresClusterExtension, Long> extension) throws Exception {
    final StackGresExtensionMetadata extensionMetadata =
        extensionMetadataManager.getExtensionCandidateSameMajorBuild(
            cluster, extension.v1);
    return ExtensionUtil.getInstalledExtension(extension.v1, extensionMetadata);
  }

  private void addOrReplaceToInstallExtension(
      final ImmutableList.Builder<JsonPatchOperation> operations,
      final List<StackGresClusterInstalledExtension> toInstallExtensions,
      final List<Integer> toRemoveFromToInstallPostgresExtensions,
      final StackGresClusterInstalledExtension installedExtension) {
    if (Seq.seq(toInstallExtensions)
        .anyMatch(installedExtension::same)) {
      Seq.seq(toInstallExtensions)
          .zipWithIndex()
          .filter(previousInstalledExtension -> previousInstalledExtension.v1
              .same(installedExtension))
          .filter(previousInstalledExtension -> !previousInstalledExtension.v1
              .equals(installedExtension))
          .forEach(previousInstalledExtension -> {
            final JsonPointer installedExtensionPointer =
                TO_INSTALL_EXTENSIONS_POINTER
                .append(previousInstalledExtension.v2.intValue());
            replaceToInstallExtension(operations, previousInstalledExtension.v1,
                installedExtensionPointer, installedExtension);
          });
    } else if (!toRemoveFromToInstallPostgresExtensions.isEmpty()) {
      final int toRemoveFromToInstallPostgresExtension =
          toRemoveFromToInstallPostgresExtensions.get(0).intValue();
      final JsonPointer installedExtensionPointer =
          TO_INSTALL_EXTENSIONS_POINTER
          .append(toRemoveFromToInstallPostgresExtension);
      replaceToInstallExtension(operations,
          toInstallExtensions.get(toRemoveFromToInstallPostgresExtension),
          installedExtensionPointer, installedExtension);
      toRemoveFromToInstallPostgresExtensions.remove(0);
    } else {
      final JsonPointer installedExtensionPointer =
          TO_INSTALL_EXTENSIONS_POINTER.append(toInstallExtensions.size());
      toInstallExtensions.add(installedExtension);
      addToInstallExtension(operations, installedExtensionPointer,
          installedExtension);
    }
  }

  private void replaceToInstallExtension(ImmutableList.Builder<JsonPatchOperation> operations,
      StackGresClusterInstalledExtension previousInstalledExtension,
      JsonPointer installedExtensionPointer,
      StackGresClusterInstalledExtension installedExtension) {
    operations.add(new ReplaceOperation(
        installedExtensionPointer.append("publisher"),
        new TextNode(installedExtension.getPublisher())));
    operations.add(new ReplaceOperation(
        installedExtensionPointer.append("repository"),
        new TextNode(installedExtension.getRepository())));
    operations.add(new ReplaceOperation(
        installedExtensionPointer.append("version"),
        new TextNode(installedExtension.getVersion())));
    operations.add(new ReplaceOperation(
        installedExtensionPointer.append("postgresVersion"),
        new TextNode(installedExtension.getPostgresVersion())));
    if (installedExtension.getBuild() != null) {
      if (previousInstalledExtension.getBuild() != null) {
        operations.add(new ReplaceOperation(
            installedExtensionPointer.append("build"),
            new TextNode(installedExtension.getBuild())));
      } else {
        operations.add(new AddOperation(
            installedExtensionPointer.append("build"),
            new TextNode(installedExtension.getBuild())));
      }
    } else if (previousInstalledExtension.getBuild() != null) {
      operations.add(new RemoveOperation(
          installedExtensionPointer.append("build")));
    }
    if (installedExtension.getExtraMounts() != null) {
      final ArrayNode extraMounts = FACTORY.arrayNode();
      for (String extraMount : installedExtension.getExtraMounts()) {
        extraMounts.add(extraMount);
      }
      if (previousInstalledExtension.getExtraMounts() != null) {
        operations.add(new ReplaceOperation(
            installedExtensionPointer.append("extraMounts"),
            extraMounts));
      } else {
        operations.add(new AddOperation(
            installedExtensionPointer.append("extraMounts"),
            extraMounts));
      }
    } else if (previousInstalledExtension.getExtraMounts() != null) {
      operations.add(new RemoveOperation(
          installedExtensionPointer.append("extraMounts")));
    }
  }

  private void addToInstallExtension(ImmutableList.Builder<JsonPatchOperation> operations,
      JsonPointer installedExtensionPointer,
      StackGresClusterInstalledExtension installedExtension) {
    operations.add(new AddOperation(
        installedExtensionPointer.append("name"),
        new TextNode(installedExtension.getName())));
    operations.add(new AddOperation(
        installedExtensionPointer.append("publisher"),
        new TextNode(installedExtension.getPublisher())));
    operations.add(new AddOperation(
        installedExtensionPointer.append("repository"),
        new TextNode(installedExtension.getRepository())));
    operations.add(new AddOperation(
        installedExtensionPointer.append("version"),
        new TextNode(installedExtension.getVersion())));
    operations.add(new AddOperation(
        installedExtensionPointer.append("postgresVersion"),
        new TextNode(installedExtension.getPostgresVersion())));
    operations.add(new AddOperation(
        installedExtensionPointer.append("build"),
        new TextNode(installedExtension.getBuild())));
    if (installedExtension.getExtraMounts() != null) {
      final ArrayNode extraMounts = FACTORY.arrayNode();
      for (String extraMount : installedExtension.getExtraMounts()) {
        extraMounts.add(extraMount);
      }
      operations.add(new AddOperation(
          installedExtensionPointer.append("extraMounts"),
          extraMounts));
    }
  }

}
