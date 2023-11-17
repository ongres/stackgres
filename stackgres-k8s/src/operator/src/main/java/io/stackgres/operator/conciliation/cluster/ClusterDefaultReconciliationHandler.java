/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.AbstractReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ReconciliationScope(value = StackGresCluster.class, kind = "HasMetadata")
@ApplicationScoped
public class ClusterDefaultReconciliationHandler
    extends AbstractReconciliationHandler<StackGresCluster> {

  @Inject
  public ClusterDefaultReconciliationHandler(KubernetesClient client) {
    super(client);
  }

}
