/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni.v09;

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
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.Unchecked;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V09_LAST)
public class PatroniInitScriptConfigMap implements
    VolumeFactory<DistributedLogsContext> {

  private static final String VOLUME_NAME_FORMAT = "%s-00000-distributed-logs-template-template1";
  private final LabelFactory<StackGresDistributedLogs> labelFactory;

  @Inject
  public PatroniInitScriptConfigMap(LabelFactory<StackGresDistributedLogs> labelFactory) {
    this.labelFactory = labelFactory;
  }

  public static String name(StackGresDistributedLogs distributedLogs) {
    final String clusterName = distributedLogs.getMetadata().getName();
    return String.format(VOLUME_NAME_FORMAT, clusterName);
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
            .withName(name(context.getSource()))
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
                .getResource("/09-distributed-logs-template.sql"),
            StandardCharsets.UTF_8)
        .read()).get();
    return new ConfigMapBuilder()
        .withNewMetadata()
        .withNamespace(cluster.getMetadata().getNamespace())
        .withName(name(cluster))
        .withLabels(labelFactory.patroniClusterLabels(cluster))
        .withOwnerReferences(context.getOwnerReferences())
        .endMetadata()
        .withData(ImmutableMap.of("00000-distributed-logs-template.template1.sql", data))
        .build();
  }

}
