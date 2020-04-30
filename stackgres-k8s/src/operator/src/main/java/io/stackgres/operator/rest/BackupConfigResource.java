/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.resource.ResourceFinder;
import io.stackgres.operator.resource.ResourceWriter;
import io.stackgres.operator.rest.dto.SecretKeySelector;
import io.stackgres.operator.rest.dto.backupconfig.BackupConfigDto;
import io.stackgres.operator.rest.transformer.ResourceTransformer;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;

@Path("/stackgres/sgbackupconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BackupConfigResource extends
    AbstractRestService<BackupConfigDto, StackGresBackupConfig> {

  private final CustomResourceFinder<StackGresBackupConfig> finder;
  private final ResourceFinder<Secret> secretFinder;
  private final ResourceWriter<Secret> secretWriter;
  private final BackupConfigResourceUtil util = new BackupConfigResourceUtil();

  @Inject
  public BackupConfigResource(
      CustomResourceScanner<StackGresBackupConfig> scanner,
      CustomResourceFinder<StackGresBackupConfig> finder,
      CustomResourceScheduler<StackGresBackupConfig> scheduler,
      ResourceTransformer<BackupConfigDto, StackGresBackupConfig> transformer,
      ResourceFinder<Secret> secretFinder,
      ResourceWriter<Secret> secretWriter) {
    super(scanner, finder, scheduler, transformer);
    this.finder = finder;
    this.secretFinder = secretFinder;
    this.secretWriter = secretWriter;
  }

  public BackupConfigResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.finder = null;
    this.secretFinder = null;
    this.secretWriter = null;
  }

  @RolesAllowed(RestAuthenticationRoles.ADMIN)
  @Override
  public List<BackupConfigDto> list() {
    return Seq.seq(super.list())
        .map(this::setSecrets)
        .toList();
  }

  @RolesAllowed(RestAuthenticationRoles.ADMIN)
  @Override
  public BackupConfigDto get(String namespace, String name) {
    return Optional.of(super.get(namespace, name))
        .map(this::setSecrets)
        .get();
  }

  @RolesAllowed(RestAuthenticationRoles.ADMIN)
  @Override
  public void create(BackupConfigDto resource) {
    setSecretKeySelectors(resource);
    createOrUpdateSecret(resource);
    super.create(resource);
    createOrUpdateSecret(resource);
  }

  @RolesAllowed(RestAuthenticationRoles.ADMIN)
  @Override
  public void delete(BackupConfigDto resource) {
    setSecretKeySelectors(resource);
    super.delete(resource);
    deleteSecret(resource);
  }

  @RolesAllowed(RestAuthenticationRoles.ADMIN)
  @Override
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
                  ResourceUtil.dencodeSecret(s.get(tt.v2.v3.getKey())), tt.v2.v2)))
              .orElse(Seq.empty());
        })
        .forEach(t -> t.v2.accept(t.v1));
    return resource;
  }

  private void setSecretKeySelectors(BackupConfigDto resource) {
    final String name = util.secretName(resource);
    util.extractSecretInfo(resource)
        .filter(t -> t.v2.v1 != null)
        .forEach(t -> t.v2.v4.accept(SecretKeySelector.create(name, t.v1)));
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
