/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.lock;

import javax.inject.Inject;

import io.quarkus.test.Mock;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceScheduler;
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
  public StackGresDbOps update(@NotNull StackGresDbOps resource) {
    return kubeDb.addOrReplaceDbOps(resource);
  }

  @Override
  public void delete(@NotNull StackGresDbOps resource) {
    kubeDb.delete(resource);
  }

  @Override
  public StackGresDbOps updateStatus(StackGresDbOps resource) {
    String name = resource.getMetadata().getName();
    String namespace = resource.getMetadata().getNamespace();

    StackGresDbOps savedOp = kubeDb.getDbOps(name, namespace);
    savedOp.setStatus(resource.getStatus());
    return kubeDb.addOrReplaceDbOps(savedOp);
  }
}
