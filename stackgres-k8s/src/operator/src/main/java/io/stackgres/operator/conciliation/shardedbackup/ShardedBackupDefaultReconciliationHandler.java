/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.conciliation.AbstractReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;

@ReconciliationScope(value = StackGresShardedBackup.class, kind = "HasMetadata")
@ApplicationScoped
public class ShardedBackupDefaultReconciliationHandler
    extends AbstractReconciliationHandler<StackGresShardedBackup> {

  @Inject
  public ShardedBackupDefaultReconciliationHandler(KubernetesClient client) {
    super(client);
  }

}
