/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.dbops.converter;

import io.stackgres.apiweb.dto.dbops.DbOpsPgbenchStatusLatency;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusLatency;

public class DbOpsPgbenchStatusLatencyConverter {

  public DbOpsPgbenchStatusLatency from(StackGresDbOpsPgbenchStatusLatency source) {
    return new DbOpsPgbenchStatusLatency();
  }

}
