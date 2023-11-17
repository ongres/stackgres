/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.replicate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.backup.AbstractBackupConfigMap;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupEnvVarFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class ReplicateConfigMap extends AbstractBackupConfigMap
    implements VolumeFactory<StackGresClusterContext> {

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  public static String name(ClusterContext context) {
    final String clusterName = context.getCluster().getMetadata().getName();
    return StackGresVolume.REPLICATE_ENV.getResourceName(clusterName);
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
        .withName(StackGresVolume.REPLICATE_ENV.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(name(context))
            .build())
        .build();
  }

  private ConfigMap buildSource(StackGresClusterContext context) {
    final Map<String, String> data = new HashMap<>();
    final StackGresCluster cluster = context.getSource();

    context.getReplicateStorage()
        .ifPresent(storage -> data.putAll(
                getBackupEnvVars(
                    context,
                    context.getReplicatePath().orElseThrow(),
                    storage
                )
            )
        );

    context.getReplicateConfiguration()
        .ifPresent(config -> data.putAll(
                getBackupEnvVars(
                    config
                )
            )
        );

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(data))
        .build();
  }

  @Override
  protected String getAwsS3CompatibleCaCertificateFilePath(ClusterContext context) {
    return ClusterPath.REPLICATE_SECRET_PATH.path(context)
        + "/" + BackupEnvVarFactory.AWS_S3_COMPATIBLE_CA_CERTIFICATE_FILE_NAME;
  }

  @Override
  protected String getGcsCredentialsFilePath(ClusterContext context) {
    return ClusterPath.REPLICATE_SECRET_PATH.path(context)
        + "/" + BackupEnvVarFactory.GCS_CREDENTIALS_FILE_NAME;
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }
}
