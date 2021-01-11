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
import io.stackgres.common.crd.sgdbops.StackGresDbOpsDefinition;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsDoneable;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsList;

@ApplicationScoped
public class DbOpsScanner
    extends AbstractCustomResourceScanner<StackGresDbOps, StackGresDbOpsList,
    StackGresDbOpsDoneable> {

  @Inject
  public DbOpsScanner(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresDbOpsDefinition.CONTEXT,
        StackGresDbOps.class, StackGresDbOpsList.class,
        StackGresDbOpsDoneable.class);
  }

  public DbOpsScanner() {
    super(null, null, null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
