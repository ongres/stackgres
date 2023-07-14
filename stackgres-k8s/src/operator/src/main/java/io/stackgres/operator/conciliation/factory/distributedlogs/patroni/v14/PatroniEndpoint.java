/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni.v14;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder(stopAt = StackGresVersion.V_1_4)
public class PatroniEndpoint implements ResourceGenerator<StackGresDistributedLogsContext> {

  private final LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  @Inject
  public PatroniEndpoint(LabelFactoryForCluster<StackGresDistributedLogs> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    StackGresDistributedLogs cluster = context.getSource();
    return Stream.of(
        new EndpointsBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(PatroniUtil.readWriteName(cluster))
            .withLabels(labelFactory.clusterLabels(cluster))
            .endMetadata()
            .build()
    );
  }
}
