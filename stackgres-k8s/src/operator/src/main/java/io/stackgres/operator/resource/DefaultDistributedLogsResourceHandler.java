/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.common.StackGresDistributedLogsContext;

@ApplicationScoped
public class DefaultDistributedLogsResourceHandler
    extends AbstractDistributedLogsResourceHandler {

  @Override
  public Stream<HasMetadata> getResources(KubernetesClient client,
      StackGresDistributedLogsContext context) {
    return STACKGRES_DISTRIBUTED_LOGS_RESOURCE_OPERATIONS.values()
        .stream()
        .flatMap(resourceOperationGetter -> resourceOperationGetter.apply(client)
            .inNamespace(context.getDistributedLogs().getMetadata().getNamespace())
            .withLabels(context.clusterLabels())
            .list()
            .getItems()
            .stream());
  }

}
