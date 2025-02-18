/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
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

  private final ResourceGenerationDiscoverer<StackGresConfigContext> discoverer;

  @Inject
  public ConfigRequiredResourcesGenerator(
      ConfigContextPipeline contextPipeline,
      ResourceGenerationDiscoverer<StackGresConfigContext> discoverer) {
    this.contextPipeline = contextPipeline;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresConfig config) {
    StackGresConfigContext.Builder contextBuilder = StackGresConfigContext.builder()
        .source(config);

    contextPipeline.appendContext(config, contextBuilder);

    return discoverer.generateResources(contextBuilder.build());
  }

}
