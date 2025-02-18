/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterConfigContextAppenderTest {

  private ClusterConfigContextAppender contextAppender;

  private StackGresCluster cluster;

  private StackGresConfig config;

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private CustomResourceScanner<StackGresConfig> configScanner;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    config = Fixtures.config().loadDefault().get();
    contextAppender = new ClusterConfigContextAppender(
        configScanner);
  }

  @Test
  void givenClusterWithConfig_shouldPass() {
    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config)));
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).config(config);
  }

  @Test
  void givenClusterWithoutConfig_shouldFail() {
    when(configScanner.findResources())
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("SGConfig not found or more than one exists. Aborting reoconciliation!", ex.getMessage());
  }

  @Test
  void givenClusterWithManyConfig_shouldFail() {
    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config, config)));
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("SGConfig not found or more than one exists. Aborting reoconciliation!", ex.getMessage());
  }

}
