/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.lock;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.test.Mock;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.resource.CustomResourceScheduler;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@Mock
public class FakeStreamScheduler implements CustomResourceScheduler<StackGresStream> {

  private final MockKubeDb kubeDb;

  @Inject
  public FakeStreamScheduler(MockKubeDb kubeDb) {
    this.kubeDb = kubeDb;
  }

  @Override
  public StackGresStream create(@NotNull StackGresStream resource, boolean dryRun) {
    return kubeDb.addOrReplaceStream(resource);
  }

  @Override
  public void delete(@NotNull StackGresStream resource, boolean dryRun) {
    kubeDb.delete(resource);
  }

  @Override
  public StackGresStream update(@NotNull StackGresStream resource, boolean dryRun) {
    return kubeDb.addOrReplaceStream(resource);
  }

  @Override
  public StackGresStream update(@NotNull StackGresStream resource,
      @NotNull Consumer<StackGresStream> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var dbOps = kubeDb.getStream(metadata.getName(), metadata.getNamespace());
    setter.accept(dbOps);
    return kubeDb.addOrReplaceStream(dbOps);
  }

  @Override
  public <S> StackGresStream updateStatus(@NotNull StackGresStream resource,
      @NotNull Consumer<StackGresStream> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var dbOps = kubeDb.getStream(metadata.getName(), metadata.getNamespace());
    setter.accept(dbOps);
    return kubeDb.addOrReplaceStream(dbOps);
  }

}
