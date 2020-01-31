/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster.factories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.stackgres.operator.cluster.ClusterStatefulSet;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgcluster.StackGresClusterRestore;
import io.stackgres.operator.customresource.sgcluster.StackGresRestoreConfigSource;
import io.stackgres.operator.customresource.storages.AwsS3Storage;
import io.stackgres.operator.customresource.storages.AzureBlobStorage;
import io.stackgres.operator.customresource.storages.BackupStorage;
import io.stackgres.operator.customresource.storages.GoogleCloudStorage;
import io.stackgres.operator.customresource.storages.PgpConfiguration;
import io.stackgres.operator.patroni.PatroniRestoreSource;
import io.stackgres.operatorframework.factories.EnvironmentVariablesFactory;

@ApplicationScoped
public class ClusterStatefulSetEnvironmentVariablesFactory
    implements EnvironmentVariablesFactory<StackGresClusterContext> {

  private PatroniRestoreSource patroniRestoreSource;

  @Inject
  public ClusterStatefulSetEnvironmentVariablesFactory(PatroniRestoreSource patroniRestoreSource) {
    this.patroniRestoreSource = patroniRestoreSource;
  }

  @Override
  public ImmutableList<EnvVar> getEnvironmentVariables(StackGresClusterContext config) {
    final String name = config.getCluster().getMetadata().getName();

    ImmutableList.Builder<EnvVar> environmentsBuilder = ImmutableList.<EnvVar>builder().add(
        buildPatroniEnvironmentVariables(name)
    );

    config.getBackupConfig().ifPresent(backupConfig -> {
      environmentsBuilder.addAll(buildBackupEnvironmentVariables(backupConfig));
    });

    config.getRestoreConfig().ifPresent(restoreConfig -> {
      environmentsBuilder.addAll(buildRecoverEnvironmentVariables(restoreConfig));
    });

    return environmentsBuilder.build();
  }

  private EnvVar[] buildPatroniEnvironmentVariables(String clusterName) {
    return new EnvVar[]{
        new EnvVarBuilder().withName("PATRONI_NAME")
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(
                    new ObjectFieldSelectorBuilder()
                        .withFieldPath("metadata.name").build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_KUBERNETES_NAMESPACE")
            .withValueFrom(new EnvVarSourceBuilder()
                .withFieldRef(
                    new ObjectFieldSelectorBuilder()
                        .withFieldPath("metadata.namespace")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_KUBERNETES_POD_IP")
            .withValueFrom(
                new EnvVarSourceBuilder()
                    .withFieldRef(
                        new ObjectFieldSelectorBuilder()
                            .withFieldPath("status.podIP")
                            .build())
                    .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_SUPERUSER_PASSWORD")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(clusterName)
                        .withKey("superuser-password")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_REPLICATION_PASSWORD")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(clusterName)
                        .withKey("replication-password")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_authenticator_PASSWORD")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(clusterName)
                        .withKey("authenticator-password")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_authenticator_OPTIONS")
            .withValue("superuser")
            .build()
    };
  }

  private List<EnvVar> buildBackupEnvironmentVariables(StackGresBackupConfig backupConfig) {

    List<EnvVar> envVars = new ArrayList<>();

    Optional<PgpConfiguration> pgpConfiguration = Optional.ofNullable(
        backupConfig.getSpec().getPgpConfiguration()
    );

    pgpConfiguration.ifPresent(pgpConf -> envVars.add(new EnvVarBuilder()
        .withName("WALG_PGP_KEY")
        .withValueFrom(new EnvVarSourceBuilder()
            .withSecretKeyRef(pgpConf.getKey())
            .build())
        .build()));

    Optional<AwsS3Storage> awsS3Storage = Optional.ofNullable(
        backupConfig.getSpec().getStorage().getS3()
    );

    awsS3Storage.ifPresent(awsConf -> {
      envVars.add(new EnvVarBuilder()
          .withName("AWS_ACCESS_KEY_ID")
          .withValueFrom(new EnvVarSourceBuilder()
              .withSecretKeyRef(awsConf.getCredentials().getAccessKey())
              .build())
          .build());
      envVars.add(new EnvVarBuilder()
          .withName("AWS_SECRET_ACCESS_KEY")
          .withValueFrom(new EnvVarSourceBuilder()
              .withSecretKeyRef(awsConf.getCredentials().getSecretKey())
              .build())
          .build());
    });

    Optional<GoogleCloudStorage> googleCloudStorage = Optional.ofNullable(
        backupConfig.getSpec().getStorage().getGcs()
    );

    googleCloudStorage.ifPresent(gcsConfig -> envVars.add(new EnvVarBuilder()
        .withName("GOOGLE_APPLICATION_CREDENTIALS")
        .withValue(ClusterStatefulSet.GCS_CONFIG_PATH
            + "/" + ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME)
        .build()
    ));

    Optional<AzureBlobStorage> azureBlobStorage = Optional.ofNullable(
        backupConfig.getSpec().getStorage().getAzureblob()
    );

    azureBlobStorage.ifPresent(azureConfig -> {
      envVars.add(
          new EnvVarBuilder()
              .withName("AZURE_STORAGE_ACCOUNT")
              .withValueFrom(new EnvVarSourceBuilder()
                  .withSecretKeyRef(azureConfig.getCredentials().getAccount())
                  .build())
              .build()
      );
      envVars.add(
          new EnvVarBuilder()
              .withName("AZURE_STORAGE_ACCESS_KEY")
              .withValueFrom(new EnvVarSourceBuilder()
                  .withSecretKeyRef(azureConfig.getCredentials().getAccessKey())
                  .build())
              .build()
      );
    });

    return envVars;

  }

  private List<EnvVar> buildRecoverEnvironmentVariables(StackGresClusterRestore restoreConfig) {

    List<EnvVar> envVars = new ArrayList<>();

    StackGresRestoreConfigSource source = patroniRestoreSource.getStorageConfig(restoreConfig);

    Optional<PgpConfiguration> pgpConfiguration = Optional.ofNullable(
        source.getPgpConfiguration()
    );

    pgpConfiguration.ifPresent(pgpConf -> envVars.add(new EnvVarBuilder()
        .withName("RESTORE_WALG_PGP_KEY")
        .withValueFrom(new EnvVarSourceBuilder()
            .withSecretKeyRef(pgpConf.getKey())
            .build())
        .build()));

    BackupStorage storage = source.getStorage();

    Optional<AwsS3Storage> awsS3Storage = Optional.ofNullable(
        storage.getS3()
    );

    awsS3Storage.ifPresent(awsConf -> {
      envVars.add(new EnvVarBuilder()
          .withName("RESTORE_AWS_ACCESS_KEY_ID")
          .withValueFrom(new EnvVarSourceBuilder()
              .withSecretKeyRef(awsConf.getCredentials().getAccessKey())
              .build())
          .build());
      envVars.add(new EnvVarBuilder()
          .withName("RESTORE_AWS_SECRET_ACCESS_KEY")
          .withValueFrom(new EnvVarSourceBuilder()
              .withSecretKeyRef(awsConf.getCredentials().getSecretKey())
              .build())
          .build());
    });

    Optional<GoogleCloudStorage> googleCloudStorage = Optional.ofNullable(
        storage.getGcs()
    );

    googleCloudStorage.ifPresent(gcsConfig -> envVars.add(new EnvVarBuilder()
        .withName("RESTORE_GOOGLE_APPLICATION_CREDENTIALS")
        .withValue(ClusterStatefulSet.GCS_RESTORE_CONFIG_PATH
            + "/" + ClusterStatefulSet.GCS_RESTORE_CREDENTIALS_FILE_NAME)
        .build()
    ));

    Optional<AzureBlobStorage> azureBlobStorage = Optional.ofNullable(
        storage.getAzureblob()
    );

    azureBlobStorage.ifPresent(azureConfig -> {
      envVars.add(
          new EnvVarBuilder()
              .withName("RESTORE_AZURE_STORAGE_ACCOUNT")
              .withValueFrom(new EnvVarSourceBuilder()
                  .withSecretKeyRef(azureConfig.getCredentials().getAccount())
                  .build())
              .build()
      );
      envVars.add(
          new EnvVarBuilder()
              .withName("RESTORE_AZURE_STORAGE_ACCESS_KEY")
              .withValueFrom(new EnvVarSourceBuilder()
                  .withSecretKeyRef(azureConfig.getCredentials().getAccessKey())
                  .build())
              .build()
      );
    });

    return envVars;

  }
}
