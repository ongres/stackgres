/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresDistributedLogs> {

  private final CustomResourceScanner<StackGresConfig> configScanner;

  private final ConnectedClustersScanner connectedClustersScanner;

  private final ResourceGenerationDiscoverer<StackGresDistributedLogsContext> discoverer;

  private final ResourceFinder<Secret> secretFinder;

  @Inject
  public DistributedLogsRequiredResourcesGenerator(
      CustomResourceScanner<StackGresConfig> configScanner,
      ResourceGenerationDiscoverer<StackGresDistributedLogsContext> discoverer,
      ConnectedClustersScanner connectedClustersScanner,
      ResourceFinder<Secret> secretFinder) {
    this.configScanner = configScanner;
    this.discoverer = discoverer;
    this.connectedClustersScanner = connectedClustersScanner;
    this.secretFinder = secretFinder;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresDistributedLogs distributedLogs) {
    final String distributedLogsName = distributedLogs.getMetadata().getName();
    final String namespace = distributedLogs.getMetadata().getNamespace();

    final StackGresConfig config = configScanner.findResources()
        .stream()
        .filter(list -> list.size() == 1)
        .flatMap(List::stream)
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            "SGConfig not found or more than one exists. Aborting reoconciliation!"));

    StackGresDistributedLogsContext context = ImmutableStackGresDistributedLogsContext.builder()
        .config(config)
        .source(distributedLogs)
        .addAllConnectedClusters(getConnectedClusters(distributedLogs))
        .databaseCredentials(secretFinder.findByNameAndNamespace(distributedLogsName, namespace))
        .build();

    return discoverer.generateResources(context);
  }

  private List<StackGresCluster> getConnectedClusters(
      StackGresDistributedLogs distributedLogs) {
    return connectedClustersScanner.getConnectedClusters(distributedLogs);
  }
}
