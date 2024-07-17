/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.fixture.stream;

import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.crd.sgstream.StackGresStreamBuilder;
import io.stackgres.common.fixture.VersionedFixture;

public class StreamFixture extends VersionedFixture<StackGresStream> {

  public StreamFixture loadSgClusterToCloudEvent() {
    fixture = readFromJson(STACKGRES_STREAM_SGCLUSTER_TO_CLOUDEVENT_JSON);
    return this;
  }

  public StackGresStreamBuilder getBuilder() {
    return new StackGresStreamBuilder(fixture);
  }

  public StreamSchedulingFixture scheduling() {
    return new StreamSchedulingFixture();
  }

}
