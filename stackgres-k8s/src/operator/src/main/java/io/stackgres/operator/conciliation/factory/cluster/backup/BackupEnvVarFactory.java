/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.common.crd.sgbackup.StackGresBackupConfigSpec;
import io.stackgres.common.crd.storages.AwsCredentials;
import io.stackgres.common.crd.storages.AwsS3CompatibleStorage;
import io.stackgres.common.crd.storages.AwsS3Storage;
import io.stackgres.common.crd.storages.AwsSecretKeySelector;
import io.stackgres.common.crd.storages.AzureBlobSecretKeySelector;
import io.stackgres.common.crd.storages.AzureBlobStorage;
import io.stackgres.common.crd.storages.AzureBlobStorageCredentials;
import io.stackgres.common.crd.storages.BackupStorage;
import io.stackgres.common.crd.storages.GoogleCloudCredentials;
import io.stackgres.common.crd.storages.GoogleCloudSecretKeySelector;
import io.stackgres.common.crd.storages.GoogleCloudStorage;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class BackupEnvVarFactory {

  public static final String AWS_S3_COMPATIBLE_CA_CERTIFICATE_FILE_NAME =
      "aws-s3-compatible-ca.crt";
  public static final String GCS_CREDENTIALS_FILE_NAME = "gcs-credentials.json";

  public Map<String, Map<String, String>> getStorageSecretReferences(
      String namespace,
      StackGresBackupConfigSpec backupConfSpec,
      Map<String, Secret> secrets) {
    return getStorageSecretReferences(namespace, backupConfSpec.getStorage(), secrets);
  }

  private Map<String, Map<String, String>> getStorageSecretReferences(
      String namespace,
      BackupStorage storage,
      Map<String, Secret> secrets) {
    return streamStorageSecretReferences(storage)
        .map(secretKeySelector -> Tuple.tuple(secretKeySelector,
            getSecret(secrets, secretKeySelector)))
        .grouped(t -> t.v1.getName())
        .collect(ImmutableMap.toImmutableMap(
            t -> t.v1, t -> t.v2
                .collect(ImmutableMap.toImmutableMap(
                    tt -> tt.v1.getKey(), tt -> tt.v2))));
  }

  public Seq<SecretKeySelector> streamStorageSecretReferences(BackupStorage storage) {
    return Seq.of(
            Optional.ofNullable(storage.getS3())
                .map(AwsS3Storage::getAwsCredentials)
                .map(AwsCredentials::getSecretKeySelectors)
                .map(AwsSecretKeySelector::getAccessKeyId),
            Optional.ofNullable(storage.getS3())
                .map(AwsS3Storage::getAwsCredentials)
                .map(AwsCredentials::getSecretKeySelectors)
                .map(AwsSecretKeySelector::getSecretAccessKey),
            Optional.ofNullable(storage.getS3Compatible())
                .map(AwsS3CompatibleStorage::getAwsCredentials)
                .map(AwsCredentials::getSecretKeySelectors)
                .map(AwsSecretKeySelector::getAccessKeyId),
            Optional.ofNullable(storage.getS3Compatible())
                .map(AwsS3CompatibleStorage::getAwsCredentials)
                .map(AwsCredentials::getSecretKeySelectors)
                .map(AwsSecretKeySelector::getSecretAccessKey),
            Optional.ofNullable(storage.getS3Compatible())
                .map(AwsS3CompatibleStorage::getAwsCredentials)
                .map(AwsCredentials::getSecretKeySelectors)
                .map(AwsSecretKeySelector::getCaCertificate),
            Optional.ofNullable(storage.getGcs())
                .map(GoogleCloudStorage::getGcpCredentials)
                .map(GoogleCloudCredentials::getSecretKeySelectors)
                .map(GoogleCloudSecretKeySelector::getServiceAccountJsonKey),
            Optional.ofNullable(storage.getAzureBlob())
                .map(AzureBlobStorage::getAzureCredentials)
                .map(AzureBlobStorageCredentials::getSecretKeySelectors)
                .map(AzureBlobSecretKeySelector::getStorageAccount),
            Optional.ofNullable(storage.getAzureBlob())
                .map(AzureBlobStorage::getAzureCredentials)
                .map(AzureBlobStorageCredentials::getSecretKeySelectors)
                .map(AzureBlobSecretKeySelector::getAccessKey))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  public Map<String, String> getSecretEnvVar(
      String namespace,
      StackGresBackupConfigSpec backupConfigSpec,
      Map<String, Secret> secrets) {
    var foundSecrets = getStorageSecretReferences(namespace, backupConfigSpec, secrets);
    return getBackupSecrets(backupConfigSpec.getStorage(), foundSecrets);
  }

  public Map<String, String> getSecretEnvVar(
      String namespace,
      BackupStorage storage,
      Map<String, Secret> secrets) {
    var foundSecrets = getStorageSecretReferences(namespace, storage, secrets);
    return getBackupSecrets(storage, foundSecrets);
  }

  private String getSecret(
      Map<String, Secret> secrets,
      SecretKeySelector secretKeySelector) {
    return Optional.of(
        Optional.ofNullable(secrets.get(secretKeySelector.getName()))
        .orElseThrow(() -> new IllegalArgumentException(
            "Secret " + secretKeySelector.getName() + " not found")))
        .map(Secret::getData)
        .map(data -> data.get(secretKeySelector.getKey()))
        .map(ResourceUtil::decodeSecret)
        .orElseThrow(() -> new IllegalArgumentException(
            "Key " + secretKeySelector.getKey()
                + " not found in secret "
                + secretKeySelector.getName()));
  }

  private ImmutableMap<String, String> getBackupSecrets(
      BackupStorage storage, Map<String, Map<String, String>> secrets) {
    return Seq.of(
            Optional.of(storage)
                .map(BackupStorage::getS3)
                .map(awsConf -> Seq.of(
                    getSecretEntry("AWS_ACCESS_KEY_ID",
                        awsConf.getAwsCredentials().getSecretKeySelectors().getAccessKeyId(),
                        secrets),
                    getSecretEntry("AWS_SECRET_ACCESS_KEY",
                        awsConf.getAwsCredentials()
                            .getSecretKeySelectors().getSecretAccessKey(), secrets))),
            Optional.of(storage)
                .map(BackupStorage::getS3Compatible)
                .map(awsConf -> Seq.of(
                    getSecretEntry("AWS_ACCESS_KEY_ID",
                        awsConf.getAwsCredentials().getSecretKeySelectors().getAccessKeyId(),
                        secrets),
                    getSecretEntry("AWS_SECRET_ACCESS_KEY",
                        awsConf.getAwsCredentials()
                        .getSecretKeySelectors().getSecretAccessKey(), secrets))
                    .append(Optional.ofNullable(awsConf.getAwsCredentials()
                            .getSecretKeySelectors().getCaCertificate())
                        .stream()
                        .map(secretKeySelector -> getSecretEntry(
                            AWS_S3_COMPATIBLE_CA_CERTIFICATE_FILE_NAME,
                            secretKeySelector, secrets)))),
            Optional.of(storage)
                .map(BackupStorage::getGcs)
                .map(GoogleCloudStorage::getGcpCredentials)
                .map(GoogleCloudCredentials::getSecretKeySelectors)
                .map(gcsConfigSecretKeySelectors -> Seq.of(
                    getSecretEntry(
                        GCS_CREDENTIALS_FILE_NAME,
                        gcsConfigSecretKeySelectors.getServiceAccountJsonKey(),
                        secrets))),
            Optional.of(storage)
                .map(BackupStorage::getAzureBlob)
                .map(azureConfig -> Seq.of(
                    getSecretEntry("AZURE_STORAGE_ACCOUNT",
                        azureConfig.getAzureCredentials()
                            .getSecretKeySelectors().getStorageAccount(), secrets),
                    getSecretEntry("AZURE_STORAGE_ACCESS_KEY",
                        azureConfig.getAzureCredentials()
                            .getSecretKeySelectors().getAccessKey(), secrets))))
        .filter(Optional::isPresent)
        .flatMap(Optional::get)
        .collect(ImmutableMap.toImmutableMap(t -> t.v1, t -> t.v2));
  }

  private Tuple2<String, String> getSecretEntry(String envvar,
                                                SecretKeySelector secretKeySelector,
                                                Map<String, Map<String, String>> secrets) {
    return Tuple.tuple(envvar, Optional.ofNullable(secrets.get(secretKeySelector.getName()))
        .map(secret -> Optional.ofNullable(secret.get(secretKeySelector.getKey()))
            .orElseThrow(() -> new IllegalArgumentException(
                "Key " + secretKeySelector.getKey() + " in secret "
                    + secretKeySelector.getName() + " not available")))
        .orElseThrow(() -> new IllegalArgumentException(
            "Secret " + secretKeySelector.getName() + " not available")));
  }
}
