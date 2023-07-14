/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.AbstractReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresShardedCluster.class, kind = "HasMetadata")
@ApplicationScoped
public class ShardedClusterDefaultReconciliationHandler
    extends AbstractReconciliationHandler<StackGresShardedCluster> {

  @Inject
  public ShardedClusterDefaultReconciliationHandler(KubernetesClient client) {
    super(client);
  }

}
