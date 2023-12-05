/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.misc;

import java.util.List;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.rest.utils.CommonApiResponses;
import io.stackgres.common.resource.ResourceScanner;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("storageclasses")
@RequestScoped
@Authenticated
public class StorageClassResource {

  private ResourceScanner<StorageClass> storageClassScanner;

  @Inject
  public void setStorageClassScanner(ResourceScanner<StorageClass> storageClassScanner) {
    this.storageClassScanner = storageClassScanner;
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.STRING))})
  @Tag(name = "misc")
  @Operation(summary = "List storageclasss", description = """
      List storageclasss.

      ### RBAC permissions required

      * storageclasss list
      """)
  @CommonApiResponses
  @GET
  public List<String> get() {
    return storageClassScanner.findResources().stream()
        .map(sc -> sc.getMetadata().getName())
        .toList();
  }

}
