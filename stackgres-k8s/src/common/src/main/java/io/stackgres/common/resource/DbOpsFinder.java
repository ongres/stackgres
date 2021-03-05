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
public class DbOpsFinder
    extends AbstractCustomResourceFinder<StackGresDbOps> {

  /**
   * Create a {@code DbOpsFinder} instance.
   */
  @Inject
  public DbOpsFinder(KubernetesClientFactory clientFactory) {
    super(clientFactory, StackGresDbOps.class, StackGresDbOpsList.class);
  }

  public DbOpsFinder() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

}
