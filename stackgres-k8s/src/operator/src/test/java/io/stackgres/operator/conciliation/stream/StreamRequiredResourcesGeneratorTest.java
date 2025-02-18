/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ClusterFinder;
import io.stackgres.common.resource.ConfigScanner;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class StreamRequiredResourcesGeneratorTest {

  @InjectMock
  ConfigScanner configScanner;

  @InjectMock
  ClusterFinder clusterFinder;

  @Inject
  StreamRequiredResourcesGenerator generator;

  private StackGresConfig config;
  private StackGresCluster cluster;
  private StackGresStream stream;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    stream = Fixtures.stream().loadSgClusterToCloudEvent().get();
    cluster = Fixtures.cluster().loadDefault().get();
  }

  @Test
  void givenValidStream_getRequiredResourcesShouldNotFail() {
    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config)));

    when(clusterFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(cluster));

    generator.getRequiredResources(stream);
  }

}
