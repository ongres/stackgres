/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.conciliation.AbstractReconciliationHandler;
import io.stackgres.operator.conciliation.ReconciliationScope;

@ReconciliationScope(value = StackGresConfig.class, kind = "HasMetadata")
@ApplicationScoped
public class ConfigDefaultReconciliationHandler
    extends AbstractReconciliationHandler<StackGresConfig> {

  @Inject
  public ConfigDefaultReconciliationHandler(KubernetesClient client) {
    super(client);
  }

}
