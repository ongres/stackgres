/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsList;

@ApplicationScoped
public class DbOpsScheduler
    extends AbstractCustomResourceScheduler<StackGresDbOps, StackGresDbOpsList> {

  @Inject
  public DbOpsScheduler(KubernetesClient client) {
    super(client, StackGresDbOps.class, StackGresDbOpsList.class);
  }

  public DbOpsScheduler() {
    super(null, null, null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
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
