/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.List;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
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
class WebConsoleGrafanaIntegrationConfigMapTest {

  private final LabelFactoryForConfig labelFactory =
      new ConfigLabelFactory(new ConfigLabelMapper());

  @Mock
  private StackGresConfigContext context;

  private WebConsoleGrafanaIntegartionConfigMap webConsoleGrafanaIntegrationConfigMap;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    webConsoleGrafanaIntegrationConfigMap =
        new WebConsoleGrafanaIntegartionConfigMap(labelFactory);
    config = Fixtures.config().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(config);
  }

  @Test
  void generateResource_whenRestapiDeployedAndCertCreateForWebApi_shouldGenerateOneConfigMap() {
    List<HasMetadata> resources =
        webConsoleGrafanaIntegrationConfigMap.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertInstanceOf(ConfigMap.class, resources.getFirst());
  }

  @Test
  void generateResource_whenDeployRestapiFalse_shouldReturnEmpty() {
    config.getSpec().getDeploy().setRestapi(false);

    List<HasMetadata> resources =
        webConsoleGrafanaIntegrationConfigMap.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenCreateForWebApiFalse_shouldReturnEmpty() {
    config.getSpec().getCert().setCreateForWebApi(false);

    List<HasMetadata> resources =
        webConsoleGrafanaIntegrationConfigMap.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldContainIntegrateGrafanaScript() {
    List<HasMetadata> resources =
        webConsoleGrafanaIntegrationConfigMap.generateResource(context).toList();

    ConfigMap configMap = (ConfigMap) resources.getFirst();
    assertTrue(configMap.getData().containsKey("integrate-grafana.sh"));
  }

}
