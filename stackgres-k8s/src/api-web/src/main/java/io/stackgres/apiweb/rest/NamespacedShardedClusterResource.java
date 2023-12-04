/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Optional;

import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterDto;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgshardedclusters")
@RequestScoped
@Authenticated
public class NamespacedShardedClusterResource
    extends AbstractNamespacedRestService<ShardedClusterDto, StackGresShardedCluster> {

  private final ShardedClusterResource shardedClusterResource;

  @Inject
  public NamespacedShardedClusterResource(
      ShardedClusterResource shardedClusterResource) {
    this.shardedClusterResource = shardedClusterResource;
  }

  public NamespacedShardedClusterResource() {
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.shardedClusterResource = null;
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ClusterDto.class))})
  @Override
  public ShardedClusterDto get(String namespace, String name) {
    return Optional.of(super.get(namespace, name))
        .map(shardedClusterResource::setScripts)
        .map(shardedClusterResource::setConfigMaps)
        .map(shardedClusterResource::setInfo)
        .orElseThrow(NotFoundException::new);
  }

}
