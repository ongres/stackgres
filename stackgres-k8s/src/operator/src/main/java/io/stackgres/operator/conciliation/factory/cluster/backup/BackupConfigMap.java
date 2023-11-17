/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class BackupConfigMap extends AbstractBackupConfigMap
    implements VolumeFactory<StackGresClusterContext> {

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  public static String name(ClusterContext clusterContext) {
    final String clusterName = clusterContext.getCluster().getMetadata().getName();
    return StackGresVolume.BACKUP_ENV.getResourceName(clusterName);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .source(buildSource(context))
            .volume(buildVolume(context))
            .build()
    );
  }

  private Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.BACKUP_ENV.getName())
        .withNewConfigMap()
        .withName(name(context))
        .withDefaultMode(0444)
        .endConfigMap()
        .build();
  }

  private HasMetadata buildSource(StackGresClusterContext context) {
    final Map<String, String> data = new HashMap<>();

    final StackGresCluster cluster = context.getCluster();
    context.getBackupConfigurationResourceVersion()
        .ifPresent(resourceVersion -> data.put(
                "BACKUP_CONFIG_RESOURCE_VERSION", resourceVersion
            )
        );

    context.getBackupStorage()
        .ifPresent(storage -> data.putAll(
                getBackupEnvVars(
                    context,
                    context.getBackupPath().orElseThrow(),
                    storage
                )
            )
        );

    context.getBackupConfiguration()
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

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

}
