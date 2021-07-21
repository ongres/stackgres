/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.dbops.converter;

import static org.junit.Assert.assertNotNull;

import io.stackgres.apiweb.dto.dbops.DbOpsPgbenchStatusLatency;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusLatency;
import org.junit.jupiter.api.Test;

class DbOpsPgbenchStatusLatencyConverterTest {

  @Test
  void shouldConvertFromSGDbOpsPgbenchStatusLatency_ToDbOpsPgbenchStatusLatency() {
    StackGresDbOpsPgbenchStatusLatency sgPgbenchStatusLatency =
        new StackGresDbOpsPgbenchStatusLatency();
    DbOpsPgbenchStatusLatencyConverter converter = new DbOpsPgbenchStatusLatencyConverter();
    DbOpsPgbenchStatusLatency dto = converter.from(sgPgbenchStatusLatency);
    assertNotNull(dto);
  }

}
