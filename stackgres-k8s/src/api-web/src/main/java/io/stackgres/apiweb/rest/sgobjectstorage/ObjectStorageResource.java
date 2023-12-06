/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest.sgobjectstorage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.objectstorage.ObjectStorageDto;
import io.stackgres.apiweb.rest.AbstractCustomResourceServiceDependency;
import io.stackgres.apiweb.rest.BackupStorageDtoUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfigurations;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("sgobjectstorages")
@RequestScoped
@Authenticated
public class ObjectStorageResource
    extends AbstractCustomResourceServiceDependency<ObjectStorageDto, StackGresObjectStorage> {

  @Inject
  public  ResourceFinder<Secret> secretFinder;

  @Inject
  public  ResourceWriter<Secret> secretWriter;

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(type = SchemaType.ARRAY, implementation = ObjectStorageDto.class))})
  @Tag(name = "sgobjectstorage")
  @Operation(summary = "List sgobjectstorages", description = """
      List sgobjectstorages and read values from the referenced secrets.

      ### RBAC permissions required

      * sgobjectstorages list
      * secrets get
      """)
  @Override
  public @Nonnull List<ObjectStorageDto> list() {
    return super.list();
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ObjectStorageDto.class))})
  @Tag(name = "sgobjectstorage")
  @Operation(summary = "Create a sgobjectstorages", description = """
      Create a sgobjectstorages.
       If values are provided referenced secrets are created/patched or a secret named as
       the sgbackupconfig with `-secrets` suffix is created/patched if no secret is
       referenced.

      ### RBAC permissions required

      * sgobjectstorages create
      * secrets get, create, patch
      """)
  @Override
  public ObjectStorageDto create(@Nonnull ObjectStorageDto resource, @Nullable Boolean dryRun) {
    setSecretKeySelectors(resource);
    if (!Optional.ofNullable(dryRun).orElse(false)) {
      createOrUpdateSecret(resource);
    }
    return super.create(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK",
      content = {@Content(
          mediaType = "application/json",
          schema = @Schema(implementation = ObjectStorageDto.class))})
  @Tag(name = "sgobjectstorage")
  @Operation(summary = "Update a sgobjectstorages", description = """
      Update a sgobjectstorages.
       If values are provided referenced secrets are created/patched or a secret named as
       the sgobjectstorages with `-secrets` suffix is created/patched if no secret is
       referenced.

      ### RBAC permissions required

      * sgobjectstorages patch
      * secrets get, create, patch
      """)
  @Override
  public ObjectStorageDto update(@Nonnull ObjectStorageDto resource, @Nullable Boolean dryRun) {
    setSecretKeySelectors(resource);
    if (!Optional.ofNullable(dryRun).orElse(false)) {
      createOrUpdateSecret(resource);
    }
    return super.update(resource, dryRun);
  }

  @APIResponse(responseCode = "200", description = "OK")
  @Tag(name = "sgobjectstorage")
  @Operation(summary = "Delete a sgobjectstorages", description = """
      Delete a sgobjectstorages.
       If a secret named as the sgbackupconfig with `-secrets` suffix is found,
       it will be deleted also.

      ### RBAC permissions required

      * sgobjectstorages delete
      * secrets get, delete
      """)
  @Override
  public void delete(@Nonnull ObjectStorageDto resource, @Nullable Boolean dryRun) {
    setSecretKeySelectors(resource);
    super.delete(resource, dryRun);
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
        .map(StackGresClusterSpec::getConfigurations)
        .map(StackGresClusterConfigurations::getBackups)
        .map(backupConfigurations -> backupConfigurations.stream()
            .map(StackGresClusterBackupConfiguration::getSgObjectStorage)
            .anyMatch(ref -> Objects.equals(ref, storageName)))
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

  @Override
  protected void updateSpec(StackGresObjectStorage resourceToUpdate,
      StackGresObjectStorage resource) {
    resourceToUpdate.setSpec(resource.getSpec());
  }

}
