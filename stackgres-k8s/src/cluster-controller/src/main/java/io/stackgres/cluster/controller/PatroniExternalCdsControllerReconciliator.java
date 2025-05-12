/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.cluster.common.StackGresClusterContext;
import io.stackgres.common.CdiUtil;
import io.stackgres.operatorframework.reconciliation.ReconciliationResult;
import io.stackgres.operatorframework.reconciliation.Reconciliator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

@ApplicationScoped
public class PatroniExternalCdsControllerReconciliator
    extends Reconciliator<StackGresClusterContext> {

  private final PatroniEndpointsReconciliator patroniEndpointsReconciliator;
  private final PatroniLabelsReconciliator patroniLabelsReconciliator;

  @Inject
  public PatroniExternalCdsControllerReconciliator(Parameters parameters) {
    this.patroniEndpointsReconciliator = parameters.patroniEndpointsReconciliator;
    this.patroniLabelsReconciliator = parameters.patroniLabelsReconciliator;
  }

  public PatroniExternalCdsControllerReconciliator() {
    super();
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy(getClass());
    this.patroniEndpointsReconciliator = null;
    this.patroniLabelsReconciliator = null;
  }

  @Override
  public ReconciliationResult<?> reconcile(KubernetesClient client,
      StackGresClusterContext context) throws Exception {

    return patroniEndpointsReconciliator.reconcile(client, context)
        .join(patroniLabelsReconciliator.reconcile(client, context));
  }

  @Dependent
  public static class Parameters {
    @Inject PatroniEndpointsReconciliator patroniEndpointsReconciliator;
    @Inject PatroniLabelsReconciliator patroniLabelsReconciliator;
  }

}
