/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.RequiredResourceDecorator;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;

@ApplicationScoped
public class DistributedLogsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresDistributedLogs> {

  private final ConnectedClustersScanner connectedClustersScanner;

  private final RequiredResourceDecorator<StackGresDistributedLogsContext> decorator;

  private final ResourceFinder<Secret> secretFinder;

  @Inject
  public DistributedLogsRequiredResourcesGenerator(
      RequiredResourceDecorator<StackGresDistributedLogsContext> decorator,
      ConnectedClustersScanner connectedClustersScanner,
      ResourceFinder<Secret> secretFinder) {
    this.decorator = decorator;
    this.connectedClustersScanner = connectedClustersScanner;
    this.secretFinder = secretFinder;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresDistributedLogs config) {
    final String distributedLogsName = config.getMetadata().getName();
    final String namespace = config.getMetadata().getNamespace();
    StackGresDistributedLogsContext context = ImmutableStackGresDistributedLogsContext.builder()
        .source(config)
        .addAllConnectedClusters(getConnectedClusters(config))
        .databaseCredentials(secretFinder.findByNameAndNamespace(distributedLogsName, namespace))
        .build();

    return decorator.decorateResources(context);
  }

  private List<StackGresCluster> getConnectedClusters(
      StackGresDistributedLogs distributedLogs) {
    return connectedClustersScanner.getConnectedClusters(distributedLogs);
  }
}
