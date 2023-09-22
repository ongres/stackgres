/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster.backup;

import java.util.Map;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.operator.conciliation.factory.AbstractTemplatesConfigMap;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class ShardedBackupTemplatesVolumeFactory
    extends AbstractTemplatesConfigMap<StackGresShardedClusterContext> {

  private LabelFactoryForShardedCluster labelFactory;

  public static String name(StackGresShardedClusterContext context) {
    final String clusterName = context.getSource().getMetadata().getName();
    return StackGresVolume.SCRIPT_TEMPLATES.getResourceName(clusterName);
  }

  @Inject
  public ShardedBackupTemplatesVolumeFactory(LabelFactoryForShardedCluster labelFactory) {
    super();
    this.labelFactory = labelFactory;
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresShardedClusterContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build());
  }

  private Volume buildVolume(StackGresShardedClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(name(context))
            .withDefaultMode(0444)
            .build())
        .build();
  }

  public @NotNull HasMetadata buildSource(StackGresShardedClusterContext context) {
    Map<String, String> data = getShardedClusterTemplates();

    final StackGresShardedCluster cluster = context.getSource();
    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withData(data)
        .build();
  }

}
