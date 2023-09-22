/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.operator.conciliation.factory.PodSecurityFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operator.configuration.OperatorPropertyContext;

@ApplicationScoped
public class ShardedClusterPodSecurityFactory extends PodSecurityFactory
    implements ResourceFactory<StackGresShardedClusterContext, PodSecurityContext> {

  @Inject
  public ShardedClusterPodSecurityFactory(OperatorPropertyContext operatorContext) {
    super(operatorContext);
  }

  @Override
  public PodSecurityContext createResource(StackGresShardedClusterContext source) {
    return createPodSecurityContext();
  }

}
