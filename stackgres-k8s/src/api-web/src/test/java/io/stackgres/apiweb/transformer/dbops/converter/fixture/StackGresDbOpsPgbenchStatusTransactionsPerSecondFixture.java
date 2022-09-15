/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.dbops.converter.fixture;

import java.math.BigDecimal;

import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusMeasure;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusMeasureBuilder;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusTransactionsPerSecond;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusTransactionsPerSecondBuilder;

public class StackGresDbOpsPgbenchStatusTransactionsPerSecondFixture {

  public StackGresDbOpsPgbenchStatusTransactionsPerSecond build() {
    StackGresDbOpsPgbenchStatusMeasure incConnections =
        new StackGresDbOpsPgbenchStatusMeasureBuilder()
        .withValue(new BigDecimal(1000))
        .withUnit("tps")
        .build();
    StackGresDbOpsPgbenchStatusMeasure excConnections =
        new StackGresDbOpsPgbenchStatusMeasureBuilder()
        .withValue(new BigDecimal(2000))
        .withUnit("tps")
        .build();
    return new StackGresDbOpsPgbenchStatusTransactionsPerSecondBuilder()
        .withExcludingConnectionsEstablishing(excConnections)
        .withIncludingConnectionsEstablishing(incConnections)
        .build();
  }

}
