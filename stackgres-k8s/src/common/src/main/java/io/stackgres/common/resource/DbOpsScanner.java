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
public class DbOpsScanner
    extends AbstractCustomResourceScanner<StackGresDbOps, StackGresDbOpsList> {

  @Inject
  public DbOpsScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory,
        StackGresDbOps.class, StackGresDbOpsList.class);
  }

  public DbOpsScanner() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
