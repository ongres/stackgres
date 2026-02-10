/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardeddbops;

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
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.crd.sgshardeddbops.StackGresShardedDbOps;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedDbOps;
import io.stackgres.common.labels.ShardedDbOpsLabelFactory;
import io.stackgres.common.labels.ShardedDbOpsLabelMapper;
import io.stackgres.operator.conciliation.shardeddbops.StackGresShardedDbOpsContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedDbOpsRoleTest {

  private final LabelFactoryForShardedDbOps labelFactory =
      new ShardedDbOpsLabelFactory(new ShardedDbOpsLabelMapper());

  @Mock
  private StackGresShardedDbOpsContext context;

  private ShardedDbOpsRole shardedDbOpsRole;

  private StackGresShardedDbOps dbOps;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    shardedDbOpsRole = new ShardedDbOpsRole();
    shardedDbOpsRole.setLabelFactory(labelFactory);
    dbOps = Fixtures.shardedDbOps().loadRestart().get();
    config = Fixtures.config().loadDefault().get();
    when(context.getSource()).thenReturn(dbOps);
    lenient().when(context.getConfig()).thenReturn(config);
  }

  @Test
  void generateResource_shouldContainServiceAccountRoleAndRoleBinding() {
    List<HasMetadata> resources = shardedDbOpsRole.generateResource(context).toList();

    assertEquals(3, resources.size());
    assertTrue(resources.stream().anyMatch(ServiceAccount.class::isInstance));
    assertTrue(resources.stream().anyMatch(Role.class::isInstance));
    assertTrue(resources.stream().anyMatch(RoleBinding.class::isInstance));
  }

  @Test
  void generateResource_roleHasExpectedNumberOfRules() {
    List<HasMetadata> resources = shardedDbOpsRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertEquals(11, role.getRules().size());
  }

  @Test
  void generateResource_roleHasRuleForPods() {
    List<HasMetadata> resources = shardedDbOpsRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains("")
            && rule.getResources().contains("pods")
            && rule.getVerbs().containsAll(List.of("get", "list", "watch", "delete"))));
  }

  @Test
  void generateResource_roleHasRuleForShardedDbOpsCrd() {
    List<HasMetadata> resources = shardedDbOpsRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresShardedDbOps.class))
            && rule.getVerbs().containsAll(List.of("get", "list", "watch", "patch", "update"))));
  }

  @Test
  void generateResource_roleHasRuleForDbOpsCrd() {
    List<HasMetadata> resources = shardedDbOpsRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresDbOps.class))
            && rule.getVerbs().containsAll(
                List.of("get", "list", "watch", "create", "patch", "update", "delete"))));
  }

  @Test
  void generateResource_roleHasRuleForShardedClusterCrd() {
    List<HasMetadata> resources = shardedDbOpsRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(
                HasMetadata.getPlural(StackGresShardedCluster.class))
            && rule.getVerbs().containsAll(
                List.of("get", "list", "watch", "patch", "update"))));
  }

  @Test
  void generateResource_allResourcesNamedCorrectly() {
    List<HasMetadata> resources = shardedDbOpsRole.generateResource(context).toList();
    String expectedName = ShardedDbOpsRole.roleName(dbOps);

    assertEquals(expectedName, findResource(resources, Role.class).getMetadata().getName());
    assertEquals(expectedName,
        findResource(resources, ServiceAccount.class).getMetadata().getName());
    assertEquals(expectedName,
        findResource(resources, RoleBinding.class).getMetadata().getName());
  }

  @Test
  void generateResource_roleBindingReferencesCorrectRoleAndSA() {
    List<HasMetadata> resources = shardedDbOpsRole.generateResource(context).toList();
    RoleBinding roleBinding = findResource(resources, RoleBinding.class);

    String expectedName = ShardedDbOpsRole.roleName(dbOps);
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
