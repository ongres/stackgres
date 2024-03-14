/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.Map;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.conciliation.factory.AbstractTemplatesVolumeFactory;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
@OperatorVersionBinder
public class PatroniTemplatesConfigMap
    extends AbstractTemplatesVolumeFactory
    implements VolumeFactory<StackGresDistributedLogsContext> {

  private LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

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
    return buildVolumeForDistributedLogs(context);
  }

  public @NotNull HasMetadata buildSource(StackGresDistributedLogsContext context) {
    Map<String, String> data = getClusterTemplates();

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
