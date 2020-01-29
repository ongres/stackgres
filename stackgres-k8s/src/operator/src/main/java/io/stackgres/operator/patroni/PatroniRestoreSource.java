/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterRestore;
import io.stackgres.operator.customresource.sgcluster.StackGresRestoreConfigSource;
import io.stackgres.operator.customresource.storages.AwsCredentials;
import io.stackgres.operator.customresource.storages.AwsS3Storage;
import io.stackgres.operator.customresource.storages.AzureBlobStorage;
import io.stackgres.operator.customresource.storages.BackupStorage;
import io.stackgres.operator.customresource.storages.GoogleCloudStorage;
import io.stackgres.operator.customresource.storages.PgpConfiguration;
import io.stackgres.operator.resource.KubernetesCustomResourceFinder;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;

@ApplicationScoped
public class PatroniRestoreSource {

  private KubernetesCustomResourceScanner<StackGresBackup> backupScanner;

  private KubernetesCustomResourceFinder<StackGresBackupConfig> backupConfigFinder;

  private KubernetesClientFactory clientFactory;

  @Inject
  public PatroniRestoreSource(
      KubernetesCustomResourceScanner<StackGresBackup> backupScanner,
      KubernetesCustomResourceFinder<StackGresBackupConfig> backupConfigFinder,
      KubernetesClientFactory clientFactory) {
    this.backupScanner = backupScanner;
    this.backupConfigFinder = backupConfigFinder;
    this.clientFactory = clientFactory;
  }

  public StackGresRestoreConfigSource getStorageConfig(StackGresClusterRestore config) {

    String stackgresBackup = config.getStackgresBackup();

    StackGresBackup backup = findBackup(stackgresBackup);
    StackGresBackupConfig backupConfig = findStackGresBackupConfig(backup);

    BackupStorage storage = backupConfig.getSpec().getStorage();
    setBackupPrefix(storage, backupConfig, backup);

    StackGresRestoreConfigSource source = new StackGresRestoreConfigSource();
    source.setAutoCopySecretsEnabled(config.isAutoCopySecretsEnabled());
    source.setPgpConfiguration(backupConfig.getSpec().getPgpConfiguration());
    source.setStorage(storage);
    source.setBackupName(backup.getStatus().getName());

    return source;

  }

  public List<SourceSecret> getSourceCredentials(StackGresClusterRestore config,
                                                 String targetNamespace) {

    ImmutableList.Builder<SourceSecret> sourceSecretList = ImmutableList.builder();

    StackGresBackup sourceBackup = findBackup(config.getStackgresBackup());
    StackGresBackupConfig sourceBackupConfig = findStackGresBackupConfig(sourceBackup);

    String sourceNamespace = sourceBackupConfig.getMetadata().getNamespace();
    if (sourceNamespace.equals(targetNamespace)) {
      return sourceSecretList.build();
    }

    addSourceSecrets(sourceSecretList, sourceBackupConfig);

    return sourceSecretList.build();

  }

  private void addSourceSecrets(ImmutableList.Builder<SourceSecret> sourceSecretList,
                                StackGresBackupConfig sourceBackupConfig) {
    String sourceNamespace = sourceBackupConfig.getMetadata().getNamespace();

    try (KubernetesClient client = clientFactory.create()) {

      PgpConfiguration pgpConfiguration = sourceBackupConfig.getSpec().getPgpConfiguration();
      if (pgpConfiguration != null) {
        String secretKeyName = pgpConfiguration
            .getKey().getName();
        sourceSecretList.add(getSourceSecret(sourceNamespace, client, secretKeyName));
      }

      BackupStorage sourceStorage = sourceBackupConfig.getSpec().getStorage();
      AwsS3Storage sourceStorageS3 = sourceStorage.getS3();

      if (sourceStorageS3 != null) {

        List<SourceSecret> awsSourceSecrets = getAwsSourceSecrets(
            sourceNamespace,
            client,
            sourceStorageS3);

        sourceSecretList.addAll(awsSourceSecrets);
      } else {
        GoogleCloudStorage sourceGcsStorage = sourceStorage.getGcs();

        if (sourceGcsStorage != null) {
          String secretName = sourceGcsStorage
              .getCredentials()
              .getServiceAccountJsonKey()
              .getName();

          SourceSecret sourceSecret = getSourceSecret(sourceNamespace, client, secretName);

          sourceSecretList.add(sourceSecret);
        } else {

          List<SourceSecret> azureSourceSecrets = getAzureSourceSecrets(sourceNamespace,
              client,
              sourceStorage);
          sourceSecretList.addAll(azureSourceSecrets);

        }
      }

    }
  }

