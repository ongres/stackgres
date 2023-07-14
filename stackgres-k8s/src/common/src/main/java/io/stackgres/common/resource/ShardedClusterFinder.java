/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedClusterFinder extends AbstractCustomResourceFinder<StackGresShardedCluster> {

  @Inject
  public ShardedClusterFinder(KubernetesClient client) {
    super(client, StackGresShardedCluster.class, StackGresShardedClusterList.class);
  }

}
