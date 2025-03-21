/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.distributedlogs.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsDatabaseSecretContextAppenderTest {

  private DistributedLogsDatabaseSecretContextAppender contextAppender;

  private StackGresDistributedLogs distributedLogs;

  @Spy
  private StackGresDistributedLogsContext.Builder contextBuilder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @BeforeEach
  void setUp() {
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    contextAppender = new DistributedLogsDatabaseSecretContextAppender(secretFinder);
  }

  @Test
  void givenDistributedLogsWithoutDatabaseSecret_shouldPass() {
    contextAppender.appendContext(distributedLogs, contextBuilder);

    verify(secretFinder).findByNameAndNamespace(any(), any());
    verify(contextBuilder).databaseSecret(Optional.empty());
  }

  @Test
  void givenDistributedLogsWithDatabaseSecret_shouldRetrieveItAndPass() {
    final Optional<Secret> databaseSecret = Optional.of(new SecretBuilder()
        .withData(Map.of())
        .build());
    when(secretFinder.findByNameAndNamespace(
        distributedLogs.getMetadata().getName(),
        distributedLogs.getMetadata().getNamespace()))
        .thenReturn(databaseSecret);
    contextAppender.appendContext(distributedLogs, contextBuilder);

    verify(contextBuilder).databaseSecret(databaseSecret);
  }

}
