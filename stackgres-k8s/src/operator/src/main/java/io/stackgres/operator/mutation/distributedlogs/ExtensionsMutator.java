/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.google.common.collect.ImmutableList;
import io.stackgres.common.CdiUtil;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsStatus;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.mutation.AbstractExtensionsMutator;
import io.stackgres.operator.mutation.ClusterExtensionMetadataManager;

@ApplicationScoped
public class ExtensionsMutator
    extends AbstractExtensionsMutator<StackGresDistributedLogs, StackGresDistributedLogsReview>
    implements DistributedLogsMutator {

  @Inject
  public ExtensionsMutator(
      ClusterExtensionMetadataManager extensionMetadataManager) {
    super(extensionMetadataManager);
  }

  public ExtensionsMutator() {
    super(null);
    CdiUtil.checkPublicNoArgsConstructorIsCalledToCreateProxy();
  }

  @Override
  protected List<StackGresClusterInstalledExtension> getToInstallExtensions(
      StackGresDistributedLogs distributedLogs) {
    return Optional.of(distributedLogs)
        .map(StackGresDistributedLogs::getStatus)
        .map(StackGresDistributedLogsStatus::getToInstallPostgresExtensions)
        .orElse(ImmutableList.of());
  }

  @Override
  protected StackGresCluster getCluster(StackGresDistributedLogs distributedLogs) {
    return StackGresDistributedLogsUtil.getStackGresClusterForDistributedLogs(distributedLogs);
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(
      StackGresDistributedLogs distributedLogs) {
    return ImmutableList.of();
  }

  @Override
  protected ImmutableList<StackGresClusterExtension> getDefaultExtensions(
      StackGresCluster cluster) {
    return ImmutableList.of(
        getExtension(cluster, "plpgsql"),
        getExtension(cluster, "pg_stat_statements"),
        getExtension(cluster, "dblink"),
        getExtension(cluster, "plpython3u"),
        getExtension(cluster, "timescaledb", "1.7.4"));
  }

}
