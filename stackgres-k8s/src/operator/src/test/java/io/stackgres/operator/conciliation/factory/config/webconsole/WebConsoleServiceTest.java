/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
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
class WebConsoleServiceTest {

  private final LabelFactoryForConfig labelFactory =
      new ConfigLabelFactory(new ConfigLabelMapper());

  @Mock
  private StackGresConfigContext context;

  private WebConsoleService webConsoleService;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    webConsoleService = new WebConsoleService(labelFactory);
    config = Fixtures.config().loadDefault().get();
    when(context.getSource()).thenReturn(config);
  }

  @Test
  void generateResource_shouldGenerateOneService() {
    List<HasMetadata> resources = webConsoleService.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof Service);
  }

  @Test
  void generateResource_whenDeployRestapiFalse_shouldReturnEmpty() {
    config.getSpec().getDeploy().setRestapi(false);

    List<HasMetadata> resources = webConsoleService.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldHaveCorrectNameAndNamespace() {
    List<HasMetadata> resources = webConsoleService.generateResource(context).toList();

    Service service = (Service) resources.get(0);
    assertEquals("stackgres-restapi", service.getMetadata().getName());
    assertEquals("stackgres", service.getMetadata().getNamespace());
  }

  @Test
  void generateResource_shouldHaveHttpsPort() {
    List<HasMetadata> resources = webConsoleService.generateResource(context).toList();

    Service service = (Service) resources.get(0);
    List<ServicePort> ports = service.getSpec().getPorts();

    assertTrue(ports.stream().anyMatch(port ->
        port.getPort().equals(443)
            && "https".equals(port.getTargetPort().getStrVal())
            && "https".equals(port.getName())));
  }

  @Test
  void generateResource_whenExposeHttpTrue_shouldHaveBothPorts() {
    config.getSpec().getAdminui().getService().setExposeHttp(true);

    List<HasMetadata> resources = webConsoleService.generateResource(context).toList();

    Service service = (Service) resources.get(0);
    List<ServicePort> ports = service.getSpec().getPorts();

    assertEquals(2, ports.size());
    assertTrue(ports.stream().anyMatch(port ->
        port.getPort().equals(443) && "https".equals(port.getName())));
    assertTrue(ports.stream().anyMatch(port ->
        port.getPort().equals(80) && "http".equals(port.getName())));
  }

}
