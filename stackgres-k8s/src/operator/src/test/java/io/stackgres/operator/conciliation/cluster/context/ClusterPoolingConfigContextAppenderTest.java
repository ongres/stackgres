/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfigBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.initialization.DefaultPoolingConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterPoolingConfigContextAppenderTest {

  private ClusterPoolingConfigContextAppender contextAppender;

  private StackGresCluster cluster;

  private DefaultPoolingConfigFactory defaultPoolingConfigFactory = new DefaultPoolingConfigFactory();

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresPoolingConfig> poolingConfigFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ClusterPoolingConfigContextAppender(
        poolingConfigFinder,
        new DefaultPoolingConfigFactory());
  }

  @Test
  void givenClusterWithPoolingConfig_shouldPass() {
    final Optional<StackGresPoolingConfig> poolingConfig = Optional.of(
        new StackGresPoolingConfigBuilder()
        .build());
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(poolingConfig);
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).poolingConfig(poolingConfig);
  }

  @Test
  void givenClusterWithoutPoolingConfig_shouldFail() {
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("SGPoolingConfig pgbouncerconf was not found", ex.getMessage());
  }

  @Test
  void givenClusterWithoutPoolingConfigAndPoolingDisabled_shouldPass() {
    cluster.getSpec().getPods().setDisableConnectionPooling(true);
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).poolingConfig(Optional.empty());
  }

  @Test
  void givenClusterWithoutDefaultPoolingConfig_shouldPass() {
    cluster.getSpec().getConfigurations().setSgPoolingConfig(
        defaultPoolingConfigFactory.getDefaultResourceName(cluster));
    when(poolingConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).poolingConfig(Optional.empty());
  }

}
