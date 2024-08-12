/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.dbops.converter;

import io.stackgres.apiweb.dto.dbops.DbOpsPgbenchStatusMeasure;
import io.stackgres.apiweb.dto.dbops.DbOpsPgbenchStatusTransactionsPerSecond;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusTransactionsPerSecond;

public class DbOpsPgbenchStatusTransactionsPerSecondConverter {

  public DbOpsPgbenchStatusTransactionsPerSecond from(
      StackGresDbOpsPgbenchStatusTransactionsPerSecond source) {
    if (source == null) {
      return null;
    }
    DbOpsPgbenchStatusMeasure excludingConnections = new DbOpsPgbenchStatusMeasure(
        source.getExcludingConnectionsEstablishing().getValue(),
        source.getExcludingConnectionsEstablishing().getUnit());
    DbOpsPgbenchStatusMeasure includingConnections = new DbOpsPgbenchStatusMeasure(
        source.getIncludingConnectionsEstablishing().getValue(),
        source.getIncludingConnectionsEstablishing().getUnit());
    return new DbOpsPgbenchStatusTransactionsPerSecond(
        excludingConnections, includingConnections);
  }

}
