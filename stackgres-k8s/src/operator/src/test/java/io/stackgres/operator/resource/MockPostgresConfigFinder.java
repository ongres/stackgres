/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;

//@Mock
public class MockPostgresConfigFinder implements CustomResourceFinder<StackGresPostgresConfig> {

  @Override
  public Optional<StackGresPostgresConfig> findByNameAndNamespace(String name, String namespace) {
    return Optional.of(Fixtures.postgresConfig().loadDefault().get());
  }
}
