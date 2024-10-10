/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.cluster.controller;

import java.util.Map;

import io.stackgres.cluster.app.ClusterInstallationInfoHolder;
import io.stackgres.common.FileSystemHandler;
import io.stackgres.common.WebClientFactory;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.common.extension.LegacyExtensionManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterLegacyExtensionManager extends LegacyExtensionManager {

  @Inject
  public ClusterLegacyExtensionManager(
      ExtensionMetadataManager extensionMetadataManager,
      ClusterInstallationInfoHolder installationInfoHolder) {
    super(
        extensionMetadataManager,
        new WebClientFactory(),
        () -> Map.ofEntries(installationInfoHolder.getUserAgentHeaderEntry()),
        new FileSystemHandler());
  }

}
