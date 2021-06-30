/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
import org.jooq.lambda.tuple.Tuple;

@Path("")
@RequestScoped
@Authenticated
public class BackupConfigResource extends
    AbstractDependencyRestService<BackupConfigDto, StackGresBackupConfig> {

  @Inject
  ResourceFinder<Secret> secretFinder;

  @Inject
  ResourceWriter<Secret> secretWriter;

  private final BackupConfigResourceUtil util = new BackupConfigResourceUtil();

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
  @Path("sgbackupconfigs")
  public List<BackupConfigDto> list() {
    return Seq.seq(super.list())
        .map(this::setSecrets)
        .toList();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK",
              content = { @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = BackupConfigDto.class)) })
      })
  @Override
  @Path("{namespace:[a-z0-9]([-a-z0-9]*[a-z0-9])?}/sgbackupconfigs/{name}")
  public BackupConfigDto get(String namespace, String name) {
    return Optional.of(super.get(namespace, name))
        .map(this::setSecrets)
        .get();
  }

  @Operation(
      responses = {
          @ApiResponse(responseCode = "200", description = "OK")
      })
  @Override
  @Path("sgbackupconfigs")
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
  @Path("sgbackupconfigs")
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
  @Path("sgbackupconfigs")
  public void update(BackupConfigDto resource) {
    setSecretKeySelectors(resource);
    createOrUpdateSecret(resource);
    super.update(resource);
  }

  private BackupConfigDto setSecrets(BackupConfigDto resource) {
    final String namespace = resource.getMetadata().getNamespace();
    util.extractSecretInfo(resource)
        .filter(t -> t.v2.v3 != null)
        .grouped(t -> t.v2.v3.getName())
        .flatMap(t -> {
          Optional<Map<String, String>> secrets = secretFinder
              .findByNameAndNamespace(t.v1, namespace)
              .map(Secret::getData);
          return secrets
              .map(s -> t.v2.map(tt -> Tuple.tuple(
                  ResourceUtil.decodeSecret(s.get(tt.v2.v3.getKey())), tt.v2.v2)))
              .orElse(Seq.empty());
        })
        .forEach(t -> t.v2.accept(t.v1));
    return resource;
  }

  private void setSecretKeySelectors(BackupConfigDto resource) {
    final String name = util.secretName(resource);
    util.extractSecretInfo(resource)
        .filter(t -> t.v2.v1 != null)
        .forEach(t -> t.v2.v4.accept(new SecretKeySelector(t.v1, name)));
  }

  private void createOrUpdateSecret(BackupConfigDto resource) {
    final ImmutableMap<String, String> secrets = util.extractSecretInfo(resource)
        .filter(t -> t.v2.v1 != null)
        .collect(ImmutableMap.toImmutableMap(t -> t.v1, t -> t.v2.v1));
    final String namespace = resource.getMetadata().getNamespace();
    final String name = util.secretName(resource);
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
    final String name = util.secretName(resource);
    secretFinder.findByNameAndNamespace(name, namespace)
        .ifPresent(secretWriter::delete);
  }

}
