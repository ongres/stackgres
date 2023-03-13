/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterList;

@ApplicationScoped
public class ShardedClusterScanner
    extends AbstractCustomResourceScanner<StackGresShardedCluster, StackGresShardedClusterList> {

  /**
   * Create a {@code ClusterScanner} instance.
   */
  @Inject
  public ShardedClusterScanner(KubernetesClient client) {
    super(client, StackGresShardedCluster.class, StackGresShardedClusterList.class);
  }

}
