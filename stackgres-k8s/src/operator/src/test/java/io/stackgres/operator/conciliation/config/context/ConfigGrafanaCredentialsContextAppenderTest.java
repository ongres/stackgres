/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigGrafanaBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigGrafanaCredentialsContextAppenderTest {

  private ConfigGrafanaCredentialsContextAppender contextAppender;

  private StackGresConfig config;

  @Spy
  private StackGresConfigContext.Builder contextBuilder;

  @Mock
  private ResourceFinder<Secret> secretFinder;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    contextAppender = new ConfigGrafanaCredentialsContextAppender(secretFinder);
  }

  @Test
  void givenConfigWithoutGrafanaCredentials_shouldPass() {
    config.getSpec().setGrafana(null);

    contextAppender.appendContext(config, contextBuilder);

    verify(secretFinder, Mockito.never()).findByNameAndNamespace(any(), any());
    verify(contextBuilder).grafanaUser(Optional.empty());
    verify(contextBuilder).grafanaPassword(Optional.empty());
  }

  @Test
  void givenConfigWithGrafanaCredentials_shouldRetrieveItAndPass() {
    config.getSpec().setGrafana(
        new StackGresConfigGrafanaBuilder()
        .withUser("test")
        .withPassword("1234")
        .build());

    contextAppender.appendContext(config, contextBuilder);

    verify(secretFinder, Mockito.never()).findByNameAndNamespace(any(), any());
    verify(contextBuilder).grafanaUser(Optional.of("test"));
    verify(contextBuilder).grafanaPassword(Optional.of("1234"));
  }

  @Test
  void givenConfigWithValidGrafanaCredentialsSecret_shouldRetrieveItAndPass() {
    config.getSpec().setGrafana(
        new StackGresConfigGrafanaBuilder()
        .withSecretName("grafana")
        .withSecretNamespace("grafana")
        .withSecretUserKey("user")
        .withSecretPasswordKey("password")
        .build());
    final Optional<Secret> secret = Optional.of(new SecretBuilder()
        .withData(ResourceUtil.encodeSecret(Map.of(
            config.getSpec().getGrafana().getSecretUserKey(), "test",
            config.getSpec().getGrafana().getSecretPasswordKey(), "1234")))
        .build());
    when(secretFinder.findByNameAndNamespace(
        config.getSpec().getGrafana().getSecretName(),
        config.getSpec().getGrafana().getSecretNamespace()))
        .thenReturn(secret);

    contextAppender.appendContext(config, contextBuilder);

    verify(contextBuilder).grafanaUser(Optional.of("test"));
    verify(contextBuilder).grafanaPassword(Optional.of("1234"));
  }

  @Test
  void givenConfigWithMissingGrafanaCredentialsSecret_shouldFail() {
    config.getSpec().setGrafana(
        new StackGresConfigGrafanaBuilder()
        .withSecretName("grafana")
        .withSecretNamespace("grafana")
        .withSecretUserKey("user")
        .withSecretPasswordKey("password")
        .build());
    when(secretFinder.findByNameAndNamespace(
        config.getSpec().getGrafana().getSecretName(),
        config.getSpec().getGrafana().getSecretNamespace()))
        .thenReturn(Optional.empty());

    var ex =
        assertThrows(IllegalArgumentException.class, () -> contextAppender.appendContext(config, contextBuilder));
    assertEquals("Can not find secret grafana.grafana for grafana credentials", ex.getMessage());
  }

}
