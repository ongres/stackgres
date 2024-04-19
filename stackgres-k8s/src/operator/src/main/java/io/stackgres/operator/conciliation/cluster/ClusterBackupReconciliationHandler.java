/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.FireAndForgetReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresCluster.class, kind = "SGBackup")
@ApplicationScoped
public class ClusterBackupReconciliationHandler
    extends FireAndForgetReconciliationHandler<StackGresCluster> {

  @Inject
  public ClusterBackupReconciliationHandler(
      @ReconciliationScope(value = StackGresCluster.class, kind = "HasMetadata")
      ReconciliationHandler<StackGresCluster> handler) {
    super(handler);
  }

  @Override
  protected boolean canForget(StackGresCluster context, HasMetadata resource) {
    return true;
  }

}
