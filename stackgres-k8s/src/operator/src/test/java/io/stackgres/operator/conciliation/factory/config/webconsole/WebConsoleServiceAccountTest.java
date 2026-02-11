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
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
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
class WebConsoleServiceAccountTest {

  private final LabelFactoryForConfig labelFactory =
      new ConfigLabelFactory(new ConfigLabelMapper());

  @Mock
  private StackGresConfigContext context;

  private WebConsoleServiceAccount webConsoleServiceAccount;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    webConsoleServiceAccount = new WebConsoleServiceAccount(labelFactory);
    config = Fixtures.config().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(config);
    lenient().when(context.getWebConsoleServiceAccount()).thenReturn(Optional.empty());
  }

  @Test
  void generateResource_whenRestapiDeployed_shouldGenerateOneServiceAccount() {
    List<HasMetadata> resources = webConsoleServiceAccount.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertInstanceOf(ServiceAccount.class, resources.getFirst());
  }

  @Test
  void generateResource_whenDeployRestapiFalse_shouldReturnEmpty() {
    config.getSpec().getDeploy().setRestapi(false);

    List<HasMetadata> resources = webConsoleServiceAccount.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldHaveCorrectName() {
    List<HasMetadata> resources = webConsoleServiceAccount.generateResource(context).toList();

    ServiceAccount serviceAccount = (ServiceAccount) resources.getFirst();
    assertEquals("stackgres-restapi", serviceAccount.getMetadata().getName());
    assertEquals("stackgres", serviceAccount.getMetadata().getNamespace());
  }

}
