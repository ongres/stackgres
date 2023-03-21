/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.resource;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.distributedlogs.common.StackGresDistributedLogsContext;

@ApplicationScoped
public class DefaultDistributedLogsResourceHandler
    extends AbstractDistributedLogsResourceHandler {

  private final LabelFactoryForCluster<StackGresDistributedLogs> labelFactory;

  @Inject
  public DefaultDistributedLogsResourceHandler(
      LabelFactoryForCluster<StackGresDistributedLogs> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public Stream<HasMetadata> getResources(KubernetesClient client,
                                          StackGresDistributedLogsContext context) {
    return STACKGRES_DISTRIBUTED_LOGS_RESOURCE_OPERATIONS.values()
        .stream()
        .flatMap(resourceOperationGetter -> resourceOperationGetter.apply(client)
            .inNamespace(context.getDistributedLogs().getMetadata().getNamespace())
            .withLabels(labelFactory.genericLabels(context.getDistributedLogs()))
            .list()
            .getItems()
            .stream());
  }

}
