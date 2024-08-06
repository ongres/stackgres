/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.dbops.converter.fixture;

import java.math.BigDecimal;

import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusLatency;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusMeasureBuilder;

public class StackGresDbOpsPgbenchStatusLatencyFixture {

  public StackGresDbOpsPgbenchStatusLatency build() {
    StackGresDbOpsPgbenchStatusLatency latency = new StackGresDbOpsPgbenchStatusLatency();
    latency.setAverage(new StackGresDbOpsPgbenchStatusMeasureBuilder()
        .withValue(new BigDecimal(10.00))
        .withUnit("ms")
        .build());
    latency
        .setStandardDeviation(new StackGresDbOpsPgbenchStatusMeasureBuilder()
            .withValue(new BigDecimal(1.00))
            .withUnit("ms")
            .build());
    return latency;
  }

}
