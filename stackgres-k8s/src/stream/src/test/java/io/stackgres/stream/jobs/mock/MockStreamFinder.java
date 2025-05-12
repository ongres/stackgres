/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.mock;

import java.util.Optional;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.resource.CustomResourceFinder;
import org.jetbrains.annotations.NotNull;

public class MockStreamFinder implements CustomResourceFinder<StackGresStream> {
  final MockKubeDb mockKubeDb;

  public MockStreamFinder(MockKubeDb mockKubeDb) {
    this.mockKubeDb = mockKubeDb;
  }

  @Override
  public @NotNull Optional<StackGresStream> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(mockKubeDb.getStream(name, namespace));
  }
}
