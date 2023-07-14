/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.EndpointsBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class PatroniSync implements ResourceGenerator<StackGresDistributedLogsContext> {

  private final LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  @Inject
  public PatroniSync(LabelFactoryForCluster<StackGresDistributedLogs> labelFactory) {
    this.labelFactory = labelFactory;
  }

  private static String name(StackGresDistributedLogs cluster) {
    return PatroniUtil.syncName(cluster);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDistributedLogsContext context) {
    StackGresDistributedLogs cluster = context.getSource();
    return Stream.of(
        new EndpointsBuilder()
            .withNewMetadata()
            .withNamespace(cluster.getMetadata().getNamespace())
            .withName(name(cluster))
            .addToLabels(labelFactory.clusterLabels(context.getSource()))
            .endMetadata()
            .build()
    );
  }

}
