/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.cluster;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.rest.AbstractNamespacedRestService;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgclusters")
@RequestScoped
@Authenticated
public class NamespacedClusterResource
    extends AbstractNamespacedRestService<ClusterDto, StackGresCluster> {

  private final ClusterResource clusterResource;

  private final CustomResourceFinder<ClusterDto> clusterFinder;

  @Inject
  public NamespacedClusterResource(CustomResourceFinder<ClusterDto> clusterFinder,
      ClusterResource clusterResource) {
    this.clusterResource = clusterResource;
    this.clusterFinder = clusterFinder;
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ClusterDto.class))})
  @Tag(name = "sgcluster")
  @Operation(summary = "Get a sgcluster", description = """
      Get a sgcluster and read values from the referenced secrets and configmaps.

      ### RBAC permissions required

      * sgclusters get
      * pod list
      * services list
      * secrets get
      * configmaps get
      """)
  @Override
  public ClusterDto get(String namespace, String name) {
    return clusterFinder.findByNameAndNamespace(name, namespace)
        .map(clusterResource::setScripts)
        .map(clusterResource::setConfigMaps)
        .map(clusterResource::setInfo)
        .orElseThrow(NotFoundException::new);
  }

}
