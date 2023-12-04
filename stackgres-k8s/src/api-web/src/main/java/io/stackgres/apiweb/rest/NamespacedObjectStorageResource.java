/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.ResourceFinder;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgobjectstorages")
@RequestScoped
@Authenticated
public class NamespacedObjectStorageResource
    extends AbstractNamespacedRestServiceDependency<ObjectStorageDto, StackGresObjectStorage> {

  @Inject
  ObjectStorageResource backupConfigResource;

  @Inject
  ResourceFinder<Secret> secretFinder;

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
  @Override
  public ObjectStorageDto get(String namespace, String name) {
    return super.get(namespace, name);
  }
}
