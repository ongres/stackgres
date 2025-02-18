/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.stream.context.StreamContextPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class StreamRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresStream> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(StreamRequiredResourcesGenerator.class);

  private final StreamContextPipeline contextPipeline;

  private final ResourceGenerationDiscoverer<StackGresStreamContext> discoverer;

  @Inject
  public StreamRequiredResourcesGenerator(
      StreamContextPipeline contextPipeline,
      ResourceGenerationDiscoverer<StackGresStreamContext> discoverer) {
    this.contextPipeline = contextPipeline;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresStream stream) {
    StackGresStreamContext.Builder contextBuilder = StackGresStreamContext.builder()
        .source(stream);

    contextPipeline.appendContext(stream, contextBuilder);

    return discoverer.generateResources(contextBuilder.build());
  }

}
