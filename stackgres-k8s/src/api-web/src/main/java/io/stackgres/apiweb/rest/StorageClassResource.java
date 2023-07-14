/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.common.resource.ResourceScanner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("storageclasses")
@RequestScoped
@Authenticated
public class StorageClassResource {

  private ResourceScanner<StorageClass> storageClassScanner;

  @Inject
  public void setStorageClassScanner(ResourceScanner<StorageClass> storageClassScanner) {
    this.storageClassScanner = storageClassScanner;
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(type = "string")))})
      })
  @CommonApiResponses
  @GET
  public List<String> get() {
    return storageClassScanner.findResources().stream()
        .map(sc -> sc.getMetadata().getName())
        .collect(ImmutableList.toImmutableList());
  }

}
