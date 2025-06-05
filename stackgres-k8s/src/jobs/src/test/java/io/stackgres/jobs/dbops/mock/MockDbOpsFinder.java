/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.dbops.mock;

import java.util.Optional;

import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.resource.CustomResourceFinder;
import org.jetbrains.annotations.NotNull;

public class MockDbOpsFinder implements CustomResourceFinder<StackGresDbOps> {
  final MockKubeDb mockKubeDb;

  public MockDbOpsFinder(MockKubeDb mockKubeDb) {
    this.mockKubeDb = mockKubeDb;
  }

  @Override
  public @NotNull Optional<StackGresDbOps> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(mockKubeDb.getDbOps(name, namespace));
  }
}
