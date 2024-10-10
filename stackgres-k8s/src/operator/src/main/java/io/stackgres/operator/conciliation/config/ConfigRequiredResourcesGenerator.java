/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.component.StackGresContext;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.config.context.ConfigContextPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ConfigRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresConfig> {

  private final ConfigContextPipeline contextPipeline;

  private final StackGresContext context;

  private final ResourceGenerationDiscoverer<StackGresConfigContext> discoverer;

  @Inject
  public ConfigRequiredResourcesGenerator(
      StackGresContext context,
      ConfigContextPipeline contextPipeline,
      ResourceGenerationDiscoverer<StackGresConfigContext> discoverer) {
    this.context = context;
    this.contextPipeline = contextPipeline;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresConfig config) {
    StackGresConfigContext.Builder contextBuilder = StackGresConfigContext.builder()
        .context(context)
        .source(config);

    contextPipeline.appendContext(config, contextBuilder);

    return discoverer.generateResources(contextBuilder.build());
  }

}
