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
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.initialization.DefaultClusterPostgresConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterPostgresConfigContextAppenderTest {

  private ClusterPostgresConfigContextAppender contextAppender;

  private StackGresCluster cluster;

  private DefaultClusterPostgresConfigFactory defaultPostgresConfigFactory = new DefaultClusterPostgresConfigFactory();

  @Spy
  private StackGresClusterContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    contextAppender = new ClusterPostgresConfigContextAppender(
        postgresConfigFinder,
        new DefaultClusterPostgresConfigFactory());
  }

  @Test
  void givenClusterWithPostgresConfig_shouldPass() {
    final var postgresConfig = Optional.of(
        new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresVersion(cluster.getSpec().getPostgres().getVersion().replaceAll("\\..*$", ""))
        .endSpec()
        .build());
    when(postgresConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(postgresConfig);
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).postgresConfig(postgresConfig);
  }

  @Test
  void givenClusterWithoutPostgresConfig_shouldFail() {
    when(postgresConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("SGPostgresConfig postgresconf was not found", ex.getMessage());
  }

  @Test
  void givenClusterWithPostgresConfigWithWrongVersion_shouldFail() {
    when(postgresConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.of(
            new StackGresPostgresConfigBuilder()
            .withNewSpec()
            .withPostgresVersion("10")
            .endSpec()
            .build()));
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(cluster, contextBuilder));
    assertEquals("Invalid postgres version, must be 10 to use SGPostgresConfig postgresconf", ex.getMessage());
  }

  @Test
  void givenClusterWithoutDefaultPostgresConfig_shouldPass() {
    cluster.getSpec().getConfigurations().setSgPostgresConfig(
        defaultPostgresConfigFactory.getDefaultResourceName(cluster));
    when(postgresConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    contextAppender.appendContext(cluster, contextBuilder);
    verify(contextBuilder).postgresConfig(Optional.empty());
  }

}
