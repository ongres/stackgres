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
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
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
class WebConsoleServiceRoleTest {

  private final LabelFactoryForConfig labelFactory =
      new ConfigLabelFactory(new ConfigLabelMapper());

  @Mock
  private StackGresConfigContext context;

  private WebConsoleServiceRole webConsoleServiceRole;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    webConsoleServiceRole = new WebConsoleServiceRole(labelFactory);
    config = Fixtures.config().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(config);
    lenient().when(context.getWebConsoleServiceAccount()).thenReturn(Optional.empty());
  }

  @Test
  void generateResource_whenRestapiDeployed_shouldGenerateRoleAndRoleBinding() {
    List<HasMetadata> resources = webConsoleServiceRole.generateResource(context).toList();

    assertEquals(2, resources.size());
    assertInstanceOf(Role.class, resources.get(0));
    assertInstanceOf(RoleBinding.class, resources.get(1));
  }

  @Test
  void generateResource_whenDeployRestapiFalse_shouldReturnEmpty() {
    config.getSpec().getDeploy().setRestapi(false);

    List<HasMetadata> resources = webConsoleServiceRole.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldHaveCorrectRoleName() {
    List<HasMetadata> resources = webConsoleServiceRole.generateResource(context).toList();

    Role role = (Role) resources.get(0);
    assertEquals("stackgres-restapi", role.getMetadata().getName());
    assertEquals("stackgres", role.getMetadata().getNamespace());
  }

  @Test
  void generateResource_shouldHaveCorrectRoleBindingName() {
    List<HasMetadata> resources = webConsoleServiceRole.generateResource(context).toList();

    RoleBinding roleBinding = (RoleBinding) resources.get(1);
    assertEquals("stackgres-restapi", roleBinding.getMetadata().getName());
    assertEquals("stackgres", roleBinding.getMetadata().getNamespace());
  }

}
