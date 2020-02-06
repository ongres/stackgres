/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.cluster;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.ObjectFieldSelectorBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.api.model.SecretKeySelectorBuilder;
import io.stackgres.operator.common.StackGresBackupContext;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresRestoreContext;
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupStatus;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.operator.customresource.storages.BackupStorage;
import io.stackgres.operator.patroni.PatroniSecret;

import org.jooq.lambda.Seq;

@ApplicationScoped
public class ClusterStatefulSetEnvironmentVariables {

  public ImmutableList<EnvVar> getPatroniEnvironmentVariables(StackGresClusterContext context) {
    return ImmutableList.of(
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
                        .withName(context.getCluster().getMetadata().getName())
                        .withKey("superuser-password")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_REPLICATION_PASSWORD")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(context.getCluster().getMetadata().getName())
                        .withKey("replication-password")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_authenticator_PASSWORD")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(
                    new SecretKeySelectorBuilder()
                        .withName(context.getCluster().getMetadata().getName())
                        .withKey("authenticator-password")
                        .build())
                .build())
            .build(),
        new EnvVarBuilder().withName("PATRONI_authenticator_OPTIONS")
            .withValue("superuser")
            .build());
  }

  public ImmutableList<EnvVar> getBackupEnvironmentVariables(StackGresClusterContext context) {
    return Seq.of(
        context.getBackupContext()
        .map(StackGresBackupContext::getBackupConfig)
        .map(StackGresBackupConfig::getSpec)
        .map(StackGresBackupConfigSpec::getPgpConfiguration)
        .map(pgpConf -> Seq.of(new EnvVarBuilder()
            .withName("WALG_PGP_KEY")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(pgpConf.getKey())
                .build())
            .build())),
        context.getBackupContext()
        .map(StackGresBackupContext::getBackupConfig)
        .map(StackGresBackupConfig::getSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(BackupStorage::getS3)
        .map(awsConf -> Seq.of(new EnvVarBuilder()
            .withName("AWS_ACCESS_KEY_ID")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(awsConf.getCredentials().getAccessKey())
                .build())
            .build(),
            new EnvVarBuilder()
            .withName("AWS_SECRET_ACCESS_KEY")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(awsConf.getCredentials().getSecretKey())
                .build())
            .build())),
        context.getBackupContext()
        .map(StackGresBackupContext::getBackupConfig)
        .map(StackGresBackupConfig::getSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(BackupStorage::getGcs)
        .map(gcsConfig -> Seq.of(new EnvVarBuilder()
            .withName("GOOGLE_APPLICATION_CREDENTIALS")
            .withValue(ClusterStatefulSet.GCS_CONFIG_PATH
                + "/" + ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME)
            .build())),
        context.getBackupContext()
        .map(StackGresBackupContext::getBackupConfig)
        .map(StackGresBackupConfig::getSpec)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(BackupStorage::getAzureblob)
        .map(azureConfig -> Seq.of(new EnvVarBuilder()
            .withName("AZURE_STORAGE_ACCOUNT")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(azureConfig.getCredentials().getAccount())
                .build())
            .build(),
            new EnvVarBuilder()
            .withName("AZURE_STORAGE_ACCESS_KEY")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(azureConfig.getCredentials().getAccessKey())
                .build())
            .build())))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(s -> s)
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<EnvVar> getRestoreEnvironmentVariables(StackGresClusterContext context) {
    return Seq.of(
        context.getRestoreContext()
        .map(StackGresRestoreContext::getBackup)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getBackupConfig)
        .map(StackGresBackupConfigSpec::getPgpConfiguration)
        .map(pgpConf -> Seq.of(new EnvVarBuilder()
            .withName("WALG_PGP_KEY")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(restoreSecretKeySelector(context,
                    pgpConf.getKey()))
                .build())
            .build())),
        context.getRestoreContext()
        .map(StackGresRestoreContext::getBackup)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getBackupConfig)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(BackupStorage::getS3)
        .map(awsConf -> Seq.of(new EnvVarBuilder()
            .withName("AWS_ACCESS_KEY_ID")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(restoreSecretKeySelector(context,
                    awsConf.getCredentials().getAccessKey()))
                .build())
            .build(),
            new EnvVarBuilder()
            .withName("AWS_SECRET_ACCESS_KEY")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(restoreSecretKeySelector(context,
                    awsConf.getCredentials().getSecretKey()))
                .build())
            .build())),
        context.getRestoreContext()
        .map(StackGresRestoreContext::getBackup)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getBackupConfig)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(BackupStorage::getGcs)
        .map(gcsConfig -> Seq.of(new EnvVarBuilder()
            .withName("GOOGLE_APPLICATION_CREDENTIALS")
            .withValue(ClusterStatefulSet.GCS_CONFIG_PATH
                + "/" + ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME)
            .build())),
        context.getRestoreContext()
        .map(StackGresRestoreContext::getBackup)
        .map(StackGresBackup::getStatus)
        .map(StackGresBackupStatus::getBackupConfig)
        .map(StackGresBackupConfigSpec::getStorage)
        .map(BackupStorage::getAzureblob)
        .map(azureConfig -> Seq.of(new EnvVarBuilder()
            .withName("AZURE_STORAGE_ACCOUNT")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(restoreSecretKeySelector(context,
                    azureConfig.getCredentials().getAccount()))
                .build())
            .build(),
            new EnvVarBuilder()
            .withName("AZURE_STORAGE_ACCESS_KEY")
            .withValueFrom(new EnvVarSourceBuilder()
                .withSecretKeyRef(restoreSecretKeySelector(context,
                    azureConfig.getCredentials().getAccessKey()))
                .build())
            .build())))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .flatMap(s -> s)
        .collect(ImmutableList.toImmutableList());
  }

  private SecretKeySelector restoreSecretKeySelector(
      StackGresClusterContext context,
      SecretKeySelector selector) {
    if (context.getRestoreContext().get().getRestore().isAutoCopySecretsEnabled()
        && !context.getRestoreContext().get().getBackup().getMetadata().getNamespace()
        .equals(context.getCluster().getMetadata().getNamespace())) {
      return new SecretKeySelectorBuilder(selector)
          .withName(PatroniSecret.restoreCopiedSecretName(context, selector.getName()))
          .build();
    }
    return selector;
  }
}
