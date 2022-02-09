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

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgbackup.BackupPhase;
import io.stackgres.common.crd.sgbackup.StackGresBackupProcess;
import io.stackgres.common.crd.sgbackup.StackGresBackupStatus;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.ClusterStatefulSet;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import io.stackgres.operator.conciliation.factory.cluster.backup.AbstractBackupConfigMap;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class RestoreConfigMap extends AbstractBackupConfigMap
    implements VolumeFactory<StackGresClusterContext> {

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  public static String name(ClusterContext context) {
    final String clusterName = context.getCluster().getMetadata().getName();
    return StatefulSetDynamicVolumes.RESTORE_ENV.getResourceName(clusterName);
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
        .withName(StatefulSetDynamicVolumes.RESTORE_ENV.getVolumeName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(name(context))
            .build())
        .build();
  }

  private @NotNull Optional<ConfigMap> buildSource(StackGresClusterContext context) {
    return context.getRestoreBackup().map(restoreBackup -> {
      final Map<String, String> data = new HashMap<>();
      final StackGresCluster cluster = context.getSource();

      if (restoreBackup.getSpec() != null && restoreBackup.getStatus() != null) {
        final String status = Optional.ofNullable(restoreBackup.getStatus())
            .map(StackGresBackupStatus::getProcess)
            .map(StackGresBackupProcess::getStatus)
            .orElse(BackupPhase.RUNNING.toString());

        if (!BackupPhase.COMPLETED.toString().equals(status)) {
          data.put("RESTORE_BACKUP_ERROR", "Backup is " + status);
        }

        data.put("BACKUP_RESOURCE_VERSION",
            restoreBackup.getMetadata().getResourceVersion());
        data.put("RESTORE_BACKUP_ID",
            restoreBackup.getStatus().getInternalName());

        data.putAll(getBackupEnvVars(context,
            restoreBackup.getMetadata().getNamespace(),
            restoreBackup.getSpec().getSgCluster(),
            restoreBackup.getStatus().getBackupConfig()));

        Optional.ofNullable(cluster.getSpec())
            .map(StackGresClusterSpec::getInitData)
            .map(StackGresClusterInitData::getRestore)
            .map(StackGresClusterRestore::getDownloadDiskConcurrency)
            .ifPresent(downloadDiskConcurrency -> data.put(
                "WALG_DOWNLOAD_CONCURRENCY", convertEnvValue(downloadDiskConcurrency)));
      } else {
        data.put("RESTORE_BACKUP_ERROR", "Can not restore from backup. Backup not found!");
      }

      return new ConfigMapBuilder()
          .withNewMetadata()
          .withNamespace(cluster.getMetadata().getNamespace())
          .withName(name(context))
          .withLabels(labelFactory.genericLabels(cluster))
          .endMetadata()
          .withData(StackGresUtil.addMd5Sum(data))
          .build();
    });
  }

  @Override
  protected String getGcsCredentialsFilePath(ClusterContext context) {
    return ClusterStatefulSetPath.RESTORE_SECRET_PATH.path(context)
        + "/" + ClusterStatefulSet.GCS_CREDENTIALS_FILE_NAME;
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}
