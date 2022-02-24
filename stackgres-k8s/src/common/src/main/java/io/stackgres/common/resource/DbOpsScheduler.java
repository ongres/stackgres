/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsList;

@ApplicationScoped
public class DbOpsScheduler
    extends AbstractCustomResourceScheduler<StackGresDbOps, StackGresDbOpsList> {

  public DbOpsScheduler() {
    super(StackGresDbOps.class, StackGresDbOpsList.class);
  }

  @Override
  public StackGresDbOps update(StackGresDbOps resource) {
    return client.resources(StackGresDbOps.class, StackGresDbOpsList.class)
        .inNamespace(resource.getMetadata().getNamespace())
        .withName(resource.getMetadata().getName())
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .replace(resource);
  }

}
