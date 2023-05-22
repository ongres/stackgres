/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
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
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class MajorVersionUpgradeConfigMap implements VolumeFactory<StackGresClusterContext> {

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  public static String name(ClusterContext clusterContext) {
    return StackGresVolume.POSTGRES_CONFIG
        .getResourceName(clusterContext.getCluster().getMetadata().getName());
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
        .withName(StackGresVolume.POSTGRES_CONFIG.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(name(context))
            .withDefaultMode(444)
            .build())
        .build();
  }

  public @NotNull HasMetadata buildSource(StackGresClusterContext context) {
    Map<String, String> data = new HashMap<>();

    data.put("postgresql.conf",
        StackGresUtil.toPlainPostgresConfig(
            context.getPostgresConfig()
            .getSpec().getPostgresqlConf()));

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(context.getCluster().getMetadata().getNamespace())
        .withName(name(context))
        .withLabels(labelFactory.genericLabels(context.getCluster()))
        .endMetadata()
        .withData(StackGresUtil.addMd5Sum(data))
        .build();
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

}
