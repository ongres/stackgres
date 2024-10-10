/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.common.PodSecurityFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedDbOpsPodSecurityFactory extends PodSecurityFactory
    implements ResourceFactory<StackGresShardedDbOpsContext, PodSecurityContext> {

  @Override
  public PodSecurityContext createResource(StackGresShardedDbOpsContext source) {
    return createPodSecurityContext();
  }

}
