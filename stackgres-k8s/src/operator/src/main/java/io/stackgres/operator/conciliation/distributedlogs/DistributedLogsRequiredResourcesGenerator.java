/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.distributedlogs.context.DistributedLogsContextPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DistributedLogsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresDistributedLogs> {

  private final StackGresContext context;

  private final DistributedLogsContextPipeline contextPipeline;

  private final ResourceGenerationDiscoverer<StackGresDistributedLogsContext> discoverer;

  @Inject
  public DistributedLogsRequiredResourcesGenerator(
      StackGresContext context,
      DistributedLogsContextPipeline contextPipeline,
      ResourceGenerationDiscoverer<StackGresDistributedLogsContext> discoverer) {
    this.context = context;
    this.contextPipeline = contextPipeline;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresDistributedLogs distributedLogs) {
    StackGresDistributedLogsContext.Builder contextBuilder = StackGresDistributedLogsContext.builder()
        .context(context)
        .source(distributedLogs);

    contextPipeline.appendContext(distributedLogs, contextBuilder);

    return discoverer.generateResources(contextBuilder.build());
  }

}
