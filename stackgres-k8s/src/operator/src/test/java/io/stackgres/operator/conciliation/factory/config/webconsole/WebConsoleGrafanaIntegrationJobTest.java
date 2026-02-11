/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.KubectlUtil;
import io.stackgres.common.StackGresVersion;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ConfigLabelFactory;
import io.stackgres.common.labels.ConfigLabelMapper;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebConsoleGrafanaIntegrationJobTest {

  private final LabelFactoryForConfig labelFactory =
      new ConfigLabelFactory(new ConfigLabelMapper());

  @Mock
  private StackGresConfigContext context;

  @Mock
  private KubectlUtil kubectl;

  @Mock
  private WebConsolePodSecurityFactory webConsolePodSecurityContext;

  private WebConsoleGrafanaIntegrationJob webConsoleGrafanaIntegrationJob;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    webConsoleGrafanaIntegrationJob =
        new WebConsoleGrafanaIntegrationJob(labelFactory, kubectl, webConsolePodSecurityContext);
    config = Fixtures.config().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(config);
    lenient().when(context.getConfig()).thenReturn(config);
    lenient().when(context.isGrafanaIntegrated()).thenReturn(false);
    lenient().when(context.isGrafanaIntegrationJobFailed()).thenReturn(false);
    lenient().when(context.getGrafanaUser()).thenReturn(Optional.of("admin"));
    lenient().when(context.getGrafanaPassword()).thenReturn(Optional.of("password"));
    lenient().when(webConsolePodSecurityContext
        .createGrafanaIntegrationPodSecurityContext(any())).thenReturn(null);
    lenient().when(kubectl.getImageName(any(StackGresVersion.class)))
        .thenReturn("kubectl:latest");
  }

  @Test
  void generateResource_whenAutoEmbedEnabled_shouldCreateJob() {
    config.getSpec().getGrafana().setAutoEmbed(true);

    List<HasMetadata> resources =
        webConsoleGrafanaIntegrationJob.generateResource(context).toList();

    assertFalse(resources.isEmpty());
    assertTrue(resources.stream().anyMatch(r -> r instanceof Job));
  }

  @Test
  void generateResource_whenAutoEmbedDisabled_shouldReturnEmpty() {
    List<HasMetadata> resources =
        webConsoleGrafanaIntegrationJob.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenDeployRestapiFalse_shouldReturnEmpty() {
    config.getSpec().getDeploy().setRestapi(false);
    config.getSpec().getGrafana().setAutoEmbed(true);

    List<HasMetadata> resources =
        webConsoleGrafanaIntegrationJob.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenAlreadyIntegrated_shouldReturnEmpty() {
    config.getSpec().getGrafana().setAutoEmbed(true);
    lenient().when(context.isGrafanaIntegrated()).thenReturn(true);

    List<HasMetadata> resources =
        webConsoleGrafanaIntegrationJob.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenIntegrationJobFailed_shouldReturnEmpty() {
    config.getSpec().getGrafana().setAutoEmbed(true);
    lenient().when(context.isGrafanaIntegrationJobFailed()).thenReturn(true);

    List<HasMetadata> resources =
        webConsoleGrafanaIntegrationJob.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

}
