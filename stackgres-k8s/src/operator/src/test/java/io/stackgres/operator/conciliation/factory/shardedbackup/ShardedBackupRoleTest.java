/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedbackup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedBackup;
import io.stackgres.common.labels.ShardedBackupLabelFactory;
import io.stackgres.common.labels.ShardedBackupLabelMapper;
import io.stackgres.operator.conciliation.factory.shardedcluster.backup.ShardedBackupCronRole;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedBackupRoleTest {

  private final LabelFactoryForShardedBackup labelFactory =
      new ShardedBackupLabelFactory(new ShardedBackupLabelMapper());

  @Mock
  private StackGresShardedBackupContext context;

  private ShardedBackupRole shardedBackupRole;

  private StackGresShardedBackup backup;

  private StackGresShardedCluster shardedCluster;

  @BeforeEach
  void setUp() {
    shardedBackupRole = new ShardedBackupRole();
    shardedBackupRole.setLabelFactory(labelFactory);
    backup = Fixtures.shardedBackup().loadDefault().get();
    shardedCluster = Fixtures.shardedCluster().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(backup);
  }

  @Test
  void generateResource_whenNotBackupCopy_shouldReturnEmpty() {
    List<HasMetadata> resources = shardedBackupRole.generateResource(context).toList();
    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenBackupCopy_shouldGenerateRoleAndRoleBinding() {
    backup.getSpec().setSgShardedCluster("other-namespace.my-cluster");
    when(context.getShardedCluster()).thenReturn(shardedCluster);

    List<HasMetadata> resources = shardedBackupRole.generateResource(context).toList();

    assertEquals(2, resources.size());
    assertTrue(resources.stream().anyMatch(Role.class::isInstance));
    assertTrue(resources.stream().anyMatch(RoleBinding.class::isInstance));
  }

  @Test
  void generateResource_roleHasCorrectPolicyRules() {
    backup.getSpec().setSgShardedCluster("other-namespace.my-cluster");
    when(context.getShardedCluster()).thenReturn(shardedCluster);

    List<HasMetadata> resources = shardedBackupRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertEquals(2, role.getRules().size());
    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresShardedBackup.class))
            && rule.getVerbs().equals(List.of("list"))));
    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresShardedBackup.class))
            && rule.getVerbs().containsAll(List.of("get", "patch", "update", "delete"))
            && rule.getResourceNames().contains(backup.getMetadata().getName())));
  }

  @Test
  void generateResource_roleBindingReferencesCorrectRoleAndSA() {
    backup.getSpec().setSgShardedCluster("other-namespace.my-cluster");
    when(context.getShardedCluster()).thenReturn(shardedCluster);

    List<HasMetadata> resources = shardedBackupRole.generateResource(context).toList();
    RoleBinding roleBinding = findResource(resources, RoleBinding.class);

    String expectedRoleName = ShardedBackupRole.roleName(backup);
    assertEquals("Role", roleBinding.getRoleRef().getKind());
    assertEquals(expectedRoleName, roleBinding.getRoleRef().getName());
    assertEquals("rbac.authorization.k8s.io", roleBinding.getRoleRef().getApiGroup());

    assertEquals(1, roleBinding.getSubjects().size());
    assertEquals("ServiceAccount", roleBinding.getSubjects().get(0).getKind());
    assertEquals(ShardedBackupCronRole.roleName(shardedCluster),
        roleBinding.getSubjects().get(0).getName());
    assertEquals(shardedCluster.getMetadata().getNamespace(),
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
