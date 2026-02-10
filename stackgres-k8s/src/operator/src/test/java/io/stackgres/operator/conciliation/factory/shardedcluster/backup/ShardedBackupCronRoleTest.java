/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster.backup;

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
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedBackupCronRoleTest {

  private final LabelFactoryForShardedCluster labelFactory =
      new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());

  @Mock
  private StackGresShardedClusterContext context;

  private ShardedBackupCronRole shardedBackupCronRole;

  private StackGresShardedCluster cluster;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    shardedBackupCronRole = new ShardedBackupCronRole();
    shardedBackupCronRole.setLabelFactory(labelFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
    config = Fixtures.config().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getConfig()).thenReturn(config);
  }

  @Test
  void generateResource_shouldContainServiceAccountRoleAndRoleBinding() {
    List<HasMetadata> resources = shardedBackupCronRole.generateResource(context).toList();

    assertEquals(3, resources.size());
    assertTrue(resources.stream().anyMatch(ServiceAccount.class::isInstance));
    assertTrue(resources.stream().anyMatch(Role.class::isInstance));
    assertTrue(resources.stream().anyMatch(RoleBinding.class::isInstance));
  }

  @Test
  void generateResource_roleHasExpectedNumberOfRules() {
    List<HasMetadata> resources = shardedBackupCronRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertEquals(6, role.getRules().size());
  }

  @Test
  void generateResource_roleHasRuleForPods() {
    List<HasMetadata> resources = shardedBackupCronRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains("")
            && rule.getResources().contains("pods")
            && rule.getVerbs().containsAll(List.of("get", "list"))));
  }

  @Test
  void generateResource_roleHasRuleForShardedClusterCrd() {
    List<HasMetadata> resources = shardedBackupCronRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(
                HasMetadata.getPlural(StackGresShardedCluster.class))
            && rule.getVerbs().containsAll(List.of("get", "patch"))));
  }

  @Test
  void generateResource_roleHasRuleForShardedBackupCrd() {
    List<HasMetadata> resources = shardedBackupCronRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(
                HasMetadata.getPlural(StackGresShardedBackup.class))
            && rule.getVerbs().containsAll(
                List.of("list", "get", "create", "patch", "update", "delete"))));
  }

  @Test
  void generateResource_roleHasRuleForBackupCrd() {
    List<HasMetadata> resources = shardedBackupCronRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresBackup.class))
            && rule.getVerbs().containsAll(
                List.of("list", "get", "create", "patch", "update", "delete"))));
  }

  @Test
  void generateResource_roleHasRuleForObjectStorageCrd() {
    List<HasMetadata> resources = shardedBackupCronRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(
                HasMetadata.getPlural(StackGresObjectStorage.class))
            && rule.getVerbs().contains("get")
            && rule.getVerbs().size() == 1));
  }

  @Test
  void generateResource_allResourcesNamedCorrectly() {
    List<HasMetadata> resources = shardedBackupCronRole.generateResource(context).toList();
    String expectedName = ShardedBackupCronRole.roleName(cluster);

    assertEquals(expectedName, findResource(resources, Role.class).getMetadata().getName());
    assertEquals(expectedName,
        findResource(resources, ServiceAccount.class).getMetadata().getName());
    assertEquals(expectedName,
        findResource(resources, RoleBinding.class).getMetadata().getName());
  }

  @Test
  void generateResource_roleBindingReferencesCorrectRoleAndSA() {
    final List<HasMetadata> resources = shardedBackupCronRole.generateResource(context).toList();
    final RoleBinding roleBinding = findResource(resources, RoleBinding.class);

    final String expectedName = ShardedBackupCronRole.roleName(cluster);
    final String expectedNamespace = cluster.getMetadata().getNamespace();

    assertEquals("Role", roleBinding.getRoleRef().getKind());
    assertEquals(expectedName, roleBinding.getRoleRef().getName());
    assertEquals("rbac.authorization.k8s.io", roleBinding.getRoleRef().getApiGroup());

    assertEquals(1, roleBinding.getSubjects().size());
    assertEquals("ServiceAccount", roleBinding.getSubjects().get(0).getKind());
    assertEquals(expectedName, roleBinding.getSubjects().get(0).getName());
    assertEquals(expectedNamespace, roleBinding.getSubjects().get(0).getNamespace());
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
