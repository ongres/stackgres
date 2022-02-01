/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.ResourceFinder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

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
    /*
     * TODO the changes of the SGCluster to support SGObjectResource haven't been done, therefore
     *  we should return to this, once the changes have been made.
     */
    return false;
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ObjectStorageDto.class))})
      })
  @Override
  public ObjectStorageDto get(String namespace, String name) {
    return super.get(namespace, name);
  }
}
