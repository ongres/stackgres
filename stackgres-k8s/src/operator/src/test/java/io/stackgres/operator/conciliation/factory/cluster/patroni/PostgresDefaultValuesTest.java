/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import io.stackgres.operator.conciliation.factory.cluster.postgres.PostgresDefaultValues;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PostgresDefaultValuesTest {

  @ParameterizedTest
  @ValueSource(strings = {"11", "12", "12.5", "12.99", "12.99999999", "9.5.21"})
  void walKeepSegmentsVersionPg12(String version) {
    Map<String, String> defaultValues = PostgresDefaultValues.getDefaultValues(version);

    assertEquals("96", defaultValues.get("wal_keep_segments"));
    assertEquals("logical", defaultValues.get("wal_level"));
    assertNull(defaultValues.get("wal_keep_size"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"13", "14", "13.5", "14.99", "13.99999999", "15.5.9.7"})
  void walKeepSizeVersionPg13(String version) {
    Map<String, String> defaultValues = PostgresDefaultValues.getDefaultValues(version);

    assertEquals("1536MB", defaultValues.get("wal_keep_size"));
    assertEquals("logical", defaultValues.get("wal_level"));
    assertNull(defaultValues.get("wal_keep_segments"));
  }

  @Test
  void throwNpeOnNull() {
    assertThrows(NullPointerException.class, () -> PostgresDefaultValues.getDefaultValues(null));
  }

}
