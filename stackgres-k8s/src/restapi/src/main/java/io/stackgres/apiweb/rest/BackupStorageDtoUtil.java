/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.rest;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.dto.storages.AwsCredentialsDto;
import io.stackgres.apiweb.dto.storages.AwsS3CompatibleStorageDto;
import io.stackgres.apiweb.dto.storages.AwsS3StorageDto;
import io.stackgres.apiweb.dto.storages.AwsSecretKeySelector;
import io.stackgres.apiweb.dto.storages.AzureBlobSecretKeySelectorDto;
import io.stackgres.apiweb.dto.storages.AzureBlobStorageCredentialsDto;
import io.stackgres.apiweb.dto.storages.AzureBlobStorageDto;
import io.stackgres.apiweb.dto.storages.BackupStorageDto;
import io.stackgres.apiweb.dto.storages.GoogleCloudCredentialsDto;
import io.stackgres.apiweb.dto.storages.GoogleCloudSecretKeySelectorDto;
import io.stackgres.apiweb.dto.storages.GoogleCloudStorageDto;
import io.stackgres.common.crd.SecretKeySelector;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple4;

public interface BackupStorageDtoUtil {

  String SECRETS_SUFFIX = "-secrets";

  String S3_ACCESS_KEY = "s3-accessKey";
  String S3_SECRET_KEY = "s3-secretKey";
  String S3COMPATIBLE_ACCESS_KEY = "s3compatible-accessKey";
  String S3COMPATIBLE_SECRET_KEY = "s3compatible-secretKey";
  String GCS_SERVICE_ACCOUNT_JSON_KEY = "gcs-service-account-json-key";
  String AZURE_ACCOUNT = "azure-account";
  String AZURE_ACCESS_KEY = "azure-accessKey";

  static String secretName(ResourceDto resource) {
    return resource.getMetadata().getName() + SECRETS_SUFFIX;
  }

  static Seq<Tuple2<String, Tuple4<String, Consumer<String>,
      SecretKeySelector, Consumer<SecretKeySelector>>>> extractSecretInfo(
      BackupStorageDto storage) {

    var backupStorageOpt = Optional.ofNullable(storage);

    return Seq.of(
            Tuple.tuple(S3_ACCESS_KEY, backupStorageOpt.map(BackupStorageDto::getS3)
                .map(AwsS3StorageDto::getAwsCredentials)
                .map(secretSelectorGetterAndSetter(
                    AwsCredentialsDto::getAccessKeyId,
                    AwsCredentialsDto::setAccessKeyId,
                    (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                        .map(AwsSecretKeySelector::getAccessKeyId)
                        .orElse(null),
                    (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                            .orElseGet(AwsSecretKeySelector::new))
                        .peek(c::setSecretKeySelectors)
                        .forEach(ss -> ss.setAccessKeyId(s))))),
            Tuple.tuple(S3_SECRET_KEY, backupStorageOpt.map(BackupStorageDto::getS3)
                .map(AwsS3StorageDto::getAwsCredentials)
                .map(secretSelectorGetterAndSetter(
                    AwsCredentialsDto::getSecretAccessKey,
                    AwsCredentialsDto::setSecretAccessKey,
                    (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                        .map(AwsSecretKeySelector::getSecretAccessKey)
                        .orElse(null),
                    (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                            .orElseGet(AwsSecretKeySelector::new))
                        .peek(c::setSecretKeySelectors)
                        .forEach(ss -> ss.setSecretAccessKey(s))))),
            Tuple.tuple(S3COMPATIBLE_ACCESS_KEY, backupStorageOpt
                .map(BackupStorageDto::getS3Compatible)
                .map(AwsS3CompatibleStorageDto::getAwsCredentials)
                .map(secretSelectorGetterAndSetter(
                    AwsCredentialsDto::getAccessKeyId,
                    AwsCredentialsDto::setAccessKeyId,
                    (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                        .map(AwsSecretKeySelector::getAccessKeyId)
                        .orElse(null),
                    (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                            .orElseGet(AwsSecretKeySelector::new))
                        .peek(c::setSecretKeySelectors)
                        .forEach(ss -> ss.setAccessKeyId(s))))),
            Tuple.tuple(S3COMPATIBLE_SECRET_KEY, backupStorageOpt
                .map(BackupStorageDto::getS3Compatible)
                .map(AwsS3CompatibleStorageDto::getAwsCredentials)
                .map(secretSelectorGetterAndSetter(
                    AwsCredentialsDto::getSecretAccessKey,
                    AwsCredentialsDto::setSecretAccessKey,
                    (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                        .map(AwsSecretKeySelector::getSecretAccessKey)
                        .orElse(null),
                    (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                            .orElseGet(AwsSecretKeySelector::new))
                        .peek(c::setSecretKeySelectors)
                        .forEach(ss -> ss.setSecretAccessKey(s))))),
            Tuple.tuple(GCS_SERVICE_ACCOUNT_JSON_KEY,
                backupStorageOpt.map(BackupStorageDto::getGcs)
                    .map(GoogleCloudStorageDto::getGcpCredentials)
                    .map(secretSelectorGetterAndSetter(
                        GoogleCloudCredentialsDto::getServiceAccountJsonKey,
                        GoogleCloudCredentialsDto::setServiceAccountJsonKey,
                        (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                            .map(GoogleCloudSecretKeySelectorDto::getServiceAccountJsonKey)
                            .orElse(null),
                        (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                                .orElseGet(GoogleCloudSecretKeySelectorDto::new))
                            .peek(c::setSecretKeySelectors)
                            .forEach(ss -> ss.setServiceAccountJsonKey(s))))),
            Tuple.tuple(AZURE_ACCOUNT, backupStorageOpt.map(BackupStorageDto::getAzureBlob)
                .map(AzureBlobStorageDto::getAzureCredentials)
                .map(secretSelectorGetterAndSetter(
                    AzureBlobStorageCredentialsDto::getStorageAccount,
                    AzureBlobStorageCredentialsDto::setStorageAccount,
                    (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                        .map(AzureBlobSecretKeySelectorDto::getStorageAccount)
                        .orElse(null),
                    (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                            .orElseGet(AzureBlobSecretKeySelectorDto::new))
                        .peek(c::setSecretKeySelectors)
                        .forEach(ss -> ss.setStorageAccount(s))))),
            Tuple.tuple(AZURE_ACCESS_KEY, backupStorageOpt.map(BackupStorageDto::getAzureBlob)
                .map(AzureBlobStorageDto::getAzureCredentials)
                .map(secretSelectorGetterAndSetter(
                    AzureBlobStorageCredentialsDto::getAccessKey,
                    AzureBlobStorageCredentialsDto::setAccessKey,
                    (c) -> Optional.ofNullable(c.getSecretKeySelectors())
                        .map(AzureBlobSecretKeySelectorDto::getAccessKey)
                        .orElse(null),
                    (c, s) -> Seq.of(Optional.ofNullable(c.getSecretKeySelectors())
                            .orElseGet(AzureBlobSecretKeySelectorDto::new))
                        .peek(c::setSecretKeySelectors)
                        .forEach(ss -> ss.setAccessKey(s))))))
        .filter(t -> t.v2.isPresent())
        .map(t -> Tuple.tuple(t.v1, t.v2.get()));
  }

  private static <T> Function<T, Tuple4<String, Consumer<String>, SecretKeySelector,
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
        secretKeySelector -> secretKeySelectorSetter.accept(object, secretKeySelector)
    );
  }
}
