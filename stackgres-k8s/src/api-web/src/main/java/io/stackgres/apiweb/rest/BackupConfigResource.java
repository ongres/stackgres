/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import static io.stackgres.apiweb.rest.BackupConfigResourceUtil.extractSecretInfo;
import static io.stackgres.apiweb.rest.BackupConfigResourceUtil.secretName;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.quarkus.security.Authenticated;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigDto;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.common.resource.ResourceWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.jooq.lambda.Seq;

@Path("sgbackupconfigs")
@RequestScoped
@Authenticated
public class BackupConfigResource extends
    AbstractRestServiceDependency<BackupConfigDto, StackGresBackupConfig> {

  @Inject
  ResourceFinder<Secret> secretFinder;

  @Inject
  ResourceWriter<Secret> secretWriter;

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
              content = { @Content(
                  mediaType = "application/json",
                  array = @ArraySchema(schema = @Schema(implementation = BackupConfigDto.class))) })
      })
  @Override
  public List<BackupConfigDto> list() {
    return Seq.seq(super.list())
        .toList();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void create(BackupConfigDto resource) {
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
  public void delete(BackupConfigDto resource) {
    setSecretKeySelectors(resource);
    super.delete(resource);
    deleteSecret(resource);
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  public void update(BackupConfigDto resource) {
    setSecretKeySelectors(resource);
    createOrUpdateSecret(resource);
    super.update(resource);
  }

  private void setSecretKeySelectors(BackupConfigDto resource) {
    final String name = secretName(resource);
    extractSecretInfo(resource)
        .filter(t -> t.v2.v1 != null)
        .forEach(t -> t.v2.v4.accept(new SecretKeySelector(t.v1, name)));
  }

  private void createOrUpdateSecret(BackupConfigDto resource) {
    final ImmutableMap<String, String> secrets = extractSecretInfo(resource)
        .filter(t -> t.v2.v1 != null)
        .collect(ImmutableMap.toImmutableMap(t -> t.v1, t -> t.v2.v1));
    final String namespace = resource.getMetadata().getNamespace();
    final String name = secretName(resource);
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

  private void deleteSecret(BackupConfigDto resource) {
    final String namespace = resource.getMetadata().getNamespace();
    final String name = secretName(resource);
    secretFinder.findByNameAndNamespace(name, namespace)
        .ifPresent(secretWriter::delete);
  }

}
