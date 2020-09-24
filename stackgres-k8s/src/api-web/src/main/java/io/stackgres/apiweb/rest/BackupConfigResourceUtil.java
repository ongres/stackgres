/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigDto;
import io.stackgres.apiweb.dto.backupconfig.BackupConfigSpec;
import io.stackgres.apiweb.dto.storages.AwsCredentials;
import io.stackgres.apiweb.dto.storages.AwsS3CompatibleStorage;
import io.stackgres.apiweb.dto.storages.AwsS3Storage;
import io.stackgres.apiweb.dto.storages.AwsSecretKeySelector;
import io.stackgres.apiweb.dto.storages.AzureBlobSecretKeySelector;
import io.stackgres.apiweb.dto.storages.AzureBlobStorage;
import io.stackgres.apiweb.dto.storages.AzureBlobStorageCredentials;
import io.stackgres.apiweb.dto.storages.BackupStorage;
import io.stackgres.apiweb.dto.storages.GoogleCloudCredentials;
import io.stackgres.apiweb.dto.storages.GoogleCloudSecretKeySelector;
import io.stackgres.apiweb.dto.storages.GoogleCloudStorage;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple4;

public class BackupConfigResourceUtil {

  public static final String SECRETS_SUFFIX = "-secrets";

  public static final String S3_ACCESS_KEY = "s3-accessKey";
  public static final String S3_SECRET_KEY = "s3-secretKey";
  public static final String S3COMPATIBLE_ACCESS_KEY = "s3compatible-accessKey";
  public static final String S3COMPATIBLE_SECRET_KEY = "s3compatible-secretKey";
  public static final String GCS_SERVICE_ACCOUNT_JSON_KEY = "gcs-service-account-json-key";
  public static final String AZURE_ACCOUNT = "azure-account";
  public static final String AZURE_ACCESS_KEY = "azure-accessKey";

  BackupConfigResourceUtil() {
  }

  String secretName(BackupConfigDto resource) {
    return resource.getMetadata().getName() + SECRETS_SUFFIX;
  }

  Seq<Tuple2<String, Tuple4<String, Consumer<String>,
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
                (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                  .map(AwsSecretKeySelector::getAccessKeyId)
                  .orElse(null),
                (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                    .orElseGet(AwsSecretKeySelector::new))
                  .peek(c::setSecretKeySelectors)
                  .forEach(ss -> ss.setAccessKeyId(s))))),
        Tuple.tuple(S3_SECRET_KEY, storage.map(BackupStorage::getS3)
            .map(AwsS3Storage::getCredentials)
            .map(secretSelectorGetterAndSetter(
                AwsCredentials::getSecretKey,
                AwsCredentials::setSecretKey,
                (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                  .map(AwsSecretKeySelector::getSecretAccessKey)
                  .orElse(null),
                (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                    .orElseGet(AwsSecretKeySelector::new))
                  .peek(c::setSecretKeySelectors)
                  .forEach(ss -> ss.setSecretAccessKey(s))))),
        Tuple.tuple(S3COMPATIBLE_ACCESS_KEY, storage.map(BackupStorage::getS3Compatible)
            .map(AwsS3CompatibleStorage::getCredentials)
            .map(secretSelectorGetterAndSetter(
                AwsCredentials::getAccessKey,
                AwsCredentials::setAccessKey,
                (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                  .map(AwsSecretKeySelector::getAccessKeyId)
                  .orElse(null),
                (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                    .orElseGet(AwsSecretKeySelector::new))
                  .peek(c::setSecretKeySelectors)
                  .forEach(ss -> ss.setAccessKeyId(s))))),
        Tuple.tuple(S3COMPATIBLE_SECRET_KEY, storage.map(BackupStorage::getS3Compatible)
            .map(AwsS3CompatibleStorage::getCredentials)
            .map(secretSelectorGetterAndSetter(
                AwsCredentials::getSecretKey,
                AwsCredentials::setSecretKey,
                (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                  .map(AwsSecretKeySelector::getSecretAccessKey)
                  .orElse(null),
                (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                    .orElseGet(AwsSecretKeySelector::new))
                  .peek(c::setSecretKeySelectors)
                  .forEach(ss -> ss.setSecretAccessKey(s))))),
        Tuple.tuple(GCS_SERVICE_ACCOUNT_JSON_KEY,
            storage.map(BackupStorage::getGcs)
            .map(GoogleCloudStorage::getCredentials)
            .map(secretSelectorGetterAndSetter(
                GoogleCloudCredentials::getServiceAccountJsonKey,
                GoogleCloudCredentials::setServiceAccountJsonKey,
                (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                  .map(GoogleCloudSecretKeySelector::getServiceAccountJsonKey)
                  .orElse(null),
                (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                    .orElseGet(GoogleCloudSecretKeySelector::new))
                  .peek(c::setSecretKeySelectors)
                  .forEach(ss -> ss.setServiceAccountJsonKey(s))))),
        Tuple.tuple(AZURE_ACCOUNT, storage.map(BackupStorage::getAzureBlob)
            .map(AzureBlobStorage::getCredentials)
            .map(secretSelectorGetterAndSetter(
                AzureBlobStorageCredentials::getAccount,
                AzureBlobStorageCredentials::setAccount,
                (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                  .map(AzureBlobSecretKeySelector::getAccount)
                  .orElse(null),
                (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                    .orElseGet(AzureBlobSecretKeySelector::new))
                  .peek(c::setSecretKeySelectors)
                  .forEach(ss -> ss.setAccount(s))))),
        Tuple.tuple(AZURE_ACCESS_KEY, storage.map(BackupStorage::getAzureBlob)
            .map(AzureBlobStorage::getCredentials)
            .map(secretSelectorGetterAndSetter(
                AzureBlobStorageCredentials::getAccessKey,
                AzureBlobStorageCredentials::setAccessKey,
                (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                  .map(AzureBlobSecretKeySelector::getAccessKey)
                  .orElse(null),
                (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                    .orElseGet(AzureBlobSecretKeySelector::new))
                  .peek(c::setSecretKeySelectors)
                  .forEach(ss -> ss.setAccessKey(s))))))
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
