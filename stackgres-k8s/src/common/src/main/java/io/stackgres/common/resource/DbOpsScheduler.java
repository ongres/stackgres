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
    if (resource.getMetadata().getResourceVersion() == null) {
      return super.update(resource);
    }
    return client.resources(StackGresDbOps.class, StackGresDbOpsList.class)
        .resource(resource)
        .lockResourceVersion(resource.getMetadata().getResourceVersion())
        .replace();
  }

}
