/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.LabelFactory;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.DeployedResourcesScanner;
import io.stackgres.operator.conciliation.ReconciliationOperations;

@ApplicationScoped
public class DistributedLogsResourceScanner
    implements DeployedResourcesScanner<StackGresDistributedLogs>,
    ReconciliationOperations {

  private final KubernetesClientFactory clientFactory;
  private final LabelFactory<StackGresDistributedLogs> labelFactory;

  @Inject
  public DistributedLogsResourceScanner(KubernetesClientFactory clientFactory,
                                        LabelFactory<StackGresDistributedLogs> labelFactory) {
    this.clientFactory = clientFactory;
    this.labelFactory = labelFactory;
  }

  @Override
  public List<HasMetadata> getDeployedResources(StackGresDistributedLogs config) {
    try (KubernetesClient client = clientFactory.create()) {

      final String namespace = config.getMetadata().getNamespace();
      List<HasMetadata> resources = STACKGRES_CLUSTER_RESOURCE_OPERATIONS.values()
          .stream()
          .flatMap(resourceOperationGetter -> resourceOperationGetter.apply(client)
              .inNamespace(namespace)
              .withLabels(labelFactory.genericClusterLabels(config))
              .list()
              .getItems()
              .stream())
          .filter(resource -> resource.getMetadata().getOwnerReferences()
              .stream().anyMatch(ownerReference -> ownerReference.getKind()
                  .equals(StackGresDistributedLogs.KIND)
                  && ownerReference.getName().equals(config.getMetadata().getName())))
          .collect(Collectors.toUnmodifiableList());
      return resources;

    }
  }
}
