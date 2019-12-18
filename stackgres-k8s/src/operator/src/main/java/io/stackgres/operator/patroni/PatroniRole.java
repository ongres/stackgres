/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.stackgres.operator.common.StackGresUtil;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupDefinition;
import io.stackgres.operator.customresource.sgcluster.StackGresCluster;
import io.stackgres.operator.resource.ResourceUtil;

public class PatroniRole {

  public static final String SUFFIX = "-patroni";

  /**
   * Create the ServiceAccount for patroni associated to the cluster.
   */
  public static ServiceAccount createServiceAccount(StackGresCluster cluster) {
    return new ServiceAccountBuilder()
          .withNewMetadata()
          .withName(cluster.getMetadata().getName() + SUFFIX)
          .withNamespace(cluster.getMetadata().getNamespace())
          .withLabels(ResourceUtil.defaultLabels(cluster.getMetadata().getName()))
          .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(cluster)))
          .endMetadata()
          .build();
  }

  /**
   * Create the Role for patroni associated to the cluster.
   */
  public static Role createRole(StackGresCluster cluster) {
    return new RoleBuilder()
        .withNewMetadata()
        .withName(cluster.getMetadata().getName() + SUFFIX)
        .withNamespace(cluster.getMetadata().getNamespace())
        .withLabels(ResourceUtil.defaultLabels(cluster.getMetadata().getName()))
        .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(cluster)))
        .endMetadata()
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("endpoints", "configmaps")
            .withVerbs("create", "get", "list", "patch", "update", "watch")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("secrets")
            .withVerbs("get")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("pods")
            .withVerbs("get", "list", "patch", "update", "watch")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("pods/exec")
            .withVerbs("create")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("batch")
            .withResources("cronjobs")
            .withVerbs("get", "patch")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("services")
            .withVerbs("create")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(StackGresUtil.CRD_GROUP)
            .withResources(StackGresBackupDefinition.PLURAL)
            .withVerbs("list", "get", "create", "patch", "delete")
            .build())
        .build();
  }

  /**
   * Create the RoleBinding for patroni associated to the cluster.
   */
  public static RoleBinding createRoleBinding(StackGresCluster cluster) {
    return new RoleBindingBuilder()
        .withNewMetadata()
        .withName(cluster.getMetadata().getName() + SUFFIX)
        .withNamespace(cluster.getMetadata().getNamespace())
        .withLabels(ResourceUtil.defaultLabels(cluster.getMetadata().getName()))
        .withOwnerReferences(ImmutableList.of(ResourceUtil.getOwnerReference(cluster)))
        .endMetadata()
        .withSubjects(new SubjectBuilder()
            .withKind("ServiceAccount")
            .withName(cluster.getMetadata().getName() + SUFFIX)
            .withNamespace(cluster.getMetadata().getNamespace())
            .build())
        .withRoleRef(new RoleRefBuilder()
            .withKind("Role")
            .withName(cluster.getMetadata().getName() + SUFFIX)
            .withApiGroup("rbac.authorization.k8s.io")
            .build())
        .build();
  }

}
