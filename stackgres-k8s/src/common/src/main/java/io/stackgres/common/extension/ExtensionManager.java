/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.extension;

import java.io.InputStream;

import io.stackgres.common.ClusterContext;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;

public interface ExtensionManager {

  ExtensionInstaller getExtensionInstaller(
      ClusterContext context,
      StackGresClusterInstalledExtension installedExtension) throws Exception;

  ExtensionUninstaller getExtensionUninstaller(
      ClusterContext context,
      StackGresClusterInstalledExtension installedExtension) throws Exception;

  StackGresClusterInstalledExtension getInstalledExtension(
      StackGresCluster cluster,
      StackGresClusterExtension clusterExtension);

  public interface ExtensionInstaller {

    boolean isExtensionInstalled() throws Exception;

    boolean areLinksCreated() throws Exception;

    boolean doesInstallOverwriteAnySharedFile() throws Exception;

    boolean doesInstallOverwriteAnySharedFile(InputStream inputStream) throws Exception;

    void installExtension() throws Exception;

    void createExtensionLinks() throws Exception;

    boolean isExtensionPendingOverwrite();

    void setExtensionAsPending() throws Exception;

    ExtensionPuller getPuller() throws Exception;

  }

  public interface ExtensionPuller {

    void downloadAndExtract() throws Exception;

    void verify() throws Exception;

  }

  public interface ExtensionUninstaller {

    boolean isExtensionInstalled() throws Exception;

    void uninstallExtension() throws Exception;

  }

}
