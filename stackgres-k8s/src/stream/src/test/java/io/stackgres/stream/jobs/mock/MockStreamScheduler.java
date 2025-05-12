/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.mock;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.resource.CustomResourceScheduler;
import org.jetbrains.annotations.NotNull;

public class MockStreamScheduler implements CustomResourceScheduler<StackGresStream> {
  final MockKubeDb mockKubeDb;

  public MockStreamScheduler(MockKubeDb mockKubeDb) {
    this.mockKubeDb = mockKubeDb;
  }

  @Override
  public StackGresStream create(@NotNull StackGresStream resource, boolean dryRun) {
    return mockKubeDb.addOrReplaceStream(resource);
  }

  @Override
  public void delete(@NotNull StackGresStream resource, boolean dryRun) {
    mockKubeDb.delete(resource);
  }

  @Override
  public StackGresStream update(@NotNull StackGresStream resource, boolean dryRun) {
    return mockKubeDb.addOrReplaceStream(resource);
  }

  @Override
  public StackGresStream update(
      @NotNull StackGresStream resource,
      @NotNull Consumer<StackGresStream> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var cluster = mockKubeDb.getStream(metadata.getName(), metadata.getNamespace());
    setter.accept(cluster);
    return mockKubeDb.addOrReplaceStream(cluster);
  }

  @Override
  public StackGresStream updateStatus(
      @NotNull StackGresStream resource,
      @NotNull Consumer<StackGresStream> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var cluster = mockKubeDb.getStream(metadata.getName(), metadata.getNamespace());
    setter.accept(cluster);
    return mockKubeDb.addOrReplaceStream(cluster);
  }
}
