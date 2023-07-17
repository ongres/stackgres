/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.conciliation.AbstractReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresDbOps.class, kind = "HasMetadata")
@ApplicationScoped
public class DbOpsDefaultReconciliationHandler
    extends AbstractReconciliationHandler<StackGresDbOps> {

  @Inject
  public DbOpsDefaultReconciliationHandler(KubernetesClient client) {
    super(client);
  }

}
