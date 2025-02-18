/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.backup;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.conciliation.RequiredResourceGenerator;
import io.stackgres.operator.conciliation.ResourceGenerationDiscoverer;
import io.stackgres.operator.conciliation.backup.context.BackupContextPipeline;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BackupRequiredResourcesGenerator
    implements RequiredResourceGenerator<StackGresBackup> {

  protected static final Logger LOGGER = LoggerFactory
      .getLogger(BackupRequiredResourcesGenerator.class);

  private final BackupContextPipeline contextPipeline;

  private final ResourceGenerationDiscoverer<StackGresBackupContext> discoverer;

  @Inject
  public BackupRequiredResourcesGenerator(
      BackupContextPipeline contextPipeline,
      ResourceGenerationDiscoverer<StackGresBackupContext> discoverer) {
    this.contextPipeline = contextPipeline;
    this.discoverer = discoverer;
  }

  @Override
  public List<HasMetadata> getRequiredResources(StackGresBackup backup) {
    var contextBuilder = StackGresBackupContext.builder()
        .source(backup);

    contextPipeline.appendContext(backup, contextBuilder);

    return discoverer.generateResources(contextBuilder.build());
  }

}
