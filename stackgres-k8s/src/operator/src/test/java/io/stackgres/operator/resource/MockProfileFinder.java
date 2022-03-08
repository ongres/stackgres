/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;

//@Mock
public class MockProfileFinder implements CustomResourceFinder<StackGresProfile> {

  @Override
  public Optional<StackGresProfile> findByNameAndNamespace(String name, String namespace) {
    return Optional.of(Fixtures.instanceProfile().loadSizeXs().get());
  }
}
