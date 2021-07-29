/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.dbops.converter.fixture;

import java.math.BigDecimal;

import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusMeasure;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusTransactionsPerSecond;

public class StackGresDbOpsPgbenchStatusTransactionsPerSecondFixture {

  public StackGresDbOpsPgbenchStatusTransactionsPerSecond build() {
    StackGresDbOpsPgbenchStatusMeasure incConnections = new StackGresDbOpsPgbenchStatusMeasure(
        new BigDecimal(1000), "tps");
    StackGresDbOpsPgbenchStatusMeasure excConnections = new StackGresDbOpsPgbenchStatusMeasure(
        new BigDecimal(2000), "tps");
    return new StackGresDbOpsPgbenchStatusTransactionsPerSecond(
        excConnections, incConnections);
  }

}
