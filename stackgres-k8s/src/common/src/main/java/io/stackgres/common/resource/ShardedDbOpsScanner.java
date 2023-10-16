/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOpsList;

@ApplicationScoped
public class ShardedDbOpsScanner
    extends AbstractCustomResourceScanner<StackGresShardedDbOps, StackGresShardedDbOpsList> {

  /**
   * Create a {@code ShardedDbOpsScanner} instance.
   */
  @Inject
  public ShardedDbOpsScanner(KubernetesClient client) {
    super(client, StackGresShardedDbOps.class, StackGresShardedDbOpsList.class);
  }

}
