/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;

@ApplicationScoped
public class DistributedLogsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresDistributedLogs> {

  private final ResourceGenerationDiscoverer<DistributedLogsContext> generators;

  private final CustomResourceScanner<StackGresCluster> clusterScanner;

  private final ConnectedClustersScanner connectedClustersScanner;

  @Inject
  public DistributedLogsRequiredResourcesGenerator(
      ResourceGenerationDiscoverer<DistributedLogsContext> generators,
      CustomResourceScanner<StackGresCluster> clusterScanner,
      ConnectedClustersScanner connectedClustersScanner) {
    this.generators = generators;
    this.clusterScanner = clusterScanner;
    this.connectedClustersScanner = connectedClustersScanner;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresDistributedLogs config) {

    DistributedLogsContext context = ImmutableDistributedLogsContext.builder()
        .source(config)
        .addAllConnectedClusters(getConnectedClusters(config))
        .ownerReferences(List.of(ResourceUtil.getOwnerReference(config)))
        .build();

    return generators.getResourceGenerators(context)
        .stream().flatMap(generator -> generator.generateResource(context))
        .collect(Collectors.toUnmodifiableList());
  }

  private List<StackGresCluster> getConnectedClusters(
      StackGresDistributedLogs distributedLogs) {
    return connectedClustersScanner.getConnectedClusters(distributedLogs);
  }
}
