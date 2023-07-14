/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
public class MockOperatorBootstrap implements OperatorBootstrap {
  @Override
  public void bootstrap() {
    //For integration testing purposes we don't need bootstrap
  }

}
