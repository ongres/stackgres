/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.lock;

import java.util.function.Consumer;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.test.Mock;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceScheduler;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@Mock
public class FakeDbOpsScheduler implements CustomResourceScheduler<StackGresDbOps> {

  private final MockKubeDb kubeDb;

  @Inject
  public FakeDbOpsScheduler(MockKubeDb kubeDb) {
    this.kubeDb = kubeDb;
  }

  @Override
  public StackGresDbOps create(@NotNull StackGresDbOps resource) {
    return kubeDb.addOrReplaceDbOps(resource);
  }

  @Override
  public void delete(@NotNull StackGresDbOps resource) {
    kubeDb.delete(resource);
  }

  @Override
  public StackGresDbOps update(@NotNull StackGresDbOps resource) {
    return kubeDb.addOrReplaceDbOps(resource);
  }

  @Override
  public StackGresDbOps update(@NotNull StackGresDbOps resource,
      @NotNull Consumer<StackGresDbOps> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var dbOps = kubeDb.getDbOps(metadata.getName(), metadata.getNamespace());
    setter.accept(dbOps);
    return kubeDb.addOrReplaceDbOps(dbOps);
  }

  @Override
  public <S> StackGresDbOps updateStatus(@NotNull StackGresDbOps resource,
      @NotNull Consumer<StackGresDbOps> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var dbOps = kubeDb.getDbOps(metadata.getName(), metadata.getNamespace());
    setter.accept(dbOps);
    return kubeDb.addOrReplaceDbOps(dbOps);
  }

}
