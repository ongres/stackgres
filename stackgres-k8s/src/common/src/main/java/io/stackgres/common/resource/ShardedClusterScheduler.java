/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterList;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterScheduler extends
    AbstractCustomResourceScheduler<StackGresShardedCluster, StackGresShardedClusterList> {

  public ShardedClusterScheduler() {
    super(StackGresShardedCluster.class, StackGresShardedClusterList.class);
  }

  @Override
  public StackGresShardedCluster update(StackGresShardedCluster resource) {
    if (resource.getMetadata().getResourceVersion() == null) {
      return super.update(resource);
    }
    return client.resources(StackGresShardedCluster.class, StackGresShardedClusterList.class)
        .resource(resource)
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .update();
  }

}
