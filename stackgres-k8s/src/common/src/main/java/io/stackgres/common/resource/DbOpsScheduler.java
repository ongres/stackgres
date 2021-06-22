/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.CdiUtil;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsList;

@ApplicationScoped
public class DbOpsScheduler
    extends AbstractCustomResourceScheduler<StackGresDbOps, StackGresDbOpsList> {

  @Inject
  public DbOpsScheduler(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresDbOps.class, StackGresDbOpsList.class);
  }

  public DbOpsScheduler() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }
}
