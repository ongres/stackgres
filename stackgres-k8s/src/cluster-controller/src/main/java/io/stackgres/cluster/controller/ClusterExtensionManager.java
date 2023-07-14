/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import io.stackgres.common.FileSystemHandler;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.extension.ExtensionManager;
import io.stackgres.common.extension.ExtensionMetadataManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterExtensionManager extends ExtensionManager {

  @Inject
  public ClusterExtensionManager(
      ExtensionMetadataManager extensionMetadataManager) {
    super(
        extensionMetadataManager,
        new WebClientFactory(), new FileSystemHandler());
  }

}
