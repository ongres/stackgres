/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.lock;

import java.util.function.Consumer;

import javax.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.test.Mock;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceScheduler;
import jakarta.inject.Inject;

@Mock
public class FakeDbOpsScheduler implements CustomResourceScheduler<StackGresDbOps> {

  private final MockKubeDb kubeDb;

  @Inject
  public FakeDbOpsScheduler(MockKubeDb kubeDb) {
    this.kubeDb = kubeDb;
  }

  @Override
  public StackGresDbOps create(@Nonnull StackGresDbOps resource, boolean dryRun) {
    return kubeDb.addOrReplaceDbOps(resource);
  }

  @Override
  public void delete(@Nonnull StackGresDbOps resource, boolean dryRun) {
    kubeDb.delete(resource);
  }

  @Override
  public StackGresDbOps update(@Nonnull StackGresDbOps resource, boolean dryRun) {
    return kubeDb.addOrReplaceDbOps(resource);
  }

  @Override
  public StackGresDbOps update(@Nonnull StackGresDbOps resource,
      @Nonnull Consumer<StackGresDbOps> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var dbOps = kubeDb.getDbOps(metadata.getName(), metadata.getNamespace());
    setter.accept(dbOps);
    return kubeDb.addOrReplaceDbOps(dbOps);
  }

  @Override
  public <S> StackGresDbOps updateStatus(@Nonnull StackGresDbOps resource,
      @Nonnull Consumer<StackGresDbOps> setter) {
    final ObjectMeta metadata = resource.getMetadata();
    var dbOps = kubeDb.getDbOps(metadata.getName(), metadata.getNamespace());
    setter.accept(dbOps);
    return kubeDb.addOrReplaceDbOps(dbOps);
  }

}
