/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.stream;

import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.fixture.Fixtures;

@QuarkusTest
class StreamSgClusterToCloudEventJobTest extends StreamJobTestCase {

  @Override
  StackGresStream getStream() {
    return Fixtures.stream().loadSgClusterToCloudEvent().get();
  }

}
