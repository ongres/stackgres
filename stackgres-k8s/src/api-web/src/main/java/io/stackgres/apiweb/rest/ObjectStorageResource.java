/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.resource.ResourceUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.jetbrains.annotations.NotNull;

@Path("sgobjectstorages")
@RequestScoped
@Authenticated
public class ObjectStorageResource extends AbstractRestServiceDependency<ObjectStorageDto,
    StackGresObjectStorage> {

  @Inject
  ResourceFinder<Secret> secretFinder;

  @Inject
  ResourceWriter<Secret> secretWriter;

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = {@Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(
                      implementation = ObjectStorageDto.class
                  )))})
      })
  @Override
  public @NotNull List<ObjectStorageDto> list() {
    return super.list();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void create(@NotNull ObjectStorageDto resource) {
    setSecretKeySelectors(resource);
    createOrUpdateSecret(resource);
    super.create(resource);
    createOrUpdateSecret(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void update(@NotNull ObjectStorageDto resource) {
    setSecretKeySelectors(resource);
    createOrUpdateSecret(resource);
    super.update(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void delete(@NotNull ObjectStorageDto resource) {
    setSecretKeySelectors(resource);
    super.delete(resource);
    deleteSecret(resource);
  }

  @Override
  public boolean belongsToCluster(StackGresObjectStorage resource, StackGresCluster cluster) {
    String storageNamespace = resource.getMetadata().getNamespace();
    String clusterNamespace = cluster.getMetadata().getNamespace();

    if (!Objects.equals(storageNamespace, clusterNamespace)) {
      return false;
    }

    String storageName = resource.getMetadata().getName();
    return Optional.of(cluster)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getConfiguration)
        .map(StackGresClusterConfiguration::getBackups)
        .map(backupConfigurations -> backupConfigurations.stream()
            .map(StackGresClusterBackupConfiguration::getObjectStorage)
            .anyMatch(ref -> Objects.equals(ref, storageName))
        )
        .orElse(false);
  }

  private void setSecretKeySelectors(ObjectStorageDto resource) {
    final String name = BackupStorageDtoUtil.secretName(resource);
    BackupStorageDtoUtil.extractSecretInfo(resource.getSpec())
        .filter(t -> t.v2.v1 != null)
        .forEach(t -> t.v2.v4.accept(new SecretKeySelector(t.v1, name)));
  }

  private void createOrUpdateSecret(ObjectStorageDto resource) {
    final ImmutableMap<String, String> secrets = BackupStorageDtoUtil
        .extractSecretInfo(resource.getSpec())
        .filter(t -> t.v2.v1 != null)
        .collect(ImmutableMap.toImmutableMap(t -> t.v1, t -> t.v2.v1));
    final String namespace = resource.getMetadata().getNamespace();
    final String name = BackupStorageDtoUtil.secretName(resource);
    secretFinder.findByNameAndNamespace(name, namespace)
        .map(secret -> {
          secret.setStringData(secrets);
          secretWriter.update(secret);
          return secret;
        })
        .orElseGet(() -> {
          secretWriter.create(new SecretBuilder()
              .withNewMetadata()
              .withNamespace(namespace)
              .withName(name)
              .withOwnerReferences(finder.findByNameAndNamespace(
                      resource.getMetadata().getName(), resource.getMetadata().getNamespace())
                  .map(ResourceUtil::getOwnerReference)
                  .map(ImmutableList::of)
                  .orElse(ImmutableList.of()))
              .endMetadata()
              .withStringData(secrets)
              .build());
          return null;
        });
  }

  private void deleteSecret(ObjectStorageDto resource) {
    final String namespace = resource.getMetadata().getNamespace();
    final String name = BackupStorageDtoUtil.secretName(resource);
    secretFinder.findByNameAndNamespace(name, namespace)
        .ifPresent(secretWriter::delete);
  }

  @Override
  protected void updateSpec(StackGresObjectStorage resourceToUpdate,
      StackGresObjectStorage resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
