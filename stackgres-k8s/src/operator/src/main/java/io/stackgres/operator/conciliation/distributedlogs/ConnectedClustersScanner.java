/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.CustomResourceScanner;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConnectedClustersScanner {

  private final CustomResourceScanner<StackGresCluster> clusterScanner;

  @Inject
  public ConnectedClustersScanner(CustomResourceScanner<StackGresCluster> clusterScanner) {
    this.clusterScanner = clusterScanner;
  }

  public List<StackGresCluster> getConnectedClusters(StackGresDistributedLogs config) {
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
