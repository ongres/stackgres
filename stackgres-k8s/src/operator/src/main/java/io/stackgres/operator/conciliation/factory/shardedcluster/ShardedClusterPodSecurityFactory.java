/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.common.PodSecurityFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedClusterPodSecurityFactory extends PodSecurityFactory
    implements ResourceFactory<StackGresShardedClusterContext, PodSecurityContext> {

  @Override
  public PodSecurityContext createResource(StackGresShardedClusterContext source) {
    return createPodSecurityContext();
  }

}
