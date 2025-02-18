/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfigBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceFinder;
import io.stackgres.operator.common.StackGresDistributedLogsUtil;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.initialization.DefaultDistributedLogsPostgresConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsPostgresConfigContextAppenderTest {

  private DistributedLogsPostgresConfigContextAppender contextAppender;

  private StackGresDistributedLogs distributedLogs;

  private DefaultDistributedLogsPostgresConfigFactory defaultPostgresConfigFactory =
      new DefaultDistributedLogsPostgresConfigFactory();

  @Spy
  private StackGresDistributedLogsContext.Builder contextBuilder;

  @Mock
  private CustomResourceFinder<StackGresPostgresConfig> postgresConfigFinder;

  @BeforeEach
  void setUp() {
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    contextAppender = new DistributedLogsPostgresConfigContextAppender(
        postgresConfigFinder,
        new DefaultDistributedLogsPostgresConfigFactory());
  }

  @Test
  void givenDistributedLogsWithPostgresConfig_shouldPass() {
    final var postgresConfig = Optional.of(
        new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresVersion(StackGresDistributedLogsUtil.getPostgresVersion(distributedLogs).replaceAll("\\..*$", ""))
        .endSpec()
        .build());
    when(postgresConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(postgresConfig);
    contextAppender.appendContext(distributedLogs, contextBuilder);
    verify(contextBuilder).postgresConfig(postgresConfig);
  }

  @Test
  void givenDistributedLogsWithoutPostgresConfig_shouldFail() {
    when(postgresConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    var ex = assertThrows(
        IllegalArgumentException.class,
        () -> contextAppender.appendContext(distributedLogs, contextBuilder));
    assertEquals("SGPostgresConfig postgresconf was not found", ex.getMessage());
  }

  @Test
  void givenDistributedLogsWithPostgresConfigWithWrongVersion_shouldPass() {
    final Optional<StackGresPostgresConfig> postgresConfig = Optional.of(
        new StackGresPostgresConfigBuilder()
        .withNewSpec()
        .withPostgresVersion("10")
        .endSpec()
        .build());
    when(postgresConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(postgresConfig);
    contextAppender.appendContext(distributedLogs, contextBuilder);
    verify(contextBuilder).postgresConfig(postgresConfig);
  }

  @Test
  void givenDistributedLogsWithoutDefaultPostgresConfig_shouldPass() {
    distributedLogs.getSpec().getConfigurations().setSgPostgresConfig(
        defaultPostgresConfigFactory.getDefaultResourceName(distributedLogs));
    when(postgresConfigFinder.findByNameAndNamespace(any(), any()))
        .thenReturn(Optional.empty());
    contextAppender.appendContext(distributedLogs, contextBuilder);
    verify(contextBuilder).postgresConfig(Optional.empty());
  }

}
