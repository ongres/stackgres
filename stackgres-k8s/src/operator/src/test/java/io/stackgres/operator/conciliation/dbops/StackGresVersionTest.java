/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.dbops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StackGresVersionTest {

  private StackGresDbOps dbOps;

  @BeforeEach
  void setUp() {
    dbOps = Fixtures.dbOps().loadRestart().get();
  }

  @Test
  void givenStackGresValidVersion_shouldNotFail() {
    StackGresVersion.getStackGresVersion(dbOps);
  }

  @Test
  void givenAValidVersion_shouldReturnTheCorrectStackGresVersion() {
    setStackGresClusterVersion(StackGresVersion.LATEST.getVersion() + ".0");

    var version = StackGresVersion.getStackGresVersion(dbOps);

    assertEquals(StackGresVersion.LATEST, version);
  }

  @Test
  void givenASnapshotVersion_shouldReturnTheCorrectStackGresVersion() {
    setStackGresClusterVersion(StackGresVersion.LATEST.getVersion() + ".0-SNAPSHOT");

    var version = StackGresVersion.getStackGresVersion(dbOps);

    assertEquals(StackGresVersion.LATEST, version);
  }

  @Test
  void givenAInvalidVersion_shouldThrowAnException() {
    setStackGresClusterVersion("0.1-SNAPSHOT");

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> StackGresVersion.getStackGresVersion(dbOps));

    assertEquals("Invalid version 0.1-SNAPSHOT", ex.getMessage());
  }

  @Test
  void givenACurrentVersion_shouldNotFail() {
    setStackGresClusterVersion(StackGresProperty.OPERATOR_VERSION.getString());

    StackGresVersion.getStackGresVersion(dbOps);
  }

  private void setStackGresClusterVersion(String configVersion) {
    dbOps.getMetadata().getAnnotations().put(StackGresContext.VERSION_KEY, configVersion);
  }
}