  private List<SourceSecret> getAzureSourceSecrets(String sourceNamespace,
                                                   KubernetesClient client,
                                                   BackupStorage sourceStorage) {
    List<SourceSecret> azureSourceSecrets = new ArrayList<>();

    AzureBlobStorage azureStorage = sourceStorage.getAzureblob();

    String accessKeySecretName = azureStorage.getCredentials().getAccessKey().getName();
    String accountSecretName = azureStorage.getCredentials().getAccount().getName();

    SourceSecret accessKeySourceSecret = getSourceSecret(sourceNamespace,
        client,
        accessKeySecretName);

    azureSourceSecrets.add(accessKeySourceSecret);

    if (!accessKeySecretName.equals(accountSecretName)) {
      SourceSecret accountSourceSecret = getSourceSecret(sourceNamespace,
          client,
          accountSecretName);

      azureSourceSecrets.add(accountSourceSecret);

    }
    return azureSourceSecrets;
  }

  private List<SourceSecret> getAwsSourceSecrets(String sourceNamespace,
                                                 KubernetesClient client,
                                                 AwsS3Storage sourceStorageS3) {
    List<SourceSecret> awsSourceSecrets = new ArrayList<>();

    AwsCredentials credentials = sourceStorageS3.getCredentials();
    String accessKeySecretName = credentials.getAccessKey().getName();

    SourceSecret sourceSecret = getSourceSecret(sourceNamespace, client, accessKeySecretName);

    awsSourceSecrets.add(sourceSecret);

    String secretKeySecretName = credentials.getSecretKey().getName();

    if (!accessKeySecretName.equals(secretKeySecretName)) {

      SourceSecret secretKeySourceSecret = getSourceSecret(
          sourceNamespace,
          client,
          secretKeySecretName);

      awsSourceSecrets.add(secretKeySourceSecret);
    }
    return awsSourceSecrets;
  }

  private SourceSecret getSourceSecret(String sourceNamespace,
                                       KubernetesClient client,
                                       String secretName) {
    Secret originSecret = client
        .secrets()
        .inNamespace(sourceNamespace)
        .withName(secretName)
        .get();

    SourceSecret sourceSecret = new SourceSecret();
    sourceSecret.setSecretName(secretName);
    sourceSecret.setData(originSecret.getData());
    return sourceSecret;
  }

  private String configurePrefix(String existingPrefix,
                                 StackGresBackup backup,
                                 StackGresBackupConfig backupConfig) {
    return existingPrefix
        + "/" + backupConfig.getMetadata().getNamespace()
        + "/" + backup.getSpec().getCluster();
  }

  private void setBackupPrefix(BackupStorage storage,
                               StackGresBackupConfig backupConfig,
                               StackGresBackup backup) {

    AwsS3Storage s3 = storage.getS3();
    if (s3 != null) {
      String prefix = s3.getPrefix();
      prefix = configurePrefix(prefix, backup, backupConfig);
      s3.setPrefix(prefix);
    } else {
      AzureBlobStorage azureblob = storage.getAzureblob();
      if (azureblob != null) {
        String prefix = azureblob.getPrefix();
        prefix = configurePrefix(prefix, backup, backupConfig);
        azureblob.setPrefix(prefix);
      } else {
        GoogleCloudStorage gcs = storage.getGcs();
        String prefix = gcs.getPrefix();
        prefix = configurePrefix(prefix, backup, backupConfig);
        storage.getGcs().setPrefix(prefix);
      }
    }
  }

  private StackGresBackupConfig findStackGresBackupConfig(StackGresBackup backup) {
    return backupConfigFinder
        .findByNameAndNamespace(backup.getStatus().getBackupConfig(),
            backup.getMetadata().getNamespace()).orElseThrow(() ->
            new IllegalArgumentException("Backup config"
                + backup.getStatus().getBackupConfig()
                + " not found")
        );
  }

  private StackGresBackup findBackup(String stackgresBackup) {
    Optional<StackGresBackup> backup = backupScanner.findResources()
        .flatMap(backups -> backups.stream()
            .filter(b -> b.getMetadata().getUid().equals(stackgresBackup))
            .findFirst()
        );

    return backup.orElseThrow(
        () -> new IllegalArgumentException("Backup " + stackgresBackup + "not found"));
  }

  public class SourceSecret {
    private String secretName;
    private Map<String, String> data;

    public String getSecretName() {
      return secretName;
    }

    public void setSecretName(String secretName) {
      this.secretName = secretName;
    }

    public Map<String, String> getData() {
      return data;
    }

    public void setData(Map<String, String> data) {
      this.data = data;
    }
  }

}
