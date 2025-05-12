/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.mock;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.resource.CustomResourceFinder;
import org.jetbrains.annotations.NotNull;

public class MockClusterFinder implements CustomResourceFinder<StackGresCluster> {
  final MockKubeDb mockKubeDb;

  public MockClusterFinder(MockKubeDb mockKubeDb) {
    this.mockKubeDb = mockKubeDb;
  }

  @Override
  public @NotNull Optional<StackGresCluster> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(mockKubeDb.getCluster(name, namespace));
  }

}
