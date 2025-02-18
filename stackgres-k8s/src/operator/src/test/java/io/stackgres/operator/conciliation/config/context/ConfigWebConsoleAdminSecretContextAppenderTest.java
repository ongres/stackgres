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
import io.stackgres.operator.conciliation.factory.config.webconsole.WebConsoleAdminSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigWebConsoleAdminSecretContextAppenderTest {

  private ConfigWebConsoleAdminSecretContextAppender contextAppender;

  private StackGresConfig config;

  @Spy
  private StackGresConfigContext.Builder contextBuilder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    contextAppender = new ConfigWebConsoleAdminSecretContextAppender(secretFinder);
  }

  @Test
  void givenConfigWithoutWebConsoleAdminSecret_shouldPass() {
    contextAppender.appendContext(config, contextBuilder);

    verify(secretFinder).findByNameAndNamespace(any(), any());
    verify(contextBuilder).webConsoleAdminSecret(Optional.empty());
  }

  @Test
  void givenConfigWithWebConsoleAdminSecret_shouldRetrieveItAndPass() {
    final Optional<Secret> secret = Optional.of(new SecretBuilder()
        .withData(Map.of())
        .build());
    when(secretFinder.findByNameAndNamespace(
        WebConsoleAdminSecret.sourceName(config),
        config.getMetadata().getNamespace()))
        .thenReturn(secret);
    contextAppender.appendContext(config, contextBuilder);

    verify(contextBuilder).webConsoleAdminSecret(secret);
  }

  @Test
  void givenConfigWithDefaultWebConsoleAdminSecret_shouldRetrieveItAndPass() {
    config.getSpec().getAuthentication().setSecretRef(null);
    final Optional<Secret> secret = Optional.of(new SecretBuilder()
        .withData(Map.of())
        .build());
    when(secretFinder.findByNameAndNamespace(
        WebConsoleAdminSecret.sourceName(config),
        config.getMetadata().getNamespace()))
        .thenReturn(secret);
    contextAppender.appendContext(config, contextBuilder);

    verify(contextBuilder).webConsoleAdminSecret(secret);
  }

}
