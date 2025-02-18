/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operator.conciliation.factory.config.webconsole.WebConsoleDeployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigWebConsoleServiceAccountContextAppenderTest {

  private ConfigWebConsoleServiceAccountContextAppender contextAppender;

  private StackGresConfig config;

  @Spy
  private StackGresConfigContext.Builder contextBuilder;

  @Mock
  private ResourceFinder<ServiceAccount> serviceAccountFinder;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    contextAppender = new ConfigWebConsoleServiceAccountContextAppender(serviceAccountFinder);
  }

  @Test
  void givenConfigWithoutWebConsoleServiceAccount_shouldPass() {
    contextAppender.appendContext(config, contextBuilder);

    verify(serviceAccountFinder).findByNameAndNamespace(any(), any());
    verify(contextBuilder).webConsoleServiceAccount(Optional.empty());
  }

  @Test
  void givenConfigWithWebConsoleServiceAccount_shouldRetrieveItAndPass() {
    final Optional<ServiceAccount> serviceAccount = Optional.of(new ServiceAccountBuilder()
        .build());
    when(serviceAccountFinder.findByNameAndNamespace(
        WebConsoleDeployment.name(config),
        config.getMetadata().getNamespace()))
        .thenReturn(serviceAccount);
    contextAppender.appendContext(config, contextBuilder);

    verify(contextBuilder).webConsoleServiceAccount(serviceAccount);
  }

}
