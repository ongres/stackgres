/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.PodSecurityContext;
import io.stackgres.operator.conciliation.factory.PodSecurityFactory;
import io.stackgres.operator.conciliation.factory.ResourceFactory;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import io.stackgres.operator.configuration.OperatorPropertyContext;

@ApplicationScoped
public class ShardedDbOpsPodSecurityFactory extends PodSecurityFactory
    implements ResourceFactory<StackGresShardedDbOpsContext, PodSecurityContext> {

  @Inject
  public ShardedDbOpsPodSecurityFactory(OperatorPropertyContext operatorContext) {
    super(operatorContext);
  }

  @Override
  public PodSecurityContext createResource(StackGresShardedDbOpsContext source) {
    return createPodSecurityContext();
  }

}
