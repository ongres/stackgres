/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsList;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ShardedDbOpsFinder
    extends AbstractCustomResourceFinder<StackGresShardedDbOps> {

  /**
   * Create a {@code ShardedDbOpsFinder} instance.
   */
  @Inject
  public ShardedDbOpsFinder(KubernetesClient client) {
    super(client, StackGresShardedDbOps.class, StackGresShardedDbOpsList.class);
  }

}
