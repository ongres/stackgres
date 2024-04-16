/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.lock;

import java.util.Optional;

import javax.annotation.Nonnull;

import io.quarkus.test.Mock;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.inject.Inject;

@Mock
public class FakeDbOpsFinder implements CustomResourceFinder<StackGresDbOps> {

  private final MockKubeDb kubeDb;

  @Inject
  public FakeDbOpsFinder(MockKubeDb kubeDb) {
    this.kubeDb = kubeDb;
  }

  @Override
  public @Nonnull Optional<StackGresDbOps> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(kubeDb.getDbOps(name, namespace));
  }
}
