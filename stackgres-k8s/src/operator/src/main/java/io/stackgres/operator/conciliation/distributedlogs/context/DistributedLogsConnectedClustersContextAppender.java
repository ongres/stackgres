/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs.context;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatusBuilder;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatusClusterBuilder;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DistributedLogsConnectedClustersContextAppender
    extends ContextAppender<StackGresDistributedLogs, Builder> {

  private final CustomResourceScanner<StackGresCluster> clusterScanner;

  public DistributedLogsConnectedClustersContextAppender(CustomResourceScanner<StackGresCluster> clusterScanner) {
    this.clusterScanner = clusterScanner;
  }

  @Override
  public void appendContext(StackGresDistributedLogs distributedLogs, Builder contextBuilder) {
    final List<StackGresCluster> connectedClusters = getConnectedClusters(distributedLogs);
    distributedLogs.setStatus(
        new StackGresDistributedLogsStatusBuilder(distributedLogs.getStatus())
        .withConnectedClusters(connectedClusters
            .stream()
            .map(cluster -> new StackGresDistributedLogsStatusClusterBuilder()
                .withNamespace(cluster.getMetadata().getNamespace())
                .withName(cluster.getMetadata().getName())
                .withConfig(cluster.getSpec().getDistributedLogs())
                .build())
            .toList())
        .build());
    contextBuilder.connectedClusters(connectedClusters);
  }

  private List<StackGresCluster> getConnectedClusters(StackGresDistributedLogs config) {
    final String namespace = config.getMetadata().getNamespace();
    final String name = config.getMetadata().getName();
    return clusterScanner.getResources()
        .stream()
        .filter(cluster -> Optional.ofNullable(cluster.getSpec().getDistributedLogs())
            .map(StackGresClusterDistributedLogs::getSgDistributedLogs)
            .map(distributedLogsRelativeId -> StackGresUtil.getNamespaceFromRelativeId(
                distributedLogsRelativeId,
                cluster.getMetadata().getNamespace()).equals(namespace)
                && StackGresUtil.getNameFromRelativeId(
                distributedLogsRelativeId).equals(name))
            .orElse(false))
        .collect(Collectors.toUnmodifiableList());
  }

}
