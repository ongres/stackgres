/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedbackup;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.shardedbackup.context.ShardedBackupContextPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShardedBackupRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresShardedBackup> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ShardedBackupRequiredResourcesGenerator.class);

  private final ShardedBackupContextPipeline contextPipeline;

  private final ResourceGenerationDiscoverer<StackGresShardedBackupContext> discoverer;

  @Inject
  public ShardedBackupRequiredResourcesGenerator(
      ShardedBackupContextPipeline contextPipeline,
      ResourceGenerationDiscoverer<StackGresShardedBackupContext> discoverer) {
    this.contextPipeline = contextPipeline;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresShardedBackup backup) {
    var contextBuilder = StackGresShardedBackupContext.builder()
        .source(backup);

    contextPipeline.appendContext(backup, contextBuilder);

    return discoverer.generateResources(contextBuilder.build());
  }

}
