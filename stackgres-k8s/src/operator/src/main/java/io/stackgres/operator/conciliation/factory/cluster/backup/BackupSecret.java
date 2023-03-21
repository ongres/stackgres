/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.SecretBuilder;
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
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class BackupSecret
    implements VolumeFactory<StackGresClusterContext> {

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  private BackupEnvVarFactory backupEnvVarFactory;

  public static String name(ClusterContext context) {
    final String clusterName = context.getCluster().getMetadata().getName();
    return StackGresVolume.BACKUP_CREDENTIALS.getResourceName(clusterName);
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

  private Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.BACKUP_CREDENTIALS.getName())
        .withNewSecret()
        .withSecretName(name(context))
        .withDefaultMode(444)
        .endSecret()
        .build();
  }

  private Optional<HasMetadata> buildSource(StackGresClusterContext context) {
    Map<String, String> data = new HashMap<>();

    context.getBackupConfigurationResourceVersion()
        .ifPresent(resourceVersion ->
            data.put("BACKUP_CONFIG_RESOURCE_VERSION", resourceVersion)
        );

    StackGresCluster cluster = context.getSource();
    final String namespace = cluster.getMetadata().getNamespace();

    context.getBackupStorage().ifPresent(
        backupStorage -> data.putAll(
            backupEnvVarFactory.getSecretEnvVar(namespace, backupStorage)
        ));

    return Optional.of(new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name(context))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withType("Opaque")
        .withStringData(StackGresUtil.addMd5Sum(data))
        .build());
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Inject
  public void setBackupEnvVarFactory(BackupEnvVarFactory backupEnvVarFactory) {
    this.backupEnvVarFactory = backupEnvVarFactory;
  }
}
