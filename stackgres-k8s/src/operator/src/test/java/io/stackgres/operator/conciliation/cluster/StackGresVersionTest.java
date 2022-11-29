/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StackGresVersionTest {

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
  }

  @Test
  void givenStackGresValidVersion_shouldNotFail() {
    setStackGresVersion(StackGresVersion.LATEST.getVersion());

    StackGresVersion.getStackGresVersion(cluster);
  }

  @Test
  void givenAValidVersion_shouldReturnTheCorrectStackGresVersion() {
    setStackGresVersion(StackGresVersion.LATEST.getVersion() + ".0");

    var version = StackGresVersion.getStackGresVersion(cluster);

    assertEquals(StackGresVersion.LATEST, version);
  }

  @Test
  void givenASnapshotVersion_shouldReturnTheCorrectStackGresVersion() {
    setStackGresVersion(StackGresVersion.LATEST.getVersion() + ".0-SNAPSHOT");

    var version = StackGresVersion.getStackGresVersion(cluster);

    assertEquals(StackGresVersion.LATEST, version);
  }

  @Test
  void givenAInvalidVersion_shouldThrowAnException() {
    setStackGresVersion("0.1-SNAPSHOT");

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> StackGresVersion.getStackGresVersion(cluster));

    assertEquals("Invalid version 0.1-SNAPSHOT", ex.getMessage());
  }

  @Test
  void givenACurrentVersion_shouldNotFail() {
    setStackGresVersion(StackGresProperty.OPERATOR_VERSION.getString());

    StackGresVersion.getStackGresVersion(cluster);
  }

  private void setStackGresVersion(String configVersion) {
    cluster.getMetadata().getAnnotations().put(StackGresContext.VERSION_KEY, configVersion);
  }
}
