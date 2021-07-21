package io.stackgres.apiweb.transformer.dbops.converter.fixture;

import java.math.BigDecimal;

import io.stackgres.apiweb.dto.dbops.DbOpsPgbenchStatusMeasure;
import io.stackgres.apiweb.dto.dbops.DbOpsPgbenchStatusTransactionsPerSecond;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusMeasure;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusTransactionsPerSecond;

public class StackGresDbOpsPgbenchStatusTransactionsPerSecondFixture {

  public StackGresDbOpsPgbenchStatusTransactionsPerSecond build() {
    StackGresDbOpsPgbenchStatusMeasure incConnections = new StackGresDbOpsPgbenchStatusMeasure(
        new BigDecimal(1000), "tps");
    StackGresDbOpsPgbenchStatusMeasure excConnections = new StackGresDbOpsPgbenchStatusMeasure(
        new BigDecimal(2000), "tps");
    return new StackGresDbOpsPgbenchStatusTransactionsPerSecond(
        excConnections,incConnections);
  }

}
