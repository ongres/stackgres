/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common;

import java.util.Optional;
import java.util.function.Function;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.storages.AwsS3CompatibleStorage;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.AzureBlobStorage;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.common.crd.storages.GoogleCloudStorage;
import org.jooq.lambda.Unchecked;
import org.jooq.lambda.fi.util.function.CheckedFunction;

public interface BackupStorageUtil {

  static String getPrefixForS3(
      String path,
      AwsS3Storage storageForS3) {
    return composePrefix(
        getFromS3(storageForS3, AwsS3Storage::getPrefix),
        path);
  }

  static String getPrefixForS3Compatible(
      String path,
      AwsS3CompatibleStorage storageForS3Compatible) {
    return composePrefix(
        getFromS3Compatible(storageForS3Compatible, AwsS3CompatibleStorage::getPrefix),
        path);
  }

  static String getPrefixForGcs(
      String path,
      GoogleCloudStorage storageForGcs) {
    return composePrefix(
        getFromGcs(storageForGcs, GoogleCloudStorage::getPrefix),
        path);
  }

  static String getPrefixForAzureBlob(
      String path,
      AzureBlobStorage storageForAzureBlob) {
    return composePrefix(
        getFromAzureBlob(storageForAzureBlob, AzureBlobStorage::getPrefix),
        path);
  }

  private static String composePrefix(
      String prefix,
      String path) {
    return prefix + "/" + path;
  }

  static String getPath(
      String namespace,
      String name,
      String postgresMajorVersion) {
    return HasMetadata.getFullResourceName(StackGresBackup.class) + "/"
        + namespace + "/" + name + "/" + postgresMajorVersion;
  }

  static <T> Optional<T> getStorageFor(
      StackGresBackupConfigSpec configSpec,
      Function<BackupStorage, T> getter) {
    return Optional.of(configSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(getter);
  }

  static <T> Optional<T> getStorageFor(
      BackupStorage storage,
      Function<BackupStorage, T> getter) {
    return Optional.of(storage)
        .map(getter);
  }

  static <T> String getFromS3(
      AwsS3Storage storageFor,
      Function<AwsS3Storage, T> getter) {
    return BackupStorageUtil.convertEnvValue(getter.apply(storageFor));
  }

  static <T> String getFromS3Compatible(
      AwsS3CompatibleStorage storageFor,
      Function<AwsS3CompatibleStorage, T> getter) {
    return BackupStorageUtil.convertEnvValue(
        getter.apply(storageFor));

  }

  static <T, R> String getFromS3Compatible(
      AwsS3CompatibleStorage storageFor,
      Function<AwsS3CompatibleStorage, T> getter,
      CheckedFunction<T, R> transformer) {
    return BackupStorageUtil.convertEnvValue(
        Unchecked.function(transformer).apply(
            getter.apply(storageFor)));
  }

  static <T> String getFromGcs(
      GoogleCloudStorage storageFor,
      Function<GoogleCloudStorage, T> getter) {
    return BackupStorageUtil.convertEnvValue(
        getter.apply(storageFor));
  }

  static <T> String getFromAzureBlob(
      AzureBlobStorage storageFor,
      Function<AzureBlobStorage, T> getter) {
    return BackupStorageUtil.convertEnvValue(
        getter.apply(storageFor));
  }

  static <T> String convertEnvValue(T value) {
    if (value != null) {
      return value.toString();
    } else {
      return "";
    }
  }

}
