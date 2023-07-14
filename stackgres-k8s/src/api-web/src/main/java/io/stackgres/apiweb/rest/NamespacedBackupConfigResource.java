/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Objects;

import io.fabric8.kubernetes.api.model.Secret;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigDto;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path("namespaces/{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgbackupconfigs")
@RequestScoped
@Authenticated
public class NamespacedBackupConfigResource
    extends AbstractNamespacedRestServiceDependency<BackupConfigDto, StackGresBackupConfig> {

  @Inject
  BackupConfigResource backupConfigResource;

  @Inject
  ResourceFinder<Secret> secretFinder;

  @Override
  public boolean belongsToCluster(StackGresBackupConfig resource, StackGresCluster cluster) {
    return cluster.getMetadata().getNamespace().equals(
        resource.getMetadata().getNamespace())
        && Objects.equals(cluster.getSpec().getConfiguration().getBackupConfig(),
            resource.getMetadata().getName());
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = BackupConfigDto.class))})
      })
  @Override
  public BackupConfigDto get(String namespace, String name) {
    return super.get(namespace, name);
  }

}
