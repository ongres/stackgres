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
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigSpec;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupEnvVarFactory;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V11)
public class RestoreSecret
    implements VolumeFactory<StackGresClusterContext> {

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  private BackupEnvVarFactory envVarFactory;

  public static String name(ClusterContext context) {
    final String clusterName = context.getCluster().getMetadata().getName();
    return StatefulSetDynamicVolumes.RESTORE_CREDENTIALS.getResourceName(clusterName);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build());
  }

  private @NotNull Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StatefulSetDynamicVolumes.RESTORE_CREDENTIALS.getVolumeName())
        .withSecret(new SecretVolumeSourceBuilder()
            .withSecretName(name(context))
            .build())
        .build();
  }

  private @NotNull Optional<Secret> buildSource(StackGresClusterContext context) {
    return context.getRestoreBackup().map(restoreBackup -> {
      final Map<String, String> data = new HashMap<>();
      final StackGresCluster cluster = context.getSource();

      if (restoreBackup.getSpec() != null && restoreBackup.getStatus() != null) {
        data.put("BACKUP_RESOURCE_VERSION",
            restoreBackup.getMetadata().getResourceVersion());
        String backupNamespace = restoreBackup.getMetadata().getNamespace();
        StackGresBackupConfigSpec backupConfig = restoreBackup.getStatus().getBackupConfig();
        data.putAll(envVarFactory.getSecretEnvVar(backupNamespace, backupConfig));
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
    });
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
