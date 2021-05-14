/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.cluster;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonpatch.AddOperation;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.jsonpatch.ReplaceOperation;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.extension.ExtensionUtil;
import io.stackgres.common.extension.StackGresExtensionMetadata;
import io.stackgres.operator.common.StackGresClusterReview;
import io.stackgres.operatorframework.admissionwebhook.AdmissionRequest;
import io.stackgres.operatorframework.admissionwebhook.Operation;
import org.jooq.lambda.Seq;
import org.jooq.lambda.Unchecked;

@ApplicationScoped
public class ExtensionsChannelMutator implements ClusterMutator {

  private final ClusterExtensionMetadataManager extensionMetadataManager;

  @Inject
  public ExtensionsChannelMutator(
      ClusterExtensionMetadataManager extensionMetadataManager) {
    this.extensionMetadataManager = extensionMetadataManager;
  }

  @Override
  public List<JsonPatchOperation> mutate(StackGresClusterReview review) {
    if (review.getRequest().getOperation() == Operation.CREATE
        || review.getRequest().getOperation() == Operation.UPDATE) {
      ImmutableList.Builder<JsonPatchOperation> operations = ImmutableList.builder();
      Optional.of(review)
          .map(StackGresClusterReview::getRequest)
          .map(AdmissionRequest<StackGresCluster>::getObject)
          .ifPresent(cluster -> {
            Optional.of(cluster)
                .map(StackGresCluster::getSpec)
                .map(StackGresClusterSpec::getPostgresExtensions)
                .stream()
                .flatMap(extensions -> Seq.seq(extensions).zipWithIndex())
                .forEach(Unchecked.consumer(extension -> {
                  final JsonPointer extensionVersionPointer =
                      CLUSTER_CONFIG_POINTER.append("postgresExtensions")
                      .append(extension.v2.intValue()).append("version");
                  final StackGresExtensionMetadata extensionMetadata =
                      extensionMetadataManager.getExtensionCandidateSameMajorBuild(
                          cluster, extension.v1);
                  final StackGresClusterInstalledExtension installedExtension =
                      ExtensionUtil.getInstalledExtension(extension.v1, extensionMetadata);
                  final TextNode extensionVersion = new TextNode(installedExtension.getVersion());
                  if (extension.v1.getVersion() == null) {
                    operations.add(new AddOperation(extensionVersionPointer, extensionVersion));
                  } else if (!installedExtension.getVersion().equals(extension.v1.getVersion())) {
                    operations.add(new ReplaceOperation(extensionVersionPointer, extensionVersion));
                  }
                }));
          });
      return operations.build();
    }

    return ImmutableList.of();
  }

}
