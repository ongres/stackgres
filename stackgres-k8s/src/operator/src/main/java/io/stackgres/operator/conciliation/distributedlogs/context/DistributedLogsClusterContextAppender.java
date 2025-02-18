/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs.context;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DistributedLogsClusterContextAppender
    extends ContextAppender<StackGresDistributedLogs, Builder> {

  private final CustomResourceFinder<StackGresCluster> clusterFinder;

  public DistributedLogsClusterContextAppender(CustomResourceFinder<StackGresCluster> clusterFinder) {
    this.clusterFinder = clusterFinder;
  }

  @Override
  public void appendContext(StackGresDistributedLogs distributedLogs, Builder contextBuilder) {
    final Optional<StackGresCluster> foundCluster = clusterFinder
        .findByNameAndNamespace(
            distributedLogs.getMetadata().getName(),
            distributedLogs.getMetadata().getNamespace());

    contextBuilder.cluster(foundCluster);
  }

}
