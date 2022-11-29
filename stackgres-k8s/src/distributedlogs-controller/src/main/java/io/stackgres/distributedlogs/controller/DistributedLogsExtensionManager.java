/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.distributedlogs.controller;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.FileSystemHandler;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.extension.ExtensionManager;

@ApplicationScoped
public class DistributedLogsExtensionManager extends ExtensionManager {

  @Inject
  public DistributedLogsExtensionManager(
      DistributedLogsExtensionMetadataManager distributedLogsExtensionMetadataManager) {
    super(
        distributedLogsExtensionMetadataManager,
        new WebClientFactory(), new FileSystemHandler());
  }

}
