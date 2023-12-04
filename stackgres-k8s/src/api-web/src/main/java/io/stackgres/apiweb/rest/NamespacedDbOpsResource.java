/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.dbops.DbOpsDto;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgdbops")
@RequestScoped
@Authenticated
public class NamespacedDbOpsResource
    extends AbstractNamespacedRestService<DbOpsDto, StackGresDbOps> {

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = DbOpsDto.class))})
  @Override
  public DbOpsDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

}
