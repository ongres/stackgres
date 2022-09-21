/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
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
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.factory.cluster.ClusterStatefulSet;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@ApplicationScoped
public class BackupEnvVarFactory {

  private final ResourceFinder<Secret> secretFinder;

  @Inject
  public BackupEnvVarFactory(ResourceFinder<Secret> secretFinder) {
    this.secretFinder = secretFinder;
  }

  public Map<String, Map<String, String>> getStorageSecretReferences(
      String namespace,
      StackGresBackupConfigSpec backupConfSpec) {
    return getStorageSecretReferences(namespace, backupConfSpec.getStorage());
  }

  public Map<String, Map<String, String>> getStorageSecretReferences(
      String namespace,
      BackupStorage storage
  ) {
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
            Optional.ofNullable(storage.getGcs())
                .map(GoogleCloudStorage::getCredentials)
                .map(GoogleCloudCredentials::getSecretKeySelectors)
                .map(GoogleCloudSecretKeySelector::getServiceAccountJsonKey),
            Optional.ofNullable(storage.getAzureBlob())
                .map(AzureBlobStorage::getAzureCredentials)
                .map(AzureBlobStorageCredentials::getSecretKeySelectors)
                .map(AzureBlobSecretKeySelector::getAccount),
            Optional.ofNullable(storage.getAzureBlob())
                .map(AzureBlobStorage::getAzureCredentials)
                .map(AzureBlobStorageCredentials::getSecretKeySelectors)
                .map(AzureBlobSecretKeySelector::getAccessKey))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(secretKeySelector -> Tuple.tuple(secretKeySelector,
            getSecret(namespace, secretKeySelector)))
        .grouped(t -> t.v1.getName())
        .collect(ImmutableMap.toImmutableMap(
            t -> t.v1, t -> t.v2
                .collect(ImmutableMap.toImmutableMap(
                    tt -> tt.v1.getKey(), tt -> tt.v2))));
  }

  public Map<String, String> getSecretEnvVar(String namespace,
      StackGresBackupConfigSpec backupConfigSpec) {
    var secrets = getStorageSecretReferences(namespace, backupConfigSpec);
    return getBackupSecrets(backupConfigSpec.getStorage(), secrets);
  }

  public Map<String, String> getSecretEnvVar(String namespace, BackupStorage storage) {
    var secrets = getStorageSecretReferences(namespace, storage);
    return getBackupSecrets(storage, secrets);
  }

  private String getSecret(String namespace, SecretKeySelector secretKeySelector) {
    return Optional.of(secretFinder.findByNameAndNamespace(secretKeySelector.getName(), namespace)
            .orElseThrow(() -> new IllegalStateException(
                "Secret " + namespace + "." + secretKeySelector.getName()
                    + " not found")))
        .map(Secret::getData)
        .map(data -> data.get(secretKeySelector.getKey()))
        .map(ResourceUtil::decodeSecret)
        .orElseThrow(() -> new IllegalStateException(
            "Key " + secretKeySelector.getKey()
                + " not found in secret " + namespace + "."
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
                            .getSecretKeySelectors().getSecretAccessKey(), secrets))),
            Optional.of(storage)
                .map(BackupStorage::getGcs)
                .map(GoogleCloudStorage::getCredentials)
                .map(GoogleCloudCredentials::getSecretKeySelectors)
                .map(gcsConfigSecretKeySelectors -> Seq.of(
                    getSecretEntry(
                        ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME,
                        gcsConfigSecretKeySelectors.getServiceAccountJsonKey(),
                        secrets))),
            Optional.of(storage)
                .map(BackupStorage::getAzureBlob)
                .map(azureConfig -> Seq.of(
                    getSecretEntry("AZURE_STORAGE_ACCOUNT",
                        azureConfig.getAzureCredentials()
                            .getSecretKeySelectors().getAccount(), secrets),
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
            .orElseThrow(() -> new IllegalStateException(
                "Key " + secretKeySelector.getKey() + " in secret "
                    + secretKeySelector.getName() + " not available")))
        .orElseThrow(() -> new IllegalStateException(
            "Secret " + secretKeySelector.getName() + " not available")));
  }
}
