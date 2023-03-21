/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.AbstractRequiredResourceDecorator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.factory.DecoratorDiscoverer;

@ApplicationScoped
public class ShardedClusterRequiredResourceDecorator
    extends AbstractRequiredResourceDecorator<StackGresShardedClusterContext> {

  @Inject
  public ShardedClusterRequiredResourceDecorator(
      DecoratorDiscoverer<StackGresShardedClusterContext> decoratorDiscoverer,
      ResourceGenerationDiscoverer<StackGresShardedClusterContext> generators) {
    super(decoratorDiscoverer, generators);
  }

}
