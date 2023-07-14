/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.replicate;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
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
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupEnvVarFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class ReplicateSecret
    implements VolumeFactory<StackGresClusterContext> {

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  private BackupEnvVarFactory envVarFactory;

  public static String name(ClusterContext context) {
    final String clusterName = context.getCluster().getMetadata().getName();
    return StackGresVolume.REPLICATE_CREDENTIALS.getResourceName(clusterName);
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
        .withName(StackGresVolume.REPLICATE_CREDENTIALS.getName())
        .withSecret(new SecretVolumeSourceBuilder()
            .withSecretName(name(context))
            .build())
        .build();
  }

  private Secret buildSource(StackGresClusterContext context) {
    Map<String, String> data = new HashMap<>();
    StackGresCluster cluster = context.getSource();
    final String namespace = cluster.getMetadata().getNamespace();

    context.getReplicateStorage().ifPresent(
        backupStorage -> data.putAll(
            envVarFactory.getSecretEnvVar(namespace, backupStorage)
        ));

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
