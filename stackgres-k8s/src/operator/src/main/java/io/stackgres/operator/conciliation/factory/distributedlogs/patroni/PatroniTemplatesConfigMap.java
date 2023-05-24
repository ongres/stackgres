/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.AbstractPatroniTemplatesConfigMap;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumePair;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class PatroniTemplatesConfigMap
    extends AbstractPatroniTemplatesConfigMap<StackGresDistributedLogsContext> {

  private LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  private static String name(StackGresDistributedLogsContext context) {
    final String clusterName = context.getSource().getMetadata().getName();
    return StackGresVolume.SCRIPT_TEMPLATES.getResourceName(clusterName);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresDistributedLogsContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build()
    );
  }

  public @NotNull Volume buildVolume(StackGresDistributedLogsContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.SCRIPT_TEMPLATES.getName())
        .withConfigMap(new ConfigMapVolumeSourceBuilder()
            .withName(name(context))
            .withDefaultMode(0440)
            .withOptional(false)
            .build())
        .build();
  }

  public @NotNull HasMetadata buildSource(StackGresDistributedLogsContext context) {
    Map<String, String> data = getPatroniTemplates();

    final StackGresDistributedLogs cluster = context.getSource();

    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(StackGresVolume.SCRIPT_TEMPLATES.getResourceName(
            cluster.getMetadata().getName()))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withData(data)
        .build();
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresDistributedLogs> labelFactory) {
    this.labelFactory = labelFactory;
  }
}
