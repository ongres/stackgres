/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni.factory;

import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.crd.sgbackup.StackGresBackupDefinition;
import io.stackgres.common.crd.sgbackupconfig.StackGresBackupConfigDefinition;
import io.stackgres.operator.common.StackGresClusterContext;
import io.stackgres.operator.common.StackGresClusterResourceStreamFactory;
import io.stackgres.operator.common.StackGresGeneratorContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.jooq.lambda.Seq;

@ApplicationScoped
public class PatroniRole implements StackGresClusterResourceStreamFactory {

  public static final String SUFFIX = "-patroni";

  public static String roleName(StackGresClusterContext clusterContext) {
    return roleName(clusterContext.getCluster().getMetadata().getName());
  }

  public static String roleName(String clusterName) {
    return ResourceUtil.resourceName(clusterName + SUFFIX);
  }

  @Override
  public Stream<HasMetadata> streamResources(StackGresGeneratorContext context) {
    return Seq.of(
        createServiceAccount(context),
        createRole(context),
        createRoleBinding(context));
  }

  /**
   * Create the ServiceAccount for patroni associated to the cluster.
   */
  private ServiceAccount createServiceAccount(StackGresGeneratorContext context) {
    return new ServiceAccountBuilder()
          .withNewMetadata()
          .withName(roleName(context.getClusterContext()))
          .withNamespace(context.getClusterContext().getCluster().getMetadata().getNamespace())
          .withLabels(context.getClusterContext().clusterLabels())
          .withOwnerReferences(context.getClusterContext().ownerReferences())
          .endMetadata()
          .build();
  }

  /**
   * Create the Role for patroni associated to the cluster.
   */
  private Role createRole(StackGresGeneratorContext context) {
    return new RoleBuilder()
        .withNewMetadata()
        .withName(roleName(context.getClusterContext()))
        .withNamespace(context.getClusterContext().getCluster().getMetadata().getNamespace())
        .withLabels(context.getClusterContext().clusterLabels())
        .withOwnerReferences(context.getClusterContext().ownerReferences())
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
            .withApiGroups(StackGresContext.CRD_GROUP)
            .withResources(StackGresBackupDefinition.PLURAL)
            .withVerbs("list", "get", "create", "patch", "delete")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(StackGresContext.CRD_GROUP)
            .withResources(StackGresBackupConfigDefinition.PLURAL)
            .withVerbs("get")
            .build())
        .build();
  }

  /**
   * Create the RoleBinding for patroni associated to the cluster.
   */
  private RoleBinding createRoleBinding(StackGresGeneratorContext context) {
    return new RoleBindingBuilder()
        .withNewMetadata()
        .withName(roleName(context.getClusterContext()))
        .withNamespace(context.getClusterContext().getCluster().getMetadata().getNamespace())
        .withLabels(context.getClusterContext().clusterLabels())
        .withOwnerReferences(context.getClusterContext().ownerReferences())
        .endMetadata()
        .withSubjects(new SubjectBuilder()
            .withKind("ServiceAccount")
            .withName(roleName(context.getClusterContext()))
            .withNamespace(context.getClusterContext().getCluster().getMetadata().getNamespace())
            .build())
        .withRoleRef(new RoleRefBuilder()
            .withKind("Role")
            .withName(roleName(context.getClusterContext()))
            .withApiGroup("rbac.authorization.k8s.io")
            .build())
        .build();
  }

}
