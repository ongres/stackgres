/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.component;

import java.util.List;

import io.stackgres.common.StackGresComponent;
import io.stackgres.common.component.Component.ComposedComponentVersion;
import io.stackgres.common.docir.StackGresContextMock;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DocirVersionReaderTest {

  DocirVersionReader docirVersionReader = new DocirVersionReader(null, null);

  @Test
  void getAllPatroniComposedVersions_shouldNotFail() {
    final List<ComposedComponentVersion> versions = docirVersionReader.getComposedVersions(
        StackGresContextMock.CONTEXT, StackGresComponent.PATRONI.get(Fixtures.registryCluster()));
    Assertions.assertEquals(9, versions.size());
    Assertions.assertEquals(9, versions.stream()
        .filter(version -> version.getVersion().getVersion().equals("3.3.3")).count());
    Assertions.assertTrue(versions.stream()
        .allMatch(version -> version.getSubComponentVersions().size() == 3));
    Assertions.assertEquals(9, versions.stream()
        .filter(version -> version.getSubComponentVersions().get(0).getVersion().equals("3.0.3")).count());
    Assertions.assertEquals(9, versions.stream()
        .filter(version -> version.getSubComponentVersions().get(1).getVersion().equals("0.10.7")).count());
    Assertions.assertTrue(versions.stream()
        .anyMatch(version -> version.getSubComponentVersions().get(2).getVersion().equals("17.6")));
    Assertions.assertTrue(versions.stream()
        .anyMatch(version -> version.getSubComponentVersions().get(2).getVersion().equals("17.0")));
    Assertions.assertTrue(versions.stream()
        .anyMatch(version -> version.getSubComponentVersions().get(2).getVersion().equals("16.4")));
    Assertions.assertTrue(versions.stream()
        .anyMatch(version -> version.getSubComponentVersions().get(2).getVersion().equals("15.8")));
    Assertions.assertTrue(versions.stream()
        .anyMatch(version -> version.getSubComponentVersions().get(2).getVersion().equals("14.13")));
    Assertions.assertTrue(versions.stream()
        .anyMatch(version -> version.getSubComponentVersions().get(2).getVersion().equals("13.16")));
    Assertions.assertTrue(versions.stream()
        .anyMatch(version -> version.getSubComponentVersions().get(2).getVersion().equals("13.15")));
    Assertions.assertTrue(versions.stream()
        .anyMatch(version -> version.getSubComponentVersions().get(2).getVersion().equals("12.20")));
  }

  @Test
  void getAllPostgresComposedVersions_shouldNotFail() {
    final List<ComposedComponentVersion> versions = docirVersionReader.getComposedVersions(
        StackGresContextMock.CONTEXT, StackGresComponent.POSTGRESQL.get(Fixtures.registryCluster()));
    Assertions.assertEquals(8, versions.size());
  }

}
