/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.webconsole;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ConfigLabelFactory;
import io.stackgres.common.labels.ConfigLabelMapper;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebConsoleAdminSecretTest {

  private final LabelFactoryForConfig labelFactory =
      new ConfigLabelFactory(new ConfigLabelMapper());

  @Mock
  private StackGresConfigContext context;

  private WebConsoleAdminSecret webConsoleAdminSecret;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    webConsoleAdminSecret = new WebConsoleAdminSecret(labelFactory);
    config = Fixtures.config().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(config);
    lenient().when(context.getWebConsoleAdminSecret()).thenReturn(Optional.empty());
  }

  @Test
  void generateResource_shouldGenerateOneSecret() {
    when(context.getWebConsoleAdminSecret()).thenReturn(Optional.empty());

    List<HasMetadata> resources = webConsoleAdminSecret.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof Secret);
  }

  @Test
  void generateResource_whenDeployRestapiFalse_shouldReturnEmpty() {
    config.getSpec().getDeploy().setRestapi(false);

    List<HasMetadata> resources = webConsoleAdminSecret.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenCreateAdminSecretFalse_shouldReturnEmpty() {
    config.getSpec().getAuthentication().setCreateAdminSecret(false);

    List<HasMetadata> resources = webConsoleAdminSecret.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldHaveCorrectName() {
    List<HasMetadata> resources = webConsoleAdminSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.get(0);
    assertEquals("stackgres-restapi-admin", secret.getMetadata().getName());
  }

  @Test
  void generateResource_shouldContainUsernameAndPassword() {
    List<HasMetadata> resources = webConsoleAdminSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.get(0);
    Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(data.containsKey("k8sUsername"));
    assertTrue(data.containsKey("password"));
  }

  @Test
  void generateResource_shouldHaveAuthLabel() {
    List<HasMetadata> resources = webConsoleAdminSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.get(0);
    Map<String, String> labels = secret.getMetadata().getLabels();
    assertTrue(labels.containsKey(StackGresContext.AUTH_KEY));
    assertEquals(StackGresContext.AUTH_USER_VALUE, labels.get(StackGresContext.AUTH_KEY));
  }

}
