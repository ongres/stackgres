/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.config.context;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operator.conciliation.factory.config.webconsole.WebConsoleGrafanaIntegrationJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfigGrafanaIntegrationContextAppenderTest {

  private ConfigGrafanaIntegrationContextAppender contextAppender;

  private StackGresConfig config;

  @Spy
  private StackGresConfigContext.Builder contextBuilder;

  @Mock
  private ConfigGrafanaIntegrationChecker grafanaIntegrationChecker;

  @Mock
  private ResourceFinder<Job> jobFinder;

  @BeforeEach
  void setUp() {
    config = Fixtures.config().loadDefault().get();
    contextAppender = new ConfigGrafanaIntegrationContextAppender(
        grafanaIntegrationChecker,
        jobFinder);
  }

  @Test
  void givenConfigWithoutGrafanaWorking_shouldPass() {
    when(jobFinder.findByNameAndNamespace(
        WebConsoleGrafanaIntegrationJob.name(config),
        config.getMetadata().getNamespace()))
        .thenReturn(Optional.empty());

    contextAppender.appendContext(config, contextBuilder);

    verify(grafanaIntegrationChecker).isGrafanaEmbedded(config);
    verify(grafanaIntegrationChecker).isGrafanaIntegrated(config);
    verify(contextBuilder).isGrafanaEmbedded(false);
    verify(contextBuilder).isGrafanaIntegrated(false);
    verify(contextBuilder).isGrafanaIntegrationJobFailed(false);
  }

  @Test
  void givenConfigWithGrafanaEmbedded_shouldPass() {
    Job job = new JobBuilder()
        .build();
    when(grafanaIntegrationChecker.isGrafanaEmbedded(config))
        .thenReturn(true);
    when(jobFinder.findByNameAndNamespace(
        WebConsoleGrafanaIntegrationJob.name(config),
        config.getMetadata().getNamespace()))
        .thenReturn(Optional.of(job));

    contextAppender.appendContext(config, contextBuilder);

    verify(grafanaIntegrationChecker).isGrafanaEmbedded(config);
    verify(grafanaIntegrationChecker).isGrafanaIntegrated(config);
    verify(contextBuilder).isGrafanaEmbedded(true);
    verify(contextBuilder).isGrafanaIntegrated(false);
    verify(contextBuilder).isGrafanaIntegrationJobFailed(false);
  }

  @Test
  void givenConfigWithGrafanaIntegrated_shouldPass() {
    Job job = new JobBuilder()
        .build();
    when(grafanaIntegrationChecker.isGrafanaEmbedded(config))
        .thenReturn(true);
    when(grafanaIntegrationChecker.isGrafanaIntegrated(config))
        .thenReturn(true);
    when(jobFinder.findByNameAndNamespace(
        WebConsoleGrafanaIntegrationJob.name(config),
        config.getMetadata().getNamespace()))
        .thenReturn(Optional.of(job));

    contextAppender.appendContext(config, contextBuilder);

    verify(grafanaIntegrationChecker).isGrafanaEmbedded(config);
    verify(grafanaIntegrationChecker).isGrafanaIntegrated(config);
    verify(contextBuilder).isGrafanaEmbedded(true);
    verify(contextBuilder).isGrafanaIntegrated(true);
    verify(contextBuilder).isGrafanaIntegrationJobFailed(false);
  }

  @Test
  void givenConfigWithGrafanaJobFailed_shouldPass() {
    Job job = new JobBuilder()
        .withNewStatus()
        .withFailed(1)
        .endStatus()
        .build();
    when(grafanaIntegrationChecker.isGrafanaEmbedded(config))
        .thenReturn(true);
    when(grafanaIntegrationChecker.isGrafanaIntegrated(config))
        .thenReturn(true);
    when(jobFinder.findByNameAndNamespace(
        WebConsoleGrafanaIntegrationJob.name(config),
        config.getMetadata().getNamespace()))
        .thenReturn(Optional.of(job));

    contextAppender.appendContext(config, contextBuilder);

    verify(grafanaIntegrationChecker).isGrafanaEmbedded(config);
    verify(grafanaIntegrationChecker).isGrafanaIntegrated(config);
    verify(contextBuilder).isGrafanaEmbedded(true);
    verify(contextBuilder).isGrafanaIntegrated(true);
    verify(contextBuilder).isGrafanaIntegrationJobFailed(true);
  }

}
