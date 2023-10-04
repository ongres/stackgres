/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.conciliation.AbstractReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;

@ReconciliationScope(value = StackGresShardedDbOps.class, kind = "HasMetadata")
@ApplicationScoped
public class ShardedDbOpsDefaultReconciliationHandler
    extends AbstractReconciliationHandler<StackGresShardedDbOps> {

  @Inject
  public ShardedDbOpsDefaultReconciliationHandler(KubernetesClient client) {
    super(client);
  }

}
