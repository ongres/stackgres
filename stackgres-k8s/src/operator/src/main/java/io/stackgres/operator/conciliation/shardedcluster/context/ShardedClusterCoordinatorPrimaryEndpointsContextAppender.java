/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterCoordinatorPrimaryEndpointsContextAppender {

  private final ResourceFinder<Endpoints> endpointsFinder;

  public ShardedClusterCoordinatorPrimaryEndpointsContextAppender(ResourceFinder<Endpoints> endpointsFinder) {
    this.endpointsFinder = endpointsFinder;
  }

  public void appendContext(StackGresCluster coordinator, Builder contextBuilder) {
    Optional<Endpoints> coordinatorPrimaryEndpoints = endpointsFinder
        .findByNameAndNamespace(
            PatroniUtil.readWriteName(coordinator),
            coordinator.getMetadata().getNamespace());
    contextBuilder.coordinatorPrimaryEndpoints(coordinatorPrimaryEndpoints);
  }

}
