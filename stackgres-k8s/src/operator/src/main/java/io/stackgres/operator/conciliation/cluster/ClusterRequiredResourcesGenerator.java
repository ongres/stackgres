/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.cluster.context.ClusterContextPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ClusterRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresCluster> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ClusterRequiredResourcesGenerator.class);

  private final StackGresContext context;

  private final ClusterContextPipeline contextPipeline;

  private final ResourceGenerationDiscoverer<StackGresClusterContext> discoverer;

  @Inject
  public ClusterRequiredResourcesGenerator(
      StackGresContext context,
      ClusterContextPipeline contextPipeline,
      ResourceGenerationDiscoverer<StackGresClusterContext> discoverer) {
    this.context = context;
    this.contextPipeline = contextPipeline;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresCluster cluster) {
    final StackGresClusterContext.Builder contextBuilder = StackGresClusterContext.builder();

    contextPipeline.appendContext(cluster, contextBuilder);

    contextBuilder
        .context(context)
        .source(cluster);

    return discoverer.generateResources(contextBuilder.build());
  }

}
