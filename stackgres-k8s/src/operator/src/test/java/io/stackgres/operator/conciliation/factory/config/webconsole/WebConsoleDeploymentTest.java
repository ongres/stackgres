/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ConfigLabelFactory;
import io.stackgres.common.labels.ConfigLabelMapper;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebConsoleDeploymentTest {

  private WebConsoleDeployment webConsoleDeployment;

  @Mock
  private StackGresConfigContext context;

  @Mock
  private WebConsolePodSecurityFactory webConsolePodSecurityContext;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    webConsoleDeployment = new WebConsoleDeployment(
        new ConfigLabelFactory(new ConfigLabelMapper()),
        webConsolePodSecurityContext);
    config = Fixtures.config().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(config);
    lenient().when(context.getConfig()).thenReturn(config);
    lenient().when(context.isGrafanaEmbedded()).thenReturn(false);
    lenient().when(webConsolePodSecurityContext.createRestApiPodSecurityContext(any()))
        .thenReturn(null);
    lenient().when(webConsolePodSecurityContext.createRestapiSecurityContext(any()))
        .thenReturn(null);
    lenient().when(webConsolePodSecurityContext.createAdminuiSecurityContext(any()))
        .thenReturn(null);
  }

  @Test
  void generateResource_shouldGenerateOneDeployment() {
    List<HasMetadata> resources = webConsoleDeployment.generateResource(context).toList();

    Assertions.assertEquals(1, resources.size());
    Assertions.assertInstanceOf(Deployment.class, resources.getFirst());
  }

  @Test
  void generateResource_whenDeployRestapiFalse_shouldReturnEmpty() {
    config.getSpec().getDeploy().setRestapi(false);

    List<HasMetadata> resources = webConsoleDeployment.generateResource(context).toList();

    Assertions.assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldHaveTwoContainers() {
    List<HasMetadata> resources = webConsoleDeployment.generateResource(context).toList();

    Deployment deployment = (Deployment) resources.getFirst();
    Assertions.assertEquals(2, deployment.getSpec().getTemplate().getSpec()
        .getContainers().size());
    Assertions.assertEquals("stackgres-restapi",
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getName());
    Assertions.assertEquals("stackgres-adminui",
        deployment.getSpec().getTemplate().getSpec().getContainers().get(1).getName());
  }

  @Test
  void generateResource_shouldHaveCorrectRestapiPorts() {
    List<HasMetadata> resources = webConsoleDeployment.generateResource(context).toList();

    Deployment deployment = (Deployment) resources.getFirst();
    var restapiContainer = deployment.getSpec().getTemplate().getSpec()
        .getContainers().get(0);
    Assertions.assertEquals(2, restapiContainer.getPorts().size());
    Assertions.assertEquals(8080, restapiContainer.getPorts().get(0).getContainerPort());
    Assertions.assertEquals(8443, restapiContainer.getPorts().get(1).getContainerPort());
  }

  @Test
  void generateResource_shouldHaveCorrectAdminuiPorts() {
    List<HasMetadata> resources = webConsoleDeployment.generateResource(context).toList();

    Deployment deployment = (Deployment) resources.getFirst();
    var adminuiContainer = deployment.getSpec().getTemplate().getSpec()
        .getContainers().get(1);
    Assertions.assertEquals(2, adminuiContainer.getPorts().size());
    Assertions.assertEquals(9080, adminuiContainer.getPorts().get(0).getContainerPort());
    Assertions.assertEquals(9443, adminuiContainer.getPorts().get(1).getContainerPort());
  }

  @Test
  void generateResource_shouldHaveReplicasSetToOne() {
    List<HasMetadata> resources = webConsoleDeployment.generateResource(context).toList();

    Deployment deployment = (Deployment) resources.getFirst();
    Assertions.assertEquals(1, deployment.getSpec().getReplicas());
  }

  @Test
  void generateResource_whenGrafanaEmbedded_shouldIncludeGrafanaContainer() {
    lenient().when(context.isGrafanaEmbedded()).thenReturn(true);

    List<HasMetadata> resources = webConsoleDeployment.generateResource(context).toList();

    Deployment deployment = (Deployment) resources.getFirst();
    var restapiContainer = deployment.getSpec().getTemplate().getSpec()
        .getContainers().get(0);
    var grafanaEmbeddedEnv = restapiContainer.getEnv().stream()
        .filter(e -> "GRAFANA_EMBEDDED".equals(e.getName()))
        .findFirst();
    Assertions.assertTrue(grafanaEmbeddedEnv.isPresent(),
        "Expected GRAFANA_EMBEDDED env var in restapi container");
    Assertions.assertEquals("true", grafanaEmbeddedEnv.get().getValue());

    var adminuiContainer = deployment.getSpec().getTemplate().getSpec()
        .getContainers().get(1);
    var adminuiGrafanaEnv = adminuiContainer.getEnv().stream()
        .filter(e -> "GRAFANA_EMBEDDED".equals(e.getName()))
        .findFirst();
    Assertions.assertTrue(adminuiGrafanaEnv.isPresent(),
        "Expected GRAFANA_EMBEDDED env var in adminui container");
    Assertions.assertEquals("true", adminuiGrafanaEnv.get().getValue());
  }

  @Test
  void generateResource_whenAdminuiDisabled_shouldNotIncludeAdminui() {
    config.getSpec().getDeploy().setRestapi(false);

    List<HasMetadata> resources = webConsoleDeployment.generateResource(context).toList();

    Assertions.assertTrue(resources.isEmpty(),
        "Expected no deployment when restapi deploy is disabled");
  }

}
