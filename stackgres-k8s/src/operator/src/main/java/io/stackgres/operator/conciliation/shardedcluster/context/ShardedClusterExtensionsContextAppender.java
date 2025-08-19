/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardedcluster.context;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.ExtensionTuple;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterExtensionBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterSpec;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedClusterStatus;
import io.stackgres.common.extension.ExtensionMetadataManager;
import io.stackgres.operator.conciliation.AbstractExtensionsContextAppender;
import io.stackgres.operator.conciliation.factory.shardedcluster.StackGresShardedClusterForCitusUtil;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext.Builder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class ShardedClusterExtensionsContextAppender
    extends AbstractExtensionsContextAppender<StackGresShardedCluster, Builder> {

  private final ExtensionMetadataManager extensionMetadataManager;

  @Inject
  public ShardedClusterExtensionsContextAppender(ExtensionMetadataManager extensionMetadataManager) {
    this.extensionMetadataManager = extensionMetadataManager;
  }

  @Override
  protected ExtensionMetadataManager getExtensionMetadataManager() {
    return extensionMetadataManager;
  }

  @Override
  protected StackGresCluster getCluster(StackGresShardedCluster inputContext) {
    return StackGresShardedClusterForCitusUtil
        .getCoordinatorCluster(inputContext);
  }

  @Override
  protected List<StackGresClusterExtension> getExtensions(
      StackGresShardedCluster inputContext, String version, String buildVersion) {
    final List<StackGresClusterExtension> extensions = Optional.of(inputContext)
            .map(StackGresShardedCluster::getSpec)
            .map(StackGresShardedClusterSpec::getPostgres)
            .map(StackGresClusterPostgres::getExtensions)
            .stream()
            .flatMap(List::stream)
            .toList();
    return Seq.seq(extensions)
        .append(
            StackGresUtil.getShardedClusterExtensions(inputContext)
            .stream()
            .filter(extension -> extensions.stream()
                .map(StackGresClusterExtension::getName)
                .noneMatch(extension.extensionName()::equals))
            .map(extension -> new StackGresClusterExtensionBuilder()
                .withName(extension.extensionName())
                .withVersion(extension.extensionVersion().orElse(null))
                .build()))
        .toList();
  }

  @Override
  protected List<ExtensionTuple> getDefaultExtensions(
      StackGresShardedCluster inputContext, String version, String buildVersion) {
    return List.of();
  }

  @Override
  protected void setToInstallExtensions(StackGresShardedCluster resource,
      List<StackGresClusterInstalledExtension> toInstallExtensions) {
    if (resource.getStatus() == null) {
      resource.setStatus(new StackGresShardedClusterStatus());
    }
    resource.getStatus().setExtensions(toInstallExtensions);
  }

}
