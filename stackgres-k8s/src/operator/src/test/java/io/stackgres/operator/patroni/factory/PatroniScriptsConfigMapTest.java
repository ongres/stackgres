/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PatroniScriptsConfigMapTest {

  @Test
  void encodeDatabaseTest() {
    assertEquals("\\\\\\h\\\\.\\h\\h\\\\\\\\\\h\\h.\\h", PatroniScriptsConfigMap.encodeDatabase("\\/\\.//\\\\//./"));
  }

}