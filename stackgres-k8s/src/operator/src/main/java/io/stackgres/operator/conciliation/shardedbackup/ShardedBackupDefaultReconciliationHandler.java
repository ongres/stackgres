/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.conciliation.AbstractReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresShardedBackup.class, kind = "HasMetadata")
@ApplicationScoped
public class ShardedBackupDefaultReconciliationHandler
    extends AbstractReconciliationHandler<StackGresShardedBackup> {

  @Inject
  public ShardedBackupDefaultReconciliationHandler(KubernetesClient client) {
    super(client);
  }

}
