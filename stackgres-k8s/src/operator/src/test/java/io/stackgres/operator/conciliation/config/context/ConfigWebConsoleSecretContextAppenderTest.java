/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operator.conciliation.factory.config.webconsole.WebConsoleSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigWebConsoleSecretContextAppenderTest {

  private ConfigWebConsoleSecretContextAppender contextAppender;

  private StackGresConfig config;

  @Spy
  private StackGresConfigContext.Builder contextBuilder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    contextAppender = new ConfigWebConsoleSecretContextAppender(secretFinder);
  }

  @Test
  void givenConfigWithoutWebConsoleSecret_shouldPass() {
    contextAppender.appendContext(config, contextBuilder);

    verify(secretFinder).findByNameAndNamespace(any(), any());
    verify(contextBuilder).webConsoleSecret(Optional.empty());
  }

  @Test
  void givenConfigWithWebConsoleSecret_shouldRetrieveItAndPass() {
    final Optional<Secret> secret = Optional.of(new SecretBuilder()
        .withData(Map.of())
        .build());
    when(secretFinder.findByNameAndNamespace(
        WebConsoleSecret.name(config),
        config.getMetadata().getNamespace()))
        .thenReturn(secret);
    contextAppender.appendContext(config, contextBuilder);

    verify(contextBuilder).webConsoleSecret(secret);
  }

}
