/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.shardeddbops.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.CustomResourceScanner;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedDbOpsConfigContextAppenderTest {

  private ShardedDbOpsConfigContextAppender contextAppender;

  private StackGresShardedDbOps dbOps;

  private StackGresConfig config;

  @Spy
  private StackGresShardedDbOpsContext.Builder contextBuilder;

  @Mock
  private CustomResourceScanner<StackGresConfig> configScanner;

  @BeforeEach
  void setUp() {
    dbOps = Fixtures.shardedDbOps().loadRestart().get();
    config = Fixtures.config().loadDefault().get();
    contextAppender = new ShardedDbOpsConfigContextAppender(
        configScanner);
  }

  @Test
  void givenDbOpsWithConfig_shouldPass() {
    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config)));
    contextAppender.appendContext(dbOps, contextBuilder);
    verify(contextBuilder).config(config);
  }

  @Test
  void givenDbOpsWithoutConfig_shouldFail() {
    when(configScanner.findResources())
        .thenReturn(Optional.empty());
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(dbOps, contextBuilder));
    assertEquals("SGConfig not found or more than one exists. Aborting reoconciliation!", ex.getMessage());
  }

  @Test
  void givenDbOpsWithManyConfig_shouldFail() {
    when(configScanner.findResources())
        .thenReturn(Optional.of(List.of(config, config)));
    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(dbOps, contextBuilder));
    assertEquals("SGConfig not found or more than one exists. Aborting reoconciliation!", ex.getMessage());
  }

}
