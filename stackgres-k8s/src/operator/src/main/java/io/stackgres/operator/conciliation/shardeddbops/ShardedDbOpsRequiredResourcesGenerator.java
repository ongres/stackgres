/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.shardeddbops.context.ShardedDbOpsContextPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShardedDbOpsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresShardedDbOps> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ShardedDbOpsRequiredResourcesGenerator.class);

  private final StackGresContext context;

  private final ShardedDbOpsContextPipeline contextPipeline;

  private final ResourceGenerationDiscoverer<StackGresShardedDbOpsContext> discoverer;

  @Inject
  public ShardedDbOpsRequiredResourcesGenerator(
      StackGresContext context,
      ShardedDbOpsContextPipeline contextPipeline,
      ResourceGenerationDiscoverer<StackGresShardedDbOpsContext> discoverer) {
    this.context = context;
    this.contextPipeline = contextPipeline;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresShardedDbOps dbOps) {
    StackGresShardedDbOpsContext.Builder contextBuilder = StackGresShardedDbOpsContext.builder()
        .context(context)
        .source(dbOps);

    contextPipeline.appendContext(dbOps, contextBuilder);

    return discoverer.generateResources(contextBuilder.build());
  }

}
