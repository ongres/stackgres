/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;

public interface ExtensionEventEmitter {

  void emitExtensionDownloading(StackGresClusterInstalledExtension extension);

  void emitExtensionDeployed(StackGresClusterInstalledExtension extension);

  void emitExtensionDeployedRestart(StackGresClusterInstalledExtension extension);

  void emitExtensionChanged(StackGresClusterInstalledExtension oldExtension,
                            StackGresClusterInstalledExtension newVersion);

  void emitExtensionRemoved(StackGresClusterInstalledExtension extension);
}
