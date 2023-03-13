/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.FileSystemHandler;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.extension.ExtensionManager;
import io.stackgres.common.extension.ExtensionMetadataManager;

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
