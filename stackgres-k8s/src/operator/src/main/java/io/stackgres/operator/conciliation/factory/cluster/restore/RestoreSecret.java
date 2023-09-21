/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.restore;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgbackup.BackupStatus;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupEnvVarFactory;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class RestoreSecret
    implements VolumeFactory<StackGresClusterContext> {

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  private BackupEnvVarFactory envVarFactory;

  public static String name(ClusterContext context) {
    final String clusterName = context.getCluster().getMetadata().getName();
    return StackGresVolume.RESTORE_CREDENTIALS.getResourceName(clusterName);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build());
  }

  private Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.RESTORE_CREDENTIALS.getName())
        .withSecret(new SecretVolumeSourceBuilder()
            .withSecretName(name(context))
            .build())
        .build();
  }

  private Secret buildSource(StackGresClusterContext context) {
    final Optional<StackGresBackup> restoreBackup = context.getRestoreBackup();
    final Map<String, String> data = new HashMap<>();
    final StackGresCluster cluster = context.getSource();

    if (restoreBackup.isPresent()) {
      final String status = restoreBackup
          .map(StackGresBackup::getStatus)
          .map(StackGresBackupStatus::getProcess)
          .map(StackGresBackupProcess::getStatus)
          .orElse(BackupStatus.PENDING.status());

      if (!BackupStatus.COMPLETED.status().equals(status)) {
        data.put("RESTORE_BACKUP_ERROR", "Backup is " + status);
      } else {
        final StackGresBackup backup = restoreBackup.get();
        data.put("BACKUP_RESOURCE_VERSION",
            backup.getMetadata().getResourceVersion());
        String backupNamespace = backup.getMetadata().getNamespace();
        StackGresBackupConfigSpec backupConfig = backup.getStatus().getSgBackupConfig();
        data.putAll(envVarFactory.getSecretEnvVar(backupNamespace, backupConfig));
      }
    } else {
      data.put("RESTORE_BACKUP_ERROR", "Can not restore from backup. Backup not found!");
    }

    return new SecretBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withType("Opaque")
        .withStringData(StackGresUtil.addMd5Sum(data))
        .build();
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Inject
  public void setEnvVarFactory(BackupEnvVarFactory envVarFactory) {
    this.envVarFactory = envVarFactory;
  }
}
