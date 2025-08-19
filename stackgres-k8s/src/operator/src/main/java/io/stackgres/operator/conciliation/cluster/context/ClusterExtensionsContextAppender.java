/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.operator.conciliation.AbstractExtensionsContextAppender;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClusterExtensionsContextAppender extends AbstractExtensionsContextAppender<StackGresCluster, Builder> {

  private final ExtensionMetadataManager extensionMetadataManager;

  @Inject
  public ClusterExtensionsContextAppender(ExtensionMetadataManager extensionMetadataManager) {
    this.extensionMetadataManager = extensionMetadataManager;
  }

  @Override
  protected ExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresCluster cluster) {
    return Optional.of(cluster)
        .map(StackGresCluster::getStatus)
        .map(StackGresClusterStatus::getExtensions);
  }

  @Override
  protected StackGresCluster getCluster(StackGresCluster inputContext) {
    return inputContext;
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(
      StackGresCluster inputContext, String version, String buildVersion) {
    return Optional.of(inputContext)
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getExtensions)
        .orElse(List.of());
  }

  @Override
  protected List<ExtensionTuple> getDefaultExtensions(
      StackGresCluster inputContext, String version, String buildVersion) {
    return StackGresUtil.getDefaultClusterExtensions(inputContext);
  }

  @Override
  protected void setToInstallExtensions(StackGresCluster resource,
      List<StackGresClusterInstalledExtension> toInstallExtensions) {
    if (resource.getStatus() == null) {
      resource.setStatus(new StackGresClusterStatus());
    }
    resource.getStatus().setExtensions(toInstallExtensions);
  }

}
