/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.stream;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.stackgres.stream.app.StreamProperty;
import io.stackgres.stream.app.StreamReconciliationClock;
import io.stackgres.stream.controller.StreamReconciliationCycle;
import io.stackgres.stream.jobs.StreamLauncher;
import jakarta.inject.Inject;

@QuarkusMain
public class Main implements QuarkusApplication {

  @Inject
  StreamLauncher streamLauncher;

  @Inject
  StreamReconciliationClock streamReconciliationClock;

  @Inject
  StreamReconciliationCycle streamReconciliationCycle;

  @Override
  public int run(String... args) throws Exception {
    streamReconciliationClock.start();
    String streamName = StreamProperty.STREAM_NAME.getString();
    String streamNamespace = StreamProperty.STREAM_NAMESPACE.getString();
    streamLauncher.launchStream(streamName, streamNamespace);
    streamReconciliationClock.stop();
    streamReconciliationCycle.reconcileAll();
    return 0;
  }

}
