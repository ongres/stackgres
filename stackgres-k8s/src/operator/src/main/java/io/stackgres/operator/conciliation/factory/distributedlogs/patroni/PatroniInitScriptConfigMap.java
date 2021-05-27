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
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.conciliation.factory.distributedlogs.StatefulSetDynamicVolumes;
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Unchecked;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class PatroniInitScriptConfigMap implements
    VolumeFactory<DistributedLogsContext> {

  private final LabelFactory<StackGresDistributedLogs> labelFactory;

  @Inject
  public PatroniInitScriptConfigMap(LabelFactory<StackGresDistributedLogs> labelFactory) {
    this.labelFactory = labelFactory;
  }

  public static String name(StackGresDistributedLogs distributedLogs) {
    final String clusterName = distributedLogs.getMetadata().getName();
    return StatefulSetDynamicVolumes.INIT_SCRIPT.getResourceName(clusterName);
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(DistributedLogsContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build()
    );
  }

  public @NotNull Volume buildVolume(DistributedLogsContext context) {
    return
        new VolumeBuilder()
            .withName(StatefulSetDynamicVolumes.INIT_SCRIPT.getVolumeName())
            .withConfigMap(new ConfigMapVolumeSourceBuilder()
                .withName(name(context.getSource()))
                .withDefaultMode(420)
                .withOptional(false)
                .build())
            .build();
  }

  public @NotNull HasMetadata buildSource(DistributedLogsContext context) {
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
        .withLabels(labelFactory.patroniClusterLabels(cluster))
        .withOwnerReferences(context.getOwnerReferences())
        .endMetadata()
        .withData(ImmutableMap.of("distributed-logs-template.sql", data))
        .build();
  }

}
