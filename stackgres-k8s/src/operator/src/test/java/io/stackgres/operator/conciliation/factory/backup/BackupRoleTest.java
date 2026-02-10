/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.BackupLabelFactory;
import io.stackgres.common.labels.BackupLabelMapper;
import io.stackgres.common.labels.LabelFactoryForBackup;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupCronRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupRoleTest {

  private final LabelFactoryForBackup labelFactory =
      new BackupLabelFactory(new BackupLabelMapper());

  @Mock
  private StackGresBackupContext context;

  private BackupRole backupRole;

  private StackGresBackup backup;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    backupRole = new BackupRole();
    backupRole.setLabelFactory(labelFactory);
    backup = Fixtures.backup().loadDefault().get();
    cluster = Fixtures.cluster().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(backup);
  }

  @Test
  void generateResource_whenNotBackupCopy_shouldReturnEmpty() {
    // Default sgCluster has no dot (same namespace)
    List<HasMetadata> resources = backupRole.generateResource(context).toList();
    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenBackupCopyButNoCluster_shouldReturnEmpty() {
    backup.getSpec().setSgCluster("other-namespace.my-cluster");
    when(context.getFoundCluster()).thenReturn(Optional.empty());

    List<HasMetadata> resources = backupRole.generateResource(context).toList();
    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenBackupCopy_shouldGenerateRoleAndRoleBinding() {
    backup.getSpec().setSgCluster("other-namespace.my-cluster");
    when(context.getFoundCluster()).thenReturn(Optional.of(cluster));
    lenient().when(context.getCluster()).thenReturn(cluster);

    List<HasMetadata> resources = backupRole.generateResource(context).toList();

    assertEquals(2, resources.size());
    assertTrue(resources.stream().anyMatch(Role.class::isInstance));
    assertTrue(resources.stream().anyMatch(RoleBinding.class::isInstance));
  }

  @Test
  void generateResource_roleHasCorrectPolicyRules() {
    backup.getSpec().setSgCluster("other-namespace.my-cluster");
    when(context.getFoundCluster()).thenReturn(Optional.of(cluster));
    lenient().when(context.getCluster()).thenReturn(cluster);

    List<HasMetadata> resources = backupRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertEquals(2, role.getRules().size());
    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresBackup.class))
            && rule.getVerbs().equals(List.of("list"))));
    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresBackup.class))
            && rule.getVerbs().containsAll(List.of("get", "patch", "update", "delete"))
            && rule.getResourceNames().contains(backup.getMetadata().getName())));
  }

  @Test
  void generateResource_roleBindingReferencesCorrectRoleAndSA() {
    backup.getSpec().setSgCluster("other-namespace.my-cluster");
    when(context.getFoundCluster()).thenReturn(Optional.of(cluster));
    when(context.getCluster()).thenReturn(cluster);

    List<HasMetadata> resources = backupRole.generateResource(context).toList();
    RoleBinding roleBinding = findResource(resources, RoleBinding.class);

    String expectedRoleName = BackupRole.roleName(backup);
    assertEquals("Role", roleBinding.getRoleRef().getKind());
    assertEquals(expectedRoleName, roleBinding.getRoleRef().getName());
    assertEquals("rbac.authorization.k8s.io", roleBinding.getRoleRef().getApiGroup());

    assertEquals(1, roleBinding.getSubjects().size());
    assertEquals("ServiceAccount", roleBinding.getSubjects().get(0).getKind());
    assertEquals(BackupCronRole.roleName(cluster), roleBinding.getSubjects().get(0).getName());
    assertEquals(cluster.getMetadata().getNamespace(),
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
