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
class AdminuiNginxConfigMapTest {

  private final LabelFactoryForConfig labelFactory =
      new ConfigLabelFactory(new ConfigLabelMapper());

  @Mock
  private StackGresConfigContext context;

  private AdminuiNginxConfigMap adminuiNginxConfigMap;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    adminuiNginxConfigMap = new AdminuiNginxConfigMap(labelFactory);
    config = Fixtures.config().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(config);
  }

  @Test
  void generateResource_whenRestapiDeployed_shouldGenerateOneConfigMap() {
    List<HasMetadata> resources = adminuiNginxConfigMap.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertInstanceOf(ConfigMap.class, resources.getFirst());
  }

  @Test
  void generateResource_whenDeployRestapiFalse_shouldReturnEmpty() {
    config.getSpec().getDeploy().setRestapi(false);

    List<HasMetadata> resources = adminuiNginxConfigMap.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldHaveCorrectDataKeys() {
    List<HasMetadata> resources = adminuiNginxConfigMap.generateResource(context).toList();

    ConfigMap configMap = (ConfigMap) resources.getFirst();
    assertTrue(configMap.getData().containsKey("start-nginx.sh"));
    assertTrue(configMap.getData().containsKey("nginx.conf"));
    assertTrue(configMap.getData().containsKey("stackgres-restapi.template"));
  }

}
