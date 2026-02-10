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
class WebConsoleSecretTest {

  private final LabelFactoryForConfig labelFactory =
      new ConfigLabelFactory(new ConfigLabelMapper());

  @Mock
  private StackGresConfigContext context;

  private WebConsoleSecret webConsoleSecret;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    webConsoleSecret = new WebConsoleSecret(labelFactory);
    config = Fixtures.config().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(config);
    lenient().when(context.getWebConsoleSecret()).thenReturn(Optional.empty());
  }

  @Test
  void generateResource_shouldGenerateOneSecret() {
    when(context.getWebConsoleSecret()).thenReturn(Optional.empty());

    List<HasMetadata> resources = webConsoleSecret.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof Secret);
  }

  @Test
  void generateResource_whenDeployRestapiFalse_shouldReturnEmpty() {
    config.getSpec().getDeploy().setRestapi(false);

    List<HasMetadata> resources = webConsoleSecret.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenCreateForWebApiFalse_shouldReturnEmpty() {
    config.getSpec().getCert().setCreateForWebApi(false);

    List<HasMetadata> resources = webConsoleSecret.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldHaveCorrectType() {
    List<HasMetadata> resources = webConsoleSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.get(0);
    assertEquals("kubernetes.io/tls", secret.getType());
  }

  @Test
  void generateResource_shouldContainTlsAndJwtKeys() {
    List<HasMetadata> resources = webConsoleSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.get(0);
    Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(data.containsKey("tls.crt"));
    assertTrue(data.containsKey("tls.key"));
    assertTrue(data.containsKey("jwt-rsa.crt"));
    assertTrue(data.containsKey("jwt-rsa.key"));
    assertTrue(data.containsKey("jwt-rsa.pub"));
  }

}
