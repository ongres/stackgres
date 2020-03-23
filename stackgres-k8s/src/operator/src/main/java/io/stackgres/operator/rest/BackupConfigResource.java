/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.operator.common.ArcUtil;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.resource.CustomResourceFinder;
import io.stackgres.operator.resource.CustomResourceScanner;
import io.stackgres.operator.resource.CustomResourceScheduler;
import io.stackgres.operator.resource.ResourceFinder;
import io.stackgres.operator.resource.ResourceWriter;
import io.stackgres.operator.rest.dto.SecretKeySelector;
import io.stackgres.operator.rest.dto.backupconfig.BackupConfigDto;
import io.stackgres.operator.rest.dto.backupconfig.BackupConfigSpec;
import io.stackgres.operator.rest.dto.storages.AwsCredentials;
import io.stackgres.operator.rest.dto.storages.AwsS3CompatibleStorage;
import io.stackgres.operator.rest.dto.storages.AwsS3Storage;
import io.stackgres.operator.rest.dto.storages.AzureBlobStorage;
import io.stackgres.operator.rest.dto.storages.AzureBlobStorageCredentials;
import io.stackgres.operator.rest.dto.storages.BackupStorage;
import io.stackgres.operator.rest.dto.storages.GoogleCloudCredentials;
import io.stackgres.operator.rest.dto.storages.GoogleCloudStorage;
import io.stackgres.operator.rest.transformer.ResourceTransformer;
import io.stackgres.operatorframework.resource.ResourceUtil;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple4;

