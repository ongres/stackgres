/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operatorframework.reconciliation;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Reconciliator<T> {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  public abstract ReconciliationResult<?> reconcile(KubernetesClient client, T context)
      throws Exception;

}
