/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgobjectstorage;

import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.apiweb.exception.ErrorResponse;
import io.stackgres.apiweb.rest.AbstractNamespacedRestServiceDependency;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.ResourceFinder;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgobjectstorages")
@RequestScoped
@Authenticated
@Tag(name = "sgobjectstorage")
@APIResponse(responseCode = "400", description = "Bad Request",
    content = {@Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "401", description = "Unauthorized",
    content = {@Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "403", description = "Forbidden",
    content = {@Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ErrorResponse.class))})
@APIResponse(responseCode = "500", description = "Internal Server Error",
    content = {@Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ErrorResponse.class))})
public class NamespacedObjectStorageResource
    extends AbstractNamespacedRestServiceDependency<ObjectStorageDto, StackGresObjectStorage> {

  @Inject
  public ObjectStorageResource backupConfigResource;

  @Inject
  public ResourceFinder<Secret> secretFinder;

  @Override
  public boolean belongsToCluster(StackGresObjectStorage resource, StackGresCluster cluster) {
    String storageNamespace = resource.getMetadata().getNamespace();
    String clusterNamespace = cluster.getMetadata().getNamespace();

    if (!Objects.equals(storageNamespace, clusterNamespace)) {
      return false;
    }

    String objectStorageName = resource.getMetadata().getName();
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getBackups)
        .map(backupConfigurations -> backupConfigurations.stream()
            .map(StackGresClusterBackupConfiguration::getSgObjectStorage)
            .anyMatch(ref -> Objects.equals(ref, objectStorageName)))
        .orElse(false);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ObjectStorageDto.class))})
  @Operation(summary = "Get a sgobjectstorages", description = """
      Get a sgobjectstorages and read values from the referenced secrets.

      ### RBAC permissions required

      * sgobjectstorages get
      * secrets get
      """)
  @Override
  public ObjectStorageDto get(String namespace, String name) {
    return super.get(namespace, name);
  }
}
