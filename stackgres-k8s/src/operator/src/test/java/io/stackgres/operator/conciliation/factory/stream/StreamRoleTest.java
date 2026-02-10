/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForStream;
import io.stackgres.common.labels.StreamLabelFactory;
import io.stackgres.common.labels.StreamLabelMapper;
import io.stackgres.operator.conciliation.stream.StackGresStreamContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreamRoleTest {

  private final LabelFactoryForStream labelFactory =
      new StreamLabelFactory(new StreamLabelMapper());

  @Mock
  private StackGresStreamContext context;

  private StreamRole streamRole;

  private StackGresStream stream;

  @BeforeEach
  void setUp() {
    streamRole = new StreamRole();
    streamRole.setLabelFactory(labelFactory);
    stream = Fixtures.stream().loadSgClusterToCloudEvent().get();
    when(context.getSource()).thenReturn(stream);
  }

  @Test
  void generateResource_shouldContainServiceAccountRoleAndRoleBinding() {
    List<HasMetadata> resources = streamRole.generateResource(context).toList();

    assertEquals(3, resources.size());
    assertTrue(resources.stream().anyMatch(ServiceAccount.class::isInstance));
    assertTrue(resources.stream().anyMatch(Role.class::isInstance));
    assertTrue(resources.stream().anyMatch(RoleBinding.class::isInstance));
  }

  @Test
  void generateResource_roleHasExpectedNumberOfRules() {
    final List<HasMetadata> resources = streamRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);
    List<PolicyRule> rules = role.getRules();
    assertEquals(5, rules.size());
  }

  @Test
  void generateResource_roleHasRuleForPods() {
    final List<HasMetadata> resources = streamRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains("")
            && rule.getResources().contains("pods")
            && rule.getVerbs().containsAll(List.of("get", "list", "watch"))));
  }

  @Test
  void generateResource_roleHasRuleForStreamCrd() {
    final List<HasMetadata> resources = streamRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresStream.class))
            && rule.getVerbs().containsAll(List.of("get", "list", "watch", "patch", "update"))));
  }

  @Test
  void generateResource_roleHasRuleForClusterCrd() {
    final List<HasMetadata> resources = streamRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresCluster.class))
            && rule.getVerbs().contains("get")
            && rule.getVerbs().size() == 1));
  }

  @Test
  void generateResource_allResourcesNamedCorrectly() {
    final List<HasMetadata> resources = streamRole.generateResource(context).toList();
    String expectedName = StreamRole.roleName(stream);

    assertEquals(expectedName, findResource(resources, Role.class).getMetadata().getName());
    assertEquals(expectedName,
        findResource(resources, ServiceAccount.class).getMetadata().getName());
    assertEquals(expectedName,
        findResource(resources, RoleBinding.class).getMetadata().getName());
  }

  @Test
  void generateResource_roleBindingReferencesCorrectRoleAndSA() {
    final List<HasMetadata> resources = streamRole.generateResource(context).toList();
    final RoleBinding roleBinding = findResource(resources, RoleBinding.class);

    final String expectedName = StreamRole.roleName(stream);
    final String expectedNamespace = stream.getMetadata().getNamespace();

    assertEquals("Role", roleBinding.getRoleRef().getKind());
    assertEquals(expectedName, roleBinding.getRoleRef().getName());
    assertEquals("rbac.authorization.k8s.io", roleBinding.getRoleRef().getApiGroup());

    assertEquals(1, roleBinding.getSubjects().size());
    assertEquals("ServiceAccount", roleBinding.getSubjects().get(0).getKind());
    assertEquals(expectedName, roleBinding.getSubjects().get(0).getName());
    assertEquals(expectedNamespace, roleBinding.getSubjects().get(0).getNamespace());
  }

  @Test
  void generateResource_withNullMetadata_shouldNotThrowNpe() {
    stream.getSpec().setMetadata(null);

    assertDoesNotThrow(() -> streamRole.generateResource(context).toList());
  }

  @SuppressWarnings("unchecked")
  private <T extends HasMetadata> T findResource(List<HasMetadata> resources,
      Class<T> resourceClass) {
    return (T) resources.stream()
        .filter(resourceClass::isInstance)
        .findFirst()
        .orElseThrow(
            () -> new AssertionError("No resource of type " + resourceClass.getSimpleName()));
  }
}
