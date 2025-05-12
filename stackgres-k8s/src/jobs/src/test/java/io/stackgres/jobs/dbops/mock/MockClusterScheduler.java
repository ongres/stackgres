/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.mock;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceScheduler;
import org.jetbrains.annotations.NotNull;

public class MockClusterScheduler implements CustomResourceScheduler<StackGresCluster> {
  final MockKubeDb mockKubeDb;

  public MockClusterScheduler(MockKubeDb mockKubeDb) {
    this.mockKubeDb = mockKubeDb;
  }

  @Override
  public StackGresCluster create(@NotNull StackGresCluster resource, boolean dryRun) {
    return mockKubeDb.addOrReplaceCluster(resource);
  }

  @Override
  public void delete(@NotNull StackGresCluster resource, boolean dryRun) {
    mockKubeDb.delete(resource);
  }

  @Override
  public StackGresCluster update(@NotNull StackGresCluster resource, boolean dryRun) {
    return mockKubeDb.addOrReplaceCluster(resource);
  }

  @Override
  public StackGresCluster update(
      @NotNull StackGresCluster resource,
      @NotNull Consumer<StackGresCluster> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var cluster = mockKubeDb.getCluster(metadata.getName(), metadata.getNamespace());
    setter.accept(cluster);
    return mockKubeDb.addOrReplaceCluster(cluster);
  }

  @Override
  public StackGresCluster updateStatus(
      @NotNull StackGresCluster resource,
      @NotNull Consumer<StackGresCluster> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var cluster = mockKubeDb.getCluster(metadata.getName(), metadata.getNamespace());
    setter.accept(cluster);
    return mockKubeDb.addOrReplaceCluster(cluster);
  }
}
