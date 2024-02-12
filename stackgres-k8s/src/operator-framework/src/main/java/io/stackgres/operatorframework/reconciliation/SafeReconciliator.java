/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.reconciliation;

import io.fabric8.kubernetes.client.KubernetesClient;

public abstract class SafeReconciliator<T, S> extends Reconciliator<T> {

  @Override
  public final ReconciliationResult<S> reconcile(KubernetesClient client, T context)
      throws Exception {
    try {
      return safeReconcile(client, context);
    } catch (Exception ex) {
      return new ReconciliationResult<>(ex);
    }
  }

  public abstract ReconciliationResult<S> safeReconcile(KubernetesClient client, T context)
      throws Exception;

}