@Path("/stackgres/backupconfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BackupConfigResource extends
    AbstractRestService<BackupConfigDto, StackGresBackupConfig> {

  public static final String SECRETS_SUFFIX = "-secrets";

  public static final String S3_ACCESS_KEY = "s3-accessKey";
  public static final String S3_SECRET_KEY = "s3-secretKey";
  public static final String S3COMPATIBLE_ACCESS_KEY = "s3compatible-accessKey";
  public static final String S3COMPATIBLE_SECRET_KEY = "s3compatible-secretKey";
  public static final String GCS_SERVICE_ACCOUNT_JSON_KEY = "gcs-service-account-json-key";
  public static final String AZURE_ACCOUNT = "azure-account";
  public static final String AZURE_ACCESS_KEY = "azure-accessKey";

  private final ResourceFinder<Secret> secretFinder;
  private final ResourceWriter<Secret> secretWriter;

  @Inject
  public BackupConfigResource(
      CustomResourceScanner<StackGresBackupConfig> scanner,
      CustomResourceFinder<StackGresBackupConfig> finder,
      CustomResourceScheduler<StackGresBackupConfig> scheduler,
      ResourceTransformer<BackupConfigDto, StackGresBackupConfig> transformer,
      ResourceFinder<Secret> secretFinder,
      ResourceWriter<Secret> secretWriter) {
    super(scanner, finder, scheduler, transformer);
    this.secretFinder = secretFinder;
    this.secretWriter = secretWriter;
  }

  public BackupConfigResource() {
    super(null, null, null, null);
    ArcUtil.checkPublicNoArgsConstructorIsCalledFromArc();
    this.secretFinder = null;
    this.secretWriter = null;
  }

  @Override
  public List<BackupConfigDto> list() {
    return Seq.seq(super.list())
        .map(this::setSecrets)
        .toList();
  }

  @Override
  public BackupConfigDto get(String namespace, String name) {
    return Optional.of(super.get(namespace, name))
        .map(this::setSecrets)
        .get();
  }

  @Override
  public void create(BackupConfigDto resource) {
    setSecretKeySelectors(resource);
    createOrUpdateSecret(resource);
    super.create(resource);
  }

  @Override
  public void delete(BackupConfigDto resource) {
    setSecretKeySelectors(resource);
    super.delete(resource);
    deleteSecret(resource);
  }

  @Override
  public void update(BackupConfigDto resource) {
    setSecretKeySelectors(resource);
    createOrUpdateSecret(resource);
    super.update(resource);
  }

  private BackupConfigDto setSecrets(BackupConfigDto resource) {
    final String namespace = resource.getMetadata().getNamespace();
    extractSecretInfo(resource)
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
    final String name = secretName(resource);
    extractSecretInfo(resource)
        .filter(t -> t.v2.v1 != null)
        .forEach(t -> t.v2.v4.accept(SecretKeySelector.create(name, t.v1)));
  }

  private void createOrUpdateSecret(BackupConfigDto resource) {
    final ImmutableMap<String, String> secrets = extractSecretInfo(resource)
        .filter(t -> t.v2.v1 != null)
        .collect(ImmutableMap.toImmutableMap(t -> t.v1, t -> t.v2.v1));
    final String namespace = resource.getMetadata().getNamespace();
    final String name = secretName(resource);
    secretFinder.findByNameAndNamespace(name, namespace)
        .map(secret -> {
          secret.setData(secrets);
          secretWriter.update(secret);
          return secret;
        })
        .orElseGet(() -> {
          secretWriter.create(new SecretBuilder()
              .withNewMetadata()
              .withNamespace(namespace)
              .withName(name)
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

  private String secretName(BackupConfigDto resource) {
    return resource.getMetadata().getName() + SECRETS_SUFFIX;
  }

  private Seq<Tuple2<String, Tuple4<String, Consumer<String>,
      SecretKeySelector, Consumer<SecretKeySelector>>>>
      extractSecretInfo(BackupConfigDto resource) {
    Optional<BackupStorage> storage = Optional.of(resource)
        .map(BackupConfigDto::getSpec)
        .map(BackupConfigSpec::getStorage);
    return Seq.of(
        Tuple.tuple(S3_ACCESS_KEY, storage.map(BackupStorage::getS3)
            .map(AwsS3Storage::getCredentials)
            .map(secretSelectorGetterAndSetter(
                AwsCredentials::getAccessKey,
                AwsCredentials::setAccessKey,
                AwsCredentials::getAccessKeySelector,
                AwsCredentials::setAccessKeySelector))),
        Tuple.tuple(S3_SECRET_KEY, storage.map(BackupStorage::getS3)
            .map(AwsS3Storage::getCredentials)
            .map(secretSelectorGetterAndSetter(
                AwsCredentials::getSecretKey,
                AwsCredentials::setSecretKey,
                AwsCredentials::getSecretKeySelector,
                AwsCredentials::setSecretKeySelector))),
        Tuple.tuple(S3COMPATIBLE_ACCESS_KEY, storage.map(BackupStorage::getS3Compatible)
            .map(AwsS3CompatibleStorage::getCredentials)
            .map(secretSelectorGetterAndSetter(
                AwsCredentials::getAccessKey,
                AwsCredentials::setAccessKey,
                AwsCredentials::getAccessKeySelector,
                AwsCredentials::setAccessKeySelector))),
        Tuple.tuple(S3COMPATIBLE_SECRET_KEY, storage.map(BackupStorage::getS3Compatible)
            .map(AwsS3CompatibleStorage::getCredentials)
            .map(secretSelectorGetterAndSetter(
                AwsCredentials::getSecretKey,
                AwsCredentials::setSecretKey,
                AwsCredentials::getSecretKeySelector,
                AwsCredentials::setSecretKeySelector))),
        Tuple.tuple(GCS_SERVICE_ACCOUNT_JSON_KEY,
            storage.map(BackupStorage::getGcs)
                .map(GoogleCloudStorage::getCredentials)
                .map(secretSelectorGetterAndSetter(
                    GoogleCloudCredentials::getServiceAccountJsonKey,
                    GoogleCloudCredentials::setServiceAccountJsonKey,
                    GoogleCloudCredentials::getServiceAccountJsonKeySelector,
                    GoogleCloudCredentials::setServiceAccountJsonKeySelector))),
        Tuple.tuple(AZURE_ACCOUNT, storage.map(BackupStorage::getAzureblob)
            .map(AzureBlobStorage::getCredentials)
            .map(secretSelectorGetterAndSetter(
                AzureBlobStorageCredentials::getAccount,
                AzureBlobStorageCredentials::setAccount,
                AzureBlobStorageCredentials::getAccountSelector,
                AzureBlobStorageCredentials::setAccountSelector))),
        Tuple.tuple(AZURE_ACCESS_KEY, storage.map(BackupStorage::getAzureblob)
            .map(AzureBlobStorage::getCredentials)
            .map(secretSelectorGetterAndSetter(
                AzureBlobStorageCredentials::getAccessKey,
                AzureBlobStorageCredentials::setAccessKey,
                AzureBlobStorageCredentials::getAccessKeySelector,
                AzureBlobStorageCredentials::setAccessKeySelector))))
        .filter(t -> t.v2.isPresent())
        .map(t -> Tuple.tuple(t.v1, t.v2.get()));
  }

  private <T> Function<T, Tuple4<String, Consumer<String>, SecretKeySelector,
      Consumer<SecretKeySelector>>> secretSelectorGetterAndSetter(
          Function<T, String> secretGetter,
          BiConsumer<T, String> secretSetter,
          Function<T, SecretKeySelector> secretKeySelectorGetter,
          BiConsumer<T, SecretKeySelector> secretKeySelectorSetter) {
    return object -> Tuple.<String, Consumer<String>,
          SecretKeySelector, Consumer<SecretKeySelector>>tuple(
              secretGetter.apply(object),
              secret -> secretSetter.accept(object, secret),
              secretKeySelectorGetter.apply(object),
              secretKeySelector -> secretKeySelectorSetter.accept(
                  object, secretKeySelector));
  }

}
