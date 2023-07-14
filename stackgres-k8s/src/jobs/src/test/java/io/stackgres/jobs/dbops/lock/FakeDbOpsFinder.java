/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.lock;

import java.util.Optional;

import io.quarkus.test.Mock;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@Mock
public class FakeDbOpsFinder implements CustomResourceFinder<StackGresDbOps> {

  private final MockKubeDb kubeDb;

  @Inject
  public FakeDbOpsFinder(MockKubeDb kubeDb) {
    this.kubeDb = kubeDb;
  }

  @Override
  public @NotNull Optional<StackGresDbOps> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(kubeDb.getDbOps(name, namespace));
  }
}
