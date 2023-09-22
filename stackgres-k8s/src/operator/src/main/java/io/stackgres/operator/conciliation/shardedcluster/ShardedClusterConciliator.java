/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.AbstractConciliator;
import io.stackgres.operator.conciliation.AbstractDeployedResourcesScanner;
import io.stackgres.operator.conciliation.DeployedResourcesCache;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;

@ApplicationScoped
public class ShardedClusterConciliator extends AbstractConciliator<StackGresShardedCluster> {

  @Inject
  public ShardedClusterConciliator(
      KubernetesClient client,
      RequiredResourceGenerator<StackGresShardedCluster> requiredResourceGenerator,
      AbstractDeployedResourcesScanner<StackGresShardedCluster> deployedResourcesScanner,
      DeployedResourcesCache deployedResourcesCache) {
    super(client, requiredResourceGenerator, deployedResourcesScanner, deployedResourcesCache);
  }

}
