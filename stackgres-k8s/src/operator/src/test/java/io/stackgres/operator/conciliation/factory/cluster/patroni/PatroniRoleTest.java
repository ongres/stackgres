/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.LocalObjectReference;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotationsBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabelsBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadataBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
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
class PatroniRoleTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private StackGresClusterContext context;

  private final PatroniRole patroniRole = new PatroniRole();

  private StackGresCluster cluster;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    config = Fixtures.config().loadDefault().get();
    patroniRole.setLabelFactory(labelFactory);
    when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getCluster()).thenReturn(cluster);
    when(context.getConfig()).thenReturn(config);
  }

  @Test
  void generateResource_shouldGenerateThreeResources() {
    List<HasMetadata> resources = patroniRole.generateResource(context).toList();

    assertEquals(3, resources.size());
    assertTrue(resources.get(0) instanceof ServiceAccount);
    assertTrue(resources.get(1) instanceof Role);
    assertTrue(resources.get(2) instanceof RoleBinding);
  }

  @Test
  void generateResource_roleHasPolicyRules() {
    List<HasMetadata> resources = patroniRole.generateResource(context).toList();

    Role role = (Role) resources.get(1);
    assertNotNull(role.getRules());
    assertEquals(14, role.getRules().size());
  }

  @Test
  void generateResource_roleBindingReferencesCorrectRoleAndServiceAccount() {
    List<HasMetadata> resources = patroniRole.generateResource(context).toList();

    String expectedName = PatroniUtil.roleName(cluster);
    String expectedNamespace = cluster.getMetadata().getNamespace();

    RoleBinding roleBinding = (RoleBinding) resources.get(2);
    assertEquals(expectedName, roleBinding.getMetadata().getName());
    assertEquals(expectedNamespace, roleBinding.getMetadata().getNamespace());

    assertEquals(1, roleBinding.getSubjects().size());
    assertEquals("ServiceAccount", roleBinding.getSubjects().get(0).getKind());
    assertEquals(expectedName, roleBinding.getSubjects().get(0).getName());
    assertEquals(expectedNamespace, roleBinding.getSubjects().get(0).getNamespace());

    assertEquals("Role", roleBinding.getRoleRef().getKind());
    assertEquals(expectedName, roleBinding.getRoleRef().getName());
    assertEquals("rbac.authorization.k8s.io", roleBinding.getRoleRef().getApiGroup());
  }

  @Test
  void generateResource_serviceAccountHasCustomLabelsAndAnnotations() {
    cluster.getSpec().setMetadata(new StackGresClusterSpecMetadataBuilder()
        .withLabels(new StackGresClusterSpecLabelsBuilder()
            .withServiceAccount(Map.of("custom-label", "label-value"))
            .build())
        .withAnnotations(new StackGresClusterSpecAnnotationsBuilder()
            .withServiceAccount(Map.of("custom-annotation", "annotation-value"))
            .build())
        .build());

    List<HasMetadata> resources = patroniRole.generateResource(context).toList();

    ServiceAccount serviceAccount = (ServiceAccount) resources.get(0);
    assertEquals("label-value",
        serviceAccount.getMetadata().getLabels().get("custom-label"));
    assertEquals("annotation-value",
        serviceAccount.getMetadata().getAnnotations().get("custom-annotation"));
  }

  @Test
  void generateResource_serviceAccountIncludesImagePullSecrets() {
    config.getSpec().setImagePullSecrets(List.of(new LocalObjectReference("my-registry-secret")));

    List<HasMetadata> resources = patroniRole.generateResource(context).toList();

    ServiceAccount serviceAccount = (ServiceAccount) resources.get(0);
    assertNotNull(serviceAccount.getImagePullSecrets());
    assertEquals(1, serviceAccount.getImagePullSecrets().size());
    assertEquals("my-registry-secret",
        serviceAccount.getImagePullSecrets().get(0).getName());
  }

  @Test
  void generateResource_withNullMetadata_shouldNotThrowNpe() {
    cluster.getSpec().setMetadata(null);

    List<HasMetadata> resources = assertDoesNotThrow(
        () -> patroniRole.generateResource(context).toList());

    assertEquals(3, resources.size());
    ServiceAccount serviceAccount = (ServiceAccount) resources.get(0);
    assertNotNull(serviceAccount.getMetadata().getName());
  }
}
