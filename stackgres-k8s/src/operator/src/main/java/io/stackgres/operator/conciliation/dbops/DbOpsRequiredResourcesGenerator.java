/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.dbops.context.DbOpsContextPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DbOpsRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresDbOps> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(DbOpsRequiredResourcesGenerator.class);

  private final DbOpsContextPipeline contextPipeline;

  private final ResourceGenerationDiscoverer<StackGresDbOpsContext> discoverer;

  @Inject
  public DbOpsRequiredResourcesGenerator(
      DbOpsContextPipeline contextPipeline,
      ResourceGenerationDiscoverer<StackGresDbOpsContext> discoverer) {
    this.contextPipeline = contextPipeline;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresDbOps dbOps) {
    StackGresDbOpsContext.Builder contextBuilder = StackGresDbOpsContext.builder()
        .source(dbOps);

    contextPipeline.appendContext(dbOps, contextBuilder);

    return discoverer.generateResources(contextBuilder.build());
  }

}
