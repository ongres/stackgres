/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factory;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.storages.BackupStorage;

import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractBackupSecret {

  protected ImmutableMap<String, String> getBackupSecrets(
      StackGresBackupConfigSpec backupConfigSpec, Map<String, Map<String, String>> secrets) {
    return Seq.of(
        Optional.of(backupConfigSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(BackupStorage::getS3)
        .map(awsConf -> Seq.of(
            getSecretEntry("AWS_ACCESS_KEY_ID",
                awsConf.getAwsCredentials().getSecretKeySelectors().getAccessKeyId(), secrets),
            getSecretEntry("AWS_SECRET_ACCESS_KEY",
                awsConf.getAwsCredentials()
                    .getSecretKeySelectors().getSecretAccessKey(), secrets))),
        Optional.of(backupConfigSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(BackupStorage::getS3Compatible)
        .map(awsConf -> Seq.of(
            getSecretEntry("AWS_ACCESS_KEY_ID",
                awsConf.getAwsCredentials().getSecretKeySelectors().getAccessKeyId(), secrets),
            getSecretEntry("AWS_SECRET_ACCESS_KEY",
                awsConf.getAwsCredentials()
                    .getSecretKeySelectors().getSecretAccessKey(), secrets))),
        Optional.of(backupConfigSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(BackupStorage::getGcs)
        .map(gcsConfig -> Seq.of(
            getSecretEntry(
                ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME,
                gcsConfig.getCredentials().getSecretKeySelectors().getServiceAccountJsonKey(),
                secrets))),
        Optional.of(backupConfigSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(BackupStorage::getAzureBlob)
        .map(azureConfig -> Seq.of(
            getSecretEntry("AZURE_STORAGE_ACCOUNT",
                azureConfig.getAzureCredentials()
                    .getSecretKeySelectors().getAccount(), secrets),
            getSecretEntry("AZURE_STORAGE_ACCESS_KEY",
                azureConfig.getAzureCredentials()
                    .getSecretKeySelectors().getAccessKey(), secrets))))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(s -> s)
        .collect(ImmutableMap.toImmutableMap(t -> t.v1, t -> t.v2));
  }

  private Tuple2<String, String> getSecretEntry(String envvar,
      SecretKeySelector secretKeySelector, Map<String, Map<String, String>> secrets) {
    return Tuple.tuple(envvar, Optional.ofNullable(secrets.get(secretKeySelector.getName()))
        .map(secret -> Optional.ofNullable(secret.get(secretKeySelector.getKey()))
            .orElseThrow(() -> new IllegalStateException(
                "Key " + secretKeySelector.getKey() + " in secret "
                    + secretKeySelector.getName() + " not available")))
        .orElseThrow(() -> new IllegalStateException(
            "Secret " + secretKeySelector.getName() + " not available")));
  }
}
