/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import java.net.URI;

import io.stackgres.common.DistributedLogsControllerProperty;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.distributedlogs.configuration.DistributedLogsControllerPropertyContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class DistributedLogsExtensionMetadataManager extends ExtensionMetadataManager {

  @Inject
  public DistributedLogsExtensionMetadataManager(
      DistributedLogsControllerPropertyContext propertyContext) {
    super(
        new WebClientFactory(),
        Seq.of(propertyContext.getStringArray(DistributedLogsControllerProperty
            .DISTRIBUTEDLOGS_CONTROLLER_EXTENSIONS_REPOSITORY_URLS))
            .map(URI::create)
            .toList());
  }

}
