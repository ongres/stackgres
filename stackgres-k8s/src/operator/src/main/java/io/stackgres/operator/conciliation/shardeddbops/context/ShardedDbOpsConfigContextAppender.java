/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops.context;

import java.util.List;

import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.ContextAppender;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ShardedDbOpsConfigContextAppender
    extends ContextAppender<StackGresShardedDbOps, Builder> {

  private final CustomResourceScanner<StackGresConfig> configScanner;

  public ShardedDbOpsConfigContextAppender(CustomResourceScanner<StackGresConfig> configScanner) {
    this.configScanner = configScanner;
  }

  @Override
  public void appendContext(StackGresShardedDbOps cluster, Builder contextBuilder) {
    final StackGresConfig config = configScanner.findResources()
        .stream()
        .filter(list -> list.size() == 1)
        .flatMap(List::stream)
        .findAny()
        .orElseThrow(() -> new IllegalArgumentException(
            StackGresConfig.KIND + " not found or more than one exists. Aborting reoconciliation!"));
    contextBuilder.config(config);
  }

}
