/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ConfigScanner;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class StreamRequiredResourcesGeneratorTest {

  @InjectMock
  ConfigScanner configScanner;

  @Inject
  StreamRequiredResourcesGenerator generator;

  private StackGresConfig config;
  private StackGresStream stream;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    stream = Fixtures.stream().loadSgClusterToCloudEvent().get();
    config = Fixtures.config().loadDefault().get();
  }

  @Test
  void givenValidCluster_getRequiredResourcesShouldNotFail() {
    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config)));

    generator.getRequiredResources(stream);
  }

  @SuppressWarnings("unused")
  private void assertException(String message) {
    var ex =
        assertThrows(IllegalArgumentException.class, () -> generator.getRequiredResources(stream));
    assertEquals(message, ex.getMessage());
  }

}
