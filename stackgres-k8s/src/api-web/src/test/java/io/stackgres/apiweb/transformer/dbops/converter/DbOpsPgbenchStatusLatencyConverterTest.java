/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.transformer.dbops.converter;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.stackgres.apiweb.dto.dbops.DbOpsPgbenchStatusLatency;
import io.stackgres.apiweb.transformer.dbops.converter.fixture.StackGresDbOpsPgbenchStatusLatencyFixture;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsPgbenchStatusLatency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DbOpsPgbenchStatusLatencyConverterTest {

  private DbOpsPgbenchStatusLatencyConverter converter;
  private StackGresDbOpsPgbenchStatusLatency sgDbOpsPgbenchStatus;

  @BeforeEach
  public void setup() {
    this.converter = new DbOpsPgbenchStatusLatencyConverter();
    this.sgDbOpsPgbenchStatus = new StackGresDbOpsPgbenchStatusLatencyFixture().build();
  }

  @Test
  void shouldConvertFromSgDbOpsPgbenchStatusLatency_ToDbOpsPgbenchStatusLatency() {
    DbOpsPgbenchStatusLatency dto = converter.from(sgDbOpsPgbenchStatus);

    assertEquals(dto.getAverage().getValue(),
        sgDbOpsPgbenchStatus.getAverage().getValue());
    assertEquals(dto.getAverage().getUnit(),
        sgDbOpsPgbenchStatus.getAverage().getUnit());
    assertEquals(dto.getStandardDeviation().getValue(),
        sgDbOpsPgbenchStatus.getStandardDeviation().getValue());
    assertEquals(dto.getStandardDeviation().getUnit(),
        sgDbOpsPgbenchStatus.getStandardDeviation().getUnit());
  }

  @Test
  void shouldReturnNull_whenSgDbOpsPgbenchStatusLatencyIsNull() {
    assertNull(converter.from(null));
  }

}
