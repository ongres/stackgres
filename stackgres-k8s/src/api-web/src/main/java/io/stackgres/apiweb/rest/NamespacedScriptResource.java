/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgscripts")
@RequestScoped
@Authenticated
public class NamespacedScriptResource
    extends AbstractNamespacedRestService<ScriptDto, StackGresScript> {

  private final ScriptResource scriptResource;

  @Inject
  public NamespacedScriptResource(ScriptResource scriptResource) {
    this.scriptResource = scriptResource;
  }

  public NamespacedScriptResource() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
    this.scriptResource = null;
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ScriptDto.class))})
      })
  @Override
  public ScriptDto get(String namespace, String name) {
    return Optional.of(super.get(namespace, name))
        .map(scriptResource::setConfigMaps)
        .orElseThrow(NotFoundException::new);
  }

}
