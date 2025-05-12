/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.mock;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceScheduler;
import org.jetbrains.annotations.NotNull;

public class MockDbOpsScheduler implements CustomResourceScheduler<StackGresDbOps> {
  final MockKubeDb mockKubeDb;

  public MockDbOpsScheduler(MockKubeDb mockKubeDb) {
    this.mockKubeDb = mockKubeDb;
  }

  @Override
  public StackGresDbOps create(@NotNull StackGresDbOps resource, boolean dryRun) {
    return mockKubeDb.addOrReplaceDbOps(resource);
  }

  @Override
  public void delete(@NotNull StackGresDbOps resource, boolean dryRun) {
    mockKubeDb.delete(resource);
  }

  @Override
  public StackGresDbOps update(@NotNull StackGresDbOps resource, boolean dryRun) {
    return mockKubeDb.addOrReplaceDbOps(resource);
  }

  @Override
  public StackGresDbOps update(
      @NotNull StackGresDbOps resource,
      @NotNull Consumer<StackGresDbOps> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var cluster = mockKubeDb.getDbOps(metadata.getName(), metadata.getNamespace());
    setter.accept(cluster);
    return mockKubeDb.addOrReplaceDbOps(cluster);
  }

  @Override
  public StackGresDbOps updateStatus(
      @NotNull StackGresDbOps resource,
      @NotNull Consumer<StackGresDbOps> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var cluster = mockKubeDb.getDbOps(metadata.getName(), metadata.getNamespace());
    setter.accept(cluster);
    return mockKubeDb.addOrReplaceDbOps(cluster);
  }
}
