/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.script.ScriptDto;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedScriptEntry;
import io.stackgres.common.crd.sgcluster.StackGresClusterManagedSql;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgscript.StackGresScript;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgscripts")
@RequestScoped
@Authenticated
public class NamespacedScriptResource
    extends AbstractNamespacedRestServiceDependency<ScriptDto, StackGresScript> {

  private final ScriptResource scriptResource;

  @Inject
  public NamespacedScriptResource(ScriptResource scriptResource) {
    this.scriptResource = scriptResource;
  }

  @Override
  public boolean belongsToCluster(StackGresScript resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Optional.of(cluster.getSpec())
            .map(StackGresClusterSpec::getManagedSql)
            .map(StackGresClusterManagedSql::getScripts)
            .stream()
            .flatMap(List::stream)
            .map(StackGresClusterManagedScriptEntry::getSgScript)
            .anyMatch(sgScript -> Objects.equals(sgScript, resource.getMetadata().getName()));
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ScriptDto.class))})
  @Override
  public ScriptDto get(String namespace, String name) {
    return Optional.of(super.get(namespace, name))
        .map(scriptResource::setConfigMaps)
        .orElseThrow(NotFoundException::new);
  }

}
