/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.Condition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdbops.StackGresDbOpsStatus;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.DbOpsLabelFactory;
import io.stackgres.common.labels.DbOpsLabelMapper;
import io.stackgres.common.labels.LabelFactoryForDbOps;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DbOpsRoleTest {

  private final LabelFactoryForDbOps labelFactory =
      new DbOpsLabelFactory(new DbOpsLabelMapper());

  @Mock
  private StackGresDbOpsContext context;

  private DbOpsRole dbOpsRole;

  private StackGresDbOps dbOps;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    dbOpsRole = new DbOpsRole();
    dbOpsRole.setLabelFactory(labelFactory);
    dbOps = Fixtures.dbOps().loadPgbench().get();
    config = Fixtures.config().loadDefault().get();
    when(context.getSource()).thenReturn(dbOps);
    lenient().when(context.getConfig()).thenReturn(config);
  }

  @Test
  void generateResource_shouldContainServiceAccountRoleAndRoleBinding() {
    List<HasMetadata> resources = dbOpsRole.generateResource(context).toList();

    assertEquals(3, resources.size());
    assertTrue(resources.stream().anyMatch(ServiceAccount.class::isInstance));
    assertTrue(resources.stream().anyMatch(Role.class::isInstance));
    assertTrue(resources.stream().anyMatch(RoleBinding.class::isInstance));
  }

  @Test
  void generateResource_whenRolloutOp_shouldReturnEmpty() {
    dbOps.getSpec().setOp("restart");

    List<HasMetadata> resources = dbOpsRole.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenAlreadyCompleted_shouldReturnEmpty() {
    Condition condition = new Condition();
    condition.setType("Completed");
    condition.setStatus("True");
    StackGresDbOpsStatus status = new StackGresDbOpsStatus();
    status.setConditions(List.of(condition));
    dbOps.setStatus(status);

    List<HasMetadata> resources = dbOpsRole.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_roleHasExpectedNumberOfRules() {
    List<HasMetadata> resources = dbOpsRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertEquals(9, role.getRules().size());
  }

  @Test
  void generateResource_roleHasRuleForPods() {
    List<HasMetadata> resources = dbOpsRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains("")
            && rule.getResources().contains("pods")
            && rule.getVerbs().containsAll(List.of("get", "list", "watch", "delete"))));
  }

  @Test
  void generateResource_roleHasRuleForDbOpsCrd() {
    List<HasMetadata> resources = dbOpsRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresDbOps.class))
            && rule.getVerbs().containsAll(List.of("get", "list", "watch", "patch", "update"))));
  }

  @Test
  void generateResource_roleHasRuleForClusterCrd() {
    List<HasMetadata> resources = dbOpsRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresCluster.class))
            && rule.getVerbs().containsAll(List.of("get", "list", "watch", "patch", "update"))));
  }

  @Test
  void generateResource_allResourcesNamedCorrectly() {
    List<HasMetadata> resources = dbOpsRole.generateResource(context).toList();
    String expectedName = DbOpsRole.roleName(dbOps);

    assertEquals(expectedName, findResource(resources, Role.class).getMetadata().getName());
    assertEquals(expectedName,
        findResource(resources, ServiceAccount.class).getMetadata().getName());
    assertEquals(expectedName,
        findResource(resources, RoleBinding.class).getMetadata().getName());
  }

  @Test
  void generateResource_roleBindingReferencesCorrectRoleAndSA() {
    List<HasMetadata> resources = dbOpsRole.generateResource(context).toList();
    RoleBinding roleBinding = findResource(resources, RoleBinding.class);

    String expectedName = DbOpsRole.roleName(dbOps);
    assertEquals("Role", roleBinding.getRoleRef().getKind());
    assertEquals(expectedName, roleBinding.getRoleRef().getName());
    assertEquals("rbac.authorization.k8s.io", roleBinding.getRoleRef().getApiGroup());

    assertEquals(1, roleBinding.getSubjects().size());
    assertEquals("ServiceAccount", roleBinding.getSubjects().get(0).getKind());
    assertEquals(expectedName, roleBinding.getSubjects().get(0).getName());
    assertEquals(dbOps.getMetadata().getNamespace(),
        roleBinding.getSubjects().get(0).getNamespace());
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
