/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.LabelFactoryForCluster;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterBackupConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterConfiguration;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.cluster.StatefulSetDynamicVolumes;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class BackupConfigMap extends AbstractBackupConfigMap
    implements VolumeFactory<StackGresClusterContext> {

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  public static String name(ClusterContext clusterContext) {
    final String clusterName = clusterContext.getCluster().getMetadata().getName();
    return StatefulSetDynamicVolumes.BACKUP_ENV.getResourceName(clusterName);
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

  public @NotNull Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StatefulSetDynamicVolumes.BACKUP_ENV.getVolumeName())
        .withNewConfigMap()
        .withName(name(context))
        .withDefaultMode(444)
        .endConfigMap()
        .build();
  }

  public @NotNull HasMetadata buildSource(StackGresClusterContext context) {
    final Map<String, String> data = new HashMap<>();

    final StackGresCluster cluster = context.getCluster();
    context.getBackupConfig()
        .ifPresent(backupConfig -> {
          data.put("BACKUP_CONFIG_RESOURCE_VERSION",
              backupConfig.getMetadata().getResourceVersion());
          data.putAll(getBackupEnvVars(context,
              Optional.of(context.getCluster())
              .map(StackGresCluster::getSpec)
              .map(StackGresClusterSpec::getConfiguration)
              .map(StackGresClusterConfiguration::getBackupPath)
              .or(() -> Optional.of(context.getCluster())
                  .map(StackGresCluster::getSpec)
                  .map(StackGresClusterSpec::getConfiguration)
                  .map(StackGresClusterConfiguration::getBackups)
                  .map(List::stream)
                  .flatMap(Stream::findFirst)
                  .map(StackGresClusterBackupConfiguration::getPath))
              .orElseThrow(),
              backupConfig.getSpec()));
        });
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
