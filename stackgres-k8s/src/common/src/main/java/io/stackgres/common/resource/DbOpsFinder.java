/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DbOpsFinder
    extends AbstractCustomResourceFinder<StackGresDbOps> {

  /**
   * Create a {@code DbOpsFinder} instance.
   */
  @Inject
  public DbOpsFinder(KubernetesClient client) {
    super(client, StackGresDbOps.class, StackGresDbOpsList.class);
  }

}
