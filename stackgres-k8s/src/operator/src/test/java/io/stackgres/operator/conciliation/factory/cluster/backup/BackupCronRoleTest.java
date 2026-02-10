/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.stackgres.common.VolumeSnapshotUtil;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.LocalObjectReference;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupCronRoleTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private StackGresClusterContext context;

  private BackupCronRole backupCronRole;

  private StackGresCluster cluster;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    backupCronRole = new BackupCronRole();
    backupCronRole.setLabelFactory(labelFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    config = Fixtures.config().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);
    when(context.getConfig()).thenReturn(config);
  }

  @Test
  void generateResource_shouldContainServiceAccountRoleAndRoleBinding() {
    List<HasMetadata> resources = backupCronRole.generateResource(context).toList();

    assertEquals(3, resources.size());
    assertTrue(resources.stream().anyMatch(ServiceAccount.class::isInstance));
    assertTrue(resources.stream().anyMatch(Role.class::isInstance));
    assertTrue(resources.stream().anyMatch(RoleBinding.class::isInstance));
  }

  @Test
  void generateResource_roleHasRuleForPods() {
    final List<HasMetadata> resources = backupCronRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(HasMetadata.getGroup(Pod.class))
            && rule.getResources().contains(HasMetadata.getPlural(Pod.class))
            && rule.getVerbs().containsAll(List.of("get", "list"))));
  }

  @Test
  void generateResource_roleHasRuleForPodsExec() {
    final List<HasMetadata> resources = backupCronRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getResources().contains(HasMetadata.getPlural(Pod.class) + "/exec")
            && rule.getVerbs().contains("create")));
  }

  @Test
  void generateResource_roleHasRuleForSgCluster() {
    final List<HasMetadata> resources = backupCronRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresCluster.class))
            && rule.getVerbs().containsAll(List.of("get", "patch"))));
  }

  @Test
  void generateResource_roleHasRuleForSgBackup() {
    final List<HasMetadata> resources = backupCronRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresBackup.class))
            && rule.getVerbs().containsAll(
                List.of("list", "get", "create", "patch", "update", "delete"))));
  }

  @Test
  void generateResource_roleHasRuleForSgObjectStorage() {
    final List<HasMetadata> resources = backupCronRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(CommonDefinition.GROUP)
            && rule.getResources().contains(HasMetadata.getPlural(StackGresObjectStorage.class))
            && rule.getVerbs().contains("get")));
  }

  @Test
  void generateResource_roleHasRuleForVolumeSnapshots() {
    final List<HasMetadata> resources = backupCronRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);

    assertTrue(role.getRules().stream().anyMatch(rule ->
        rule.getApiGroups().contains(VolumeSnapshotUtil.VOLUME_SNAPSHOT_GROUP)
            && rule.getResources().contains(VolumeSnapshotUtil.VOLUME_SNAPSHOT_CRD_PLURAL)
            && rule.getVerbs().containsAll(List.of("create", "get", "list", "watch"))));
  }

  @Test
  void generateResource_roleHasExpectedNumberOfRules() {
    final List<HasMetadata> resources = backupCronRole.generateResource(context).toList();
    Role role = findResource(resources, Role.class);
    List<PolicyRule> rules = role.getRules();
    assertEquals(6, rules.size());
  }

  @Test
  void generateResource_allResourcesNamedWithBackupSuffix() {
    final List<HasMetadata> resources = backupCronRole.generateResource(context).toList();
    String expectedName = cluster.getMetadata().getName() + BackupCronRole.SUFFIX;

    assertEquals(expectedName, findResource(resources, Role.class).getMetadata().getName());
    assertEquals(expectedName, findResource(resources, ServiceAccount.class).getMetadata().getName());
    assertEquals(expectedName, findResource(resources, RoleBinding.class).getMetadata().getName());
  }

  @Test
  void generateResource_serviceAccountHasImagePullSecrets() {
    LocalObjectReference pullSecret = new LocalObjectReference("my-registry-secret");
    config.getSpec().setImagePullSecrets(List.of(pullSecret));

    final List<HasMetadata> resources = backupCronRole.generateResource(context).toList();

    ServiceAccount serviceAccount = findResource(resources, ServiceAccount.class);

    assertEquals(1, serviceAccount.getImagePullSecrets().size());
    assertEquals("my-registry-secret", serviceAccount.getImagePullSecrets().get(0).getName());
  }

  @Test
  void generateResource_roleBindingReferencesCorrectRoleAndSA() {
    final List<HasMetadata> resources = backupCronRole.generateResource(context).toList();
    final RoleBinding roleBinding = findResource(resources, RoleBinding.class);

    final String expectedName = cluster.getMetadata().getName() + BackupCronRole.SUFFIX;
    final String expectedNamespace = cluster.getMetadata().getNamespace();

    assertEquals("Role", roleBinding.getRoleRef().getKind());
    assertEquals(expectedName, roleBinding.getRoleRef().getName());
    assertEquals("rbac.authorization.k8s.io", roleBinding.getRoleRef().getApiGroup());

    assertEquals(1, roleBinding.getSubjects().size());
    assertEquals("ServiceAccount", roleBinding.getSubjects().get(0).getKind());
    assertEquals(expectedName, roleBinding.getSubjects().get(0).getName());
    assertEquals(expectedNamespace, roleBinding.getSubjects().get(0).getNamespace());
  }

  @Test
  void generateResource_allResourcesHaveCorrectNamespaceAndLabels() {
    final String expectedNamespace = cluster.getMetadata().getNamespace();

    final List<HasMetadata> resources = backupCronRole.generateResource(context).toList();

    for (HasMetadata resource : resources) {
      assertEquals(expectedNamespace, resource.getMetadata().getNamespace());
      assertFalse(resource.getMetadata().getLabels().isEmpty());
    }
  }

  @SuppressWarnings("unchecked")
  private <T extends HasMetadata> T findResource(List<HasMetadata> resources, Class<T> resourceClass) {
    return (T) resources.stream()
        .filter(resourceClass::isInstance)
        .findFirst()
        .orElseThrow(() -> new AssertionError("No resource of type " + resourceClass.getSimpleName()));
  }
}
