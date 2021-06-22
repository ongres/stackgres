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

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterStatefulSetPath;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInitData;
import io.stackgres.common.crd.sgcluster.StackGresClusterRestore;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.ClusterStatefulSet;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import io.stackgres.operator.conciliation.factory.cluster.backup.AbstractBackupConfigMap;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class RestoreConfigMap extends AbstractBackupConfigMap
    implements VolumeFactory<StackGresClusterContext> {

  private LabelFactory<StackGresCluster> labelFactory;

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
            .build()
    );
  }

  public @NotNull Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StatefulSetDynamicVolumes.RESTORE_ENV.getVolumeName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(name(context))
            .build())
        .build();
  }

  public @NotNull Optional<HasMetadata> buildSource(StackGresClusterContext context) {
    return context.getRestoreBackup().map(restoreBackup -> {
      final Map<String, String> data = new HashMap<>();

      data.put("BACKUP_RESOURCE_VERSION",
          restoreBackup.getMetadata().getResourceVersion());
      data.put("RESTORE_BACKUP_ID",
          restoreBackup.getStatus().getInternalName());

      data.putAll(getBackupEnvVars(context,
          restoreBackup.getMetadata().getNamespace(),
          restoreBackup.getSpec().getSgCluster(),
          restoreBackup.getStatus().getBackupConfig()));

      final StackGresCluster cluster = context.getSource();
      Optional.ofNullable(cluster.getSpec())
          .map(StackGresClusterSpec::getInitData)
          .map(StackGresClusterInitData::getRestore)
          .map(StackGresClusterRestore::getDownloadDiskConcurrency)
          .ifPresent(downloadDiskConcurrency -> data.put(
              "WALG_DOWNLOAD_CONCURRENCY", convertEnvValue(downloadDiskConcurrency)));
      return new ConfigMapBuilder()
          .withNewMetadata()
          .withNamespace(cluster.getMetadata().getNamespace())
          .withName(name(context))
          .withLabels(labelFactory.patroniClusterLabels(cluster))
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

  private <T> String convertEnvValue(T value) {
    return value.toString();
  }

  @Inject
  public void setLabelFactory(LabelFactory<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}
