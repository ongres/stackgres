/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.app;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
public class MockWatcherHandler implements OperatorWatcherHandler {

  @Override
  public void startWatchers() {
    //For integration testing purposes we don't need watchers

  }

  @Override
  public void stopWatchers() {
    //Do nothing

  }
}
