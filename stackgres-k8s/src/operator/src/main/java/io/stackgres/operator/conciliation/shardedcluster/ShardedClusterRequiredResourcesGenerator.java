/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.shardedcluster.context.ShardedClusterContextPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShardedClusterRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresShardedCluster> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ShardedClusterRequiredResourcesGenerator.class);

  private final ShardedClusterContextPipeline contextPipeline;

  private final ResourceGenerationDiscoverer<StackGresShardedClusterContext> discoverer;

  @Inject
  public ShardedClusterRequiredResourcesGenerator(
      ShardedClusterContextPipeline contextPipeline,
      ResourceGenerationDiscoverer<StackGresShardedClusterContext> discoverer) {
    this.contextPipeline = contextPipeline;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresShardedCluster cluster) {
    StackGresShardedClusterContext.Builder contextBuilder = StackGresShardedClusterContext.builder()
        .source(cluster);

    contextPipeline.appendContext(cluster, contextBuilder);

    return discoverer.generateResources(contextBuilder.build());
  }

}
