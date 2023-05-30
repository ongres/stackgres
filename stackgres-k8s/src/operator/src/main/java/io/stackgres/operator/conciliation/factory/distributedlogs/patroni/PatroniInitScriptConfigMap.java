/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
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
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Unchecked;

@Singleton
@OperatorVersionBinder
public class PatroniInitScriptConfigMap implements
    VolumeFactory<StackGresDistributedLogsContext> {

  private final LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  @Inject
  public PatroniInitScriptConfigMap(LabelFactoryForCluster<StackGresDistributedLogs> labelFactory) {
    this.labelFactory = labelFactory;
  }

  public static String name(StackGresDistributedLogs distributedLogs) {
    final String clusterName = distributedLogs.getMetadata().getName();
    return StackGresVolume.INIT_SCRIPT.getResourceName(clusterName);
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
    return
        new VolumeBuilder()
            .withName(StackGresVolume.INIT_SCRIPT.getName())
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(name(context.getSource()))
                .withDefaultMode(0440)
                .withOptional(false)
                .build())
            .build();
  }

  public @NotNull HasMetadata buildSource(StackGresDistributedLogsContext context) {
    final StackGresDistributedLogs cluster = context.getSource();

    String data = Unchecked.supplier(() -> Resources
        .asCharSource(PatroniInitScriptConfigMap.class
                .getResource("/distributed-logs-template.sql"),
            StandardCharsets.UTF_8)
        .read()).get();
    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(cluster))
        .withLabels(labelFactory.genericLabels(cluster))
        .endMetadata()
        .withData(ImmutableMap.of("distributed-logs-template.sql", data))
        .build();
  }

}
