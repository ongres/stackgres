/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.testutil.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StackGresVersionTest {

  private StackGresScript script;

  @BeforeEach
  void setUp() {
    script = JsonUtil
        .readFromJson("stackgres_script/default.json", StackGresScript.class);
  }

  @Test
  void givenStackGresValidVersion_shouldNotFail() {

    StackGresVersion.getStackGresVersion(script);
  }

  @Test
  void givenAValidVersion_shouldReturnTheCorrectStackGresVersion() {
    setStackGresScriptVersion("1.0.0");

    var version = StackGresVersion.getStackGresVersion(script);

    assertEquals(StackGresVersion.V_1_0, version);
  }

  @Test
  void givenASnapshotVersion_shouldReturnTheCorrectStackGresVersion() {
    setStackGresScriptVersion("1.0.0-SNAPSHOT");

    var version = StackGresVersion.getStackGresVersion(script);

    assertEquals(StackGresVersion.V_1_0, version);
  }

  @Test
  void givenAInvalidVersion_shouldThrowAnException() {
    setStackGresScriptVersion("0.1-SNAPSHOT");

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> StackGresVersion.getStackGresVersion(script));

    assertEquals("Invalid version 0.1-SNAPSHOT", ex.getMessage());
  }

  @Test
  void givenACurrentVersion_shouldNotFail() {
    setStackGresScriptVersion(StackGresProperty.OPERATOR_VERSION.getString());

    StackGresVersion.getStackGresVersion(script);
  }

  private void setStackGresScriptVersion(String configVersion) {
    script.getMetadata().getAnnotations().put(StackGresContext.VERSION_KEY, configVersion);
  }
}
