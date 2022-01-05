/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.cluster.factory;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.stackgres.common.StackGresVersion;
import io.stackgres.operator.conciliation.GeneratorTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class PatroniRoleGeneratorTest extends GeneratorTest {

  @Test
  @DisplayName("Test patroni role for 0.9.5")
  void testPatroniRoleForV095() {

    var expectedRole = new RoleBuilder()
        .withNewMetadata()
        .withNamespace(CLUSTER_NAMESPACE)
        .withName(CLUSTER_NAME + "-patroni")
        .withAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .withLabels(Map.of(
            "cluster-uid", CLUSTER_UID,
            "cluster-name", CLUSTER_NAME,
            "app", "StackGresCluster"
        ))
        .endMetadata()
        .withRules(List.of(
            new PolicyRuleBuilder()
                .withApiGroups("")
                .withResources("endpoints", "configmaps")
                .withVerbs("create", "get", "list", "patch", "update", "watch")
                .build(),
            new PolicyRuleBuilder()
                .withApiGroups("")
                .withResources("secrets")
                .withVerbs("get")
                .build(),
            new PolicyRuleBuilder()
                .withApiGroups("")
                .withResources("pods")
                .withVerbs("get", "list", "patch", "update", "watch")
                .build(),
            new PolicyRuleBuilder()
                .withApiGroups("")
                .withResources("pods/exec")
                .withVerbs("create")
                .build(),
            new PolicyRuleBuilder()
                .withApiGroups("batch")
                .withResources("cronjobs")
                .withVerbs("get", "patch")
                .build(),
            new PolicyRuleBuilder()
                .withApiGroups("")
                .withResources("services")
                .withVerbs("create")
                .build(),
            new PolicyRuleBuilder()
                .withApiGroups("stackgres.io")
                .withResources("sgbackups")
                .withVerbs("list", "get", "create", "patch", "delete")
                .build(),
            new PolicyRuleBuilder()
                .withApiGroups("stackgres.io")
                .withResources("sgbackupconfigs")
                .withVerbs("get")
                .build()
        ))
        .build();

    givenAClusterWithVersion(StackGresVersion.V09_LAST)
        .andAllResourceAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .generatedResourceShouldBeEqualTo(expectedRole);

  }

  @Test
  @DisplayName("Test patroni role binding for 0.9.5")
  void testPatroniRoleBindingForV095() {

    var expectedRoleBinding = new RoleBindingBuilder()
        .withNewMetadata()
        .withNamespace(CLUSTER_NAMESPACE)
        .withName(CLUSTER_NAME + "-patroni")
        .withAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .withLabels(Map.of(
            "cluster-uid", CLUSTER_UID,
            "cluster-name", CLUSTER_NAME,
            "app", "StackGresCluster"
        ))
        .endMetadata()
        .withRoleRef(new RoleRefBuilder()
            .withKind("Role")
            .withName(CLUSTER_NAME + "-patroni")
            .withApiGroup("rbac.authorization.k8s.io")
            .build())
        .withSubjects(new SubjectBuilder()
            .withKind("ServiceAccount")
            .withName(CLUSTER_NAME + "-patroni")
            .withNamespace(cluster.getMetadata().getNamespace())
            .build())
        .build();

    givenAClusterWithVersion(StackGresVersion.V09_LAST)
        .andAllResourceAnnotations(Map.of("allResourceAnnotation", "allResourceValue"))
        .generatedResourceShouldBeEqualTo(expectedRoleBinding);

  }

}
