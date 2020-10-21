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
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresVersion;
import io.stackgres.operator.conciliation.distributedlogs.DistributedLogsContext;
import org.jooq.lambda.Unchecked;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V10)
public class PatroniInitScriptConfigMap implements
    ResourceGenerator<DistributedLogsContext> {

  private final LabelFactory<StackGresDistributedLogs> labelFactory;

  @Inject
  public PatroniInitScriptConfigMap(LabelFactory<StackGresDistributedLogs> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(DistributedLogsContext context) {
    final StackGresDistributedLogs cluster = context.getSource();

    String data = Unchecked.supplier(() -> Resources
        .asCharSource(PatroniInitScriptConfigMap.class
                .getResource("/distributed-logs-template.sql"),
            StandardCharsets.UTF_8)
        .read()).get();
    return Stream.of(new ConfigMapBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName("distributed-logs-template")
            .withLabels(labelFactory.patroniClusterLabels(cluster))
            .withOwnerReferences(context.getOwnerReferences())
            .endMetadata()
            .withData(ImmutableMap.of("distributed-logs-template.sql", data))
            .build());
  }
}
