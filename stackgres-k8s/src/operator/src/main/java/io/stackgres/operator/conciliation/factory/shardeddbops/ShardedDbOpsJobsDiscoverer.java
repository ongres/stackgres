/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import io.stackgres.operator.conciliation.AbstractDiscoverer;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;

@ApplicationScoped
public class ShardedDbOpsJobsDiscoverer
    extends AbstractDiscoverer<ShardedDbOpsJobFactory> {

  @Inject
  public ShardedDbOpsJobsDiscoverer(
      @ShardedDbOpsJob Instance<ShardedDbOpsJobFactory> instance) {
    super(instance);
  }

  public Map<String, ShardedDbOpsJobFactory> discoverFactories(
      StackGresShardedDbOpsContext context) {
    return hub.get(context.getVersion()).stream()
        .collect(Collectors.toMap(
            dbop -> getAnnotation(dbop, ShardedDbOpsJob.class).value(),
            Function.identity()));
  }
}
