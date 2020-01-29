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
import io.stackgres.operator.customresource.sgcluster.StackGresClusterRestore;
import io.stackgres.operator.customresource.sgcluster.StackGresRestoreConfigSource;
import io.stackgres.operator.customresource.storages.AwsCredentials;
import io.stackgres.operator.customresource.storages.AwsS3Storage;
import io.stackgres.operator.customresource.storages.AzureBlobStorage;
import io.stackgres.operator.customresource.storages.BackupStorage;
import io.stackgres.operator.customresource.storages.GoogleCloudStorage;
import io.stackgres.operator.customresource.storages.PgpConfiguration;
import io.stackgres.operator.resource.KubernetesCustomResourceScanner;

@ApplicationScoped
public class PatroniRestoreSource {

  private KubernetesCustomResourceScanner<StackGresBackup> backupScanner;

  private KubernetesClientFactory clientFactory;

  @Inject
  public PatroniRestoreSource(
      KubernetesCustomResourceScanner<StackGresBackup> backupScanner,
      KubernetesClientFactory clientFactory) {
    this.backupScanner = backupScanner;
    this.clientFactory = clientFactory;
  }

  public StackGresRestoreConfigSource getStorageConfig(StackGresClusterRestore config) {

    String stackgresBackup = config.getStackgresBackup();

    StackGresBackup backup = findBackup(stackgresBackup);

    setBackupPrefix(backup);

    StackGresRestoreConfigSource source = new StackGresRestoreConfigSource();
    source.setAutoCopySecretsEnabled(config.isAutoCopySecretsEnabled());
    source.setPgpConfiguration(backup.getStatus().getBackupConfig().getPgpConfiguration());
    source.setStorage(backup.getStatus().getBackupConfig().getStorage());
    source.setBackupName(backup.getStatus().getName());

    return source;

  }

  public List<SourceSecret> getSourceCredentials(StackGresClusterRestore config,
      String targetNamespace) {

    ImmutableList.Builder<SourceSecret> sourceSecretList = ImmutableList.builder();

    StackGresBackup sourceBackup = findBackup(config.getStackgresBackup());

    String sourceNamespace = sourceBackup.getMetadata().getNamespace();
    if (sourceNamespace.equals(targetNamespace)) {
      return sourceSecretList.build();
    }

    addSourceSecrets(sourceSecretList, sourceBackup);

    return sourceSecretList.build();

  }

  private void addSourceSecrets(ImmutableList.Builder<SourceSecret> sourceSecretList,
                                StackGresBackup sourceBackup) {
    String sourceNamespace = sourceBackup.getMetadata().getNamespace();

    try (KubernetesClient client = clientFactory.create()) {

      PgpConfiguration pgpConfiguration = sourceBackup.getStatus()
          .getBackupConfig().getPgpConfiguration();
      if (pgpConfiguration != null) {
        String secretKeyName = pgpConfiguration
            .getKey().getName();
        sourceSecretList.add(getSourceSecret(sourceNamespace, client, secretKeyName));
      }

      BackupStorage sourceStorage = sourceBackup.getStatus().getBackupConfig().getStorage();
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
      StackGresBackup backup) {
    return existingPrefix
        + "/" + backup.getMetadata().getNamespace()
        + "/" + backup.getSpec().getCluster();
  }

  private void setBackupPrefix(StackGresBackup backup) {

    BackupStorage backupStorage = backup.getStatus().getBackupConfig().getStorage();
    AwsS3Storage s3 = backupStorage.getS3();
    if (s3 != null) {
      String prefix = s3.getPrefix();
      prefix = configurePrefix(prefix, backup);
      s3.setPrefix(prefix);
    } else {
      AzureBlobStorage azureblob = backupStorage.getAzureblob();
      if (azureblob != null) {
        String prefix = azureblob.getPrefix();
        prefix = configurePrefix(prefix, backup);
        azureblob.setPrefix(prefix);
      } else {
        GoogleCloudStorage gcs = backupStorage.getGcs();
        String prefix = gcs.getPrefix();
        prefix = configurePrefix(prefix, backup);
        backupStorage.getGcs().setPrefix(prefix);
      }
    }
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
