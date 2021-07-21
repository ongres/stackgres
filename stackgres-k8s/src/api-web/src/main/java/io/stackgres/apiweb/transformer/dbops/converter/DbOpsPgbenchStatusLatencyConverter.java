/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.dbops.converter;

import io.stackgres.apiweb.dto.dbops.DbOpsPgbenchStatusLatency;
import io.stackgres.apiweb.dto.dbops.DbOpsPgbenchStatusMeasure;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusLatency;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusMeasure;

public class DbOpsPgbenchStatusLatencyConverter {

  public DbOpsPgbenchStatusLatency from(StackGresDbOpsPgbenchStatusLatency source) {
    if (source == null) {
      return null;
    }

    StackGresDbOpsPgbenchStatusMeasure sgAverage = source.getAverage();
    StackGresDbOpsPgbenchStatusMeasure sgDeviation = source.getStandardDeviation();

    DbOpsPgbenchStatusMeasure average = new DbOpsPgbenchStatusMeasure(
        sgAverage.getValue(), sgAverage.getUnit());
    DbOpsPgbenchStatusMeasure standardDeviation = new DbOpsPgbenchStatusMeasure(
        sgDeviation.getValue(), sgDeviation.getUnit());
    return new DbOpsPgbenchStatusLatency(average, standardDeviation);
  }

}
