/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.mutation.distributedlogs;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.stackgres.common.StackGresDistributedLogsUtil;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogsSpec;
import io.stackgres.operator.common.OperatorExtensionMetadataManager;
import io.stackgres.operator.common.StackGresDistributedLogsReview;
import io.stackgres.operator.mutation.AbstractExtensionsMutator;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ExtensionsMutator
    extends AbstractExtensionsMutator<StackGresDistributedLogs, StackGresDistributedLogsReview>
    implements DistributedLogsMutator {

  private final OperatorExtensionMetadataManager extensionMetadataManager;
  private final ObjectMapper objectMapper;

  @Inject
  public ExtensionsMutator(OperatorExtensionMetadataManager extensionMetadataManager,
      ObjectMapper objectMapper) {
    super();
    this.extensionMetadataManager = extensionMetadataManager;
    this.objectMapper = objectMapper;
  }

  @Override
  protected OperatorExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected ObjectMapper getObjectMapper() {
    return objectMapper;
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
    return ImmutableList.of();
  }

  @Override
  protected ImmutableList<StackGresClusterInstalledExtension> getDefaultExtensions(
      StackGresCluster cluster) {
    return Seq.seq(StackGresUtil.getDefaultDistributedLogsExtensions(cluster))
        .map(t -> t.v2
        .map(version -> getExtension(cluster, t.v1, version))
            .orElseGet(() -> getExtension(cluster, t.v1)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(ImmutableList.toImmutableList());
  }

}
