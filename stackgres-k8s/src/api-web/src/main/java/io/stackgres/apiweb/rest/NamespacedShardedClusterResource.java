/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.cluster.ClusterDto;
import io.stackgres.apiweb.dto.shardedcluster.ShardedClusterDto;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgshardedclusters")
@RequestScoped
@Authenticated
public class NamespacedShardedClusterResource
    extends AbstractNamespacedRestServiceDependency<ShardedClusterDto, StackGresShardedCluster> {

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

  @Override
  public boolean belongsToCluster(StackGresShardedCluster resource, StackGresCluster cluster) {
    String shardedNamespace = resource.getMetadata().getNamespace();
    String clusterNamespace = cluster.getMetadata().getNamespace();

    if (!Objects.equals(shardedNamespace, clusterNamespace)) {
      return false;
    }

    String shardedName = resource.getMetadata().getName();
    return Optional.of(cluster)
        .map(StackGresCluster::getMetadata)
        .map(ObjectMeta::getOwnerReferences)
        .stream()
        .flatMap(List::stream)
        .anyMatch(ownerReference -> Objects.equals(shardedName, ownerReference.getName())
            && Objects.equals(StackGresShardedCluster.KIND, ownerReference.getKind()));
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = ClusterDto.class))})
      })
  @Override
  public ShardedClusterDto get(String namespace, String name) {
    return Optional.of(super.get(namespace, name))
        .map(shardedClusterResource::setScripts)
        .map(shardedClusterResource::setConfigMaps)
        .map(shardedClusterResource::setInfo)
        .orElseThrow(NotFoundException::new);
  }

}
