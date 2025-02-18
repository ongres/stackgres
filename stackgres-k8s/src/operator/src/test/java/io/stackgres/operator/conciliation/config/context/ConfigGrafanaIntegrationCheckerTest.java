/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgconfig.StackGresConfigStatusBuilder;
import io.stackgres.common.fixture.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigGrafanaIntegrationCheckerTest {

  private StackGresConfig config;

  @Spy
  private ConfigGrafanaIntegrationChecker grafanaIntegrationChecker;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
  }

  @Test
  void givenConfigWithValidGrafanaEmbedded_shouldReturnTrue() {
    config.getSpec().getGrafana().setAutoEmbed(true);
    config.setStatus(
        new StackGresConfigStatusBuilder()
        .withNewGrafana()
        .withConfigHash(
            String.valueOf(config.getSpec().getGrafana().hashCode()))
        .withUrls(List.of("https://url"))
        .withToken("test")
        .endGrafana()
        .build());
    assertTrue(grafanaIntegrationChecker.isGrafanaEmbedded(config));
  }

  @Test
  void givenConfigWithoutGrafanaAutoEmbedded_shouldReturnFalse() {
    config.getSpec().getGrafana().setAutoEmbed(false);
    config.setStatus(
        new StackGresConfigStatusBuilder()
        .withNewGrafana()
        .withConfigHash(
            String.valueOf(config.getSpec().getGrafana().hashCode()))
        .withUrls(List.of("https://url"))
        .withToken("test")
        .endGrafana()
        .build());
    assertFalse(grafanaIntegrationChecker.isGrafanaEmbedded(config));
  }

  @Test
  void givenConfigWithoutGrafanaStatus_shouldReturnFalse() {
    config.getSpec().getGrafana().setAutoEmbed(true);
    config.setStatus(null);
    assertFalse(grafanaIntegrationChecker.isGrafanaEmbedded(config));
  }

  @Test
  void givenConfigWithoutGrafanaConfigHash_shouldReturnFalse() {
    config.getSpec().getGrafana().setAutoEmbed(true);
    config.setStatus(
        new StackGresConfigStatusBuilder()
        .withNewGrafana()
        .withUrls(List.of("https://url"))
        .withToken("test")
        .endGrafana()
        .build());
    assertFalse(grafanaIntegrationChecker.isGrafanaEmbedded(config));
  }

  @Test
  void givenConfigWithInvalidGrafanaConfigHash_shouldReturnFalse() {
    config.getSpec().getGrafana().setAutoEmbed(true);
    config.setStatus(
        new StackGresConfigStatusBuilder()
        .withNewGrafana()
        .withConfigHash("test")
        .withUrls(List.of("https://url"))
        .withToken("test")
        .endGrafana()
        .build());
    assertFalse(grafanaIntegrationChecker.isGrafanaEmbedded(config));
  }

  @Test
  void givenConfigWithoutGrafanaUrls_shouldReturnFalse() {
    config.getSpec().getGrafana().setAutoEmbed(true);
    config.setStatus(
        new StackGresConfigStatusBuilder()
        .withNewGrafana()
        .withConfigHash(
            String.valueOf(config.getSpec().getGrafana().hashCode()))
        .withToken("test")
        .endGrafana()
        .build());
    assertFalse(grafanaIntegrationChecker.isGrafanaEmbedded(config));
  }

  @Test
  void givenConfigWithoutGrafanaToken_shouldReturnFalse() {
    config.getSpec().getGrafana().setAutoEmbed(true);
    config.setStatus(
        new StackGresConfigStatusBuilder()
        .withNewGrafana()
        .withConfigHash(
            String.valueOf(config.getSpec().getGrafana().hashCode()))
        .withUrls(List.of("https://url"))
        .endGrafana()
        .build());
    assertFalse(grafanaIntegrationChecker.isGrafanaEmbedded(config));
  }

  @Test
  void givenConfigWithValidGrafanaIntegration_shouldReturnTrue() {
    config.getSpec().getGrafana().setAutoEmbed(true);
    config.setStatus(
        new StackGresConfigStatusBuilder()
        .withNewGrafana()
        .withConfigHash(
            String.valueOf(config.getSpec().getGrafana().hashCode()))
        .withUrls(List.of("https://url"))
        .withToken("test")
        .endGrafana()
        .build());
    when(grafanaIntegrationChecker.checkUnsecureUri(config.getStatus().getGrafana(), "https://url"))
        .thenReturn(Optional.empty());

    assertTrue(grafanaIntegrationChecker.isGrafanaIntegrated(config));
  }

  @Test
  void givenConfigWithInvalidGrafanaUrl_shouldReturnFalse() {
    config.getSpec().getGrafana().setAutoEmbed(true);
    config.setStatus(
        new StackGresConfigStatusBuilder()
        .withNewGrafana()
        .withConfigHash(
            String.valueOf(config.getSpec().getGrafana().hashCode()))
        .withUrls(List.of("not-an-url"))
        .withToken("test")
        .endGrafana()
        .build());

    assertFalse(grafanaIntegrationChecker.isGrafanaIntegrated(config));
    verify(grafanaIntegrationChecker, Mockito.never()).checkUnsecureUri(Mockito.any(), Mockito.any());
  }

  @Test
  void givenConfigWithInvalidGrafanaUrlResponse_shouldReturnFalse() {
    config.getSpec().getGrafana().setAutoEmbed(true);
    config.setStatus(
        new StackGresConfigStatusBuilder()
        .withNewGrafana()
        .withConfigHash(
            String.valueOf(config.getSpec().getGrafana().hashCode()))
        .withUrls(List.of("https://url"))
        .withToken("test")
        .endGrafana()
        .build());
    when(grafanaIntegrationChecker.checkUnsecureUri(config.getStatus().getGrafana(), "https://url"))
        .thenReturn(Optional.of(new Exception()));

    assertFalse(grafanaIntegrationChecker.isGrafanaIntegrated(config));
  }

}
