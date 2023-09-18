/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.mutation.AbstractExtensionsMutator;

@ApplicationScoped
public class ExtensionsMutator
    extends AbstractExtensionsMutator<StackGresDistributedLogs, StackGresDistributedLogsReview>
    implements DistributedLogsMutator {

  private final ExtensionMetadataManager extensionMetadataManager;

  @Inject
  public ExtensionsMutator(ExtensionMetadataManager extensionMetadataManager) {
    this.extensionMetadataManager = extensionMetadataManager;
  }

  @Override
  protected ExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected Optional<List<StackGresClusterInstalledExtension>> getToInstallExtensions(
      StackGresDistributedLogs distributedLogs) {
    return Optional.of(distributedLogs)
        .map(StackGresDistributedLogs::getSpec)
        .map(StackGresDistributedLogsSpec::getToInstallPostgresExtensions);
  }

  @Override
  protected StackGresCluster getCluster(StackGresDistributedLogs distributedLogs) {
    return StackGresDistributedLogsUtil.getStackGresClusterForDistributedLogs(distributedLogs);
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(
      StackGresDistributedLogs distributedLogs) {
    return List.of();
  }

  @Override
  protected List<ExtensionTuple> getDefaultExtensions(StackGresDistributedLogs cluster) {
    return StackGresUtil.getDefaultDistributedLogsExtensions(cluster);
  }

  @Override
  protected void setToInstallExtensions(StackGresDistributedLogs resource,
      List<StackGresClusterInstalledExtension> toInstallExtensions) {
    resource.getSpec().setToInstallPostgresExtensions(toInstallExtensions);
  }

}
