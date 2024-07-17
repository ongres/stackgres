/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream.jobs.lock;

import java.util.Optional;

import io.quarkus.test.Mock;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.resource.CustomResourceFinder;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

@Mock
public class FakeStreamFinder implements CustomResourceFinder<StackGresStream> {

  private final MockKubeDb kubeDb;

  @Inject
  public FakeStreamFinder(MockKubeDb kubeDb) {
    this.kubeDb = kubeDb;
  }

  @Override
  public @NotNull Optional<StackGresStream> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(kubeDb.getStream(name, namespace));
  }
}
