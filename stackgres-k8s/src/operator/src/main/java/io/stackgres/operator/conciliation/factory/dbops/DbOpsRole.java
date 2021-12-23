/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.dbops;

import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

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
import io.stackgres.common.LabelFactoryForDbOps;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.operator.common.StackGresVersion;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.dbops.StackGresDbOpsContext;
import io.stackgres.operatorframework.resource.ResourceUtil;

@Singleton
@OperatorVersionBinder(startAt = StackGresVersion.V09, stopAt = StackGresVersion.V11)
public class DbOpsRole implements ResourceGenerator<StackGresDbOpsContext> {

  public static final String SUFFIX = "-dbops";

  private LabelFactoryForDbOps labelFactory;

  public static String roleName(StackGresDbOpsContext context) {
    return roleName(context.getSource());
  }

  public static String roleName(StackGresDbOps dbOps) {
    return roleName(dbOps.getMetadata().getName());
  }

  public static String roleName(String dbOpsName) {
    return ResourceUtil.resourceName(dbOpsName + SUFFIX);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresDbOpsContext context) {
    return Stream.of(
        createServiceAccount(context),
        createRole(context),
        createRoleBinding(context));
  }

  /**
   * Create the ServiceAccount for Job associated to the dbOps.
   */
  private ServiceAccount createServiceAccount(StackGresDbOpsContext context) {
    final StackGresDbOps dbOps = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(context.getSource());

    final String serviceAccountName = roleName(context);
    final String serviceAccountNamespace = dbOps.getMetadata().getNamespace();
    return new ServiceAccountBuilder()
        .withNewMetadata()
        .withName(serviceAccountName)
        .withNamespace(serviceAccountNamespace)
        .withLabels(labels)
        .endMetadata()
        .build();

  }

  /**
   * Create the Role for Job associated to the dbOps.
   */
  private Role createRole(StackGresDbOpsContext context) {
    final StackGresDbOps dbOps = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(context.getSource());
    return new RoleBuilder()
        .withNewMetadata()
        .withName(roleName(context))
        .withNamespace(dbOps.getMetadata().getNamespace())
        .withLabels(labels)
        .endMetadata()
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("pods")
            .withVerbs("get", "list", "watch", "delete")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("pods/log")
            .withVerbs("get")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("pods/exec")
            .withVerbs("create")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("services", "secrets", "endpoints")
            .withVerbs("get", "list")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("apps")
            .withResources("statefulsets")
            .withVerbs("get", "delete")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("events")
            .withVerbs("get", "list", "create", "patch", "update")
            .build())

        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(HasMetadata.getPlural(StackGresDbOps.class))
            .withVerbs("get", "list", "watch", "patch", "update")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(HasMetadata.getPlural(StackGresCluster.class))
            .withVerbs("get", "list", "watch", "patch", "update")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(
                HasMetadata.getPlural(StackGresCluster.class) + "/status")
            .withVerbs("update")
            .build())
        .build();
  }

  /**
   * Create the RoleBinding for Job associated to the dbOps.
   */
  private RoleBinding createRoleBinding(StackGresDbOpsContext context) {
    final StackGresDbOps dbOps = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(context.getSource());
    return new RoleBindingBuilder()
        .withNewMetadata()
        .withName(roleName(context))
        .withNamespace(dbOps.getMetadata().getNamespace())
        .withLabels(labels)
        .endMetadata()
        .withSubjects(new SubjectBuilder()
            .withKind("ServiceAccount")
            .withName(roleName(context))
            .withNamespace(dbOps.getMetadata().getNamespace())
            .build())
        .withRoleRef(new RoleRefBuilder()
            .withKind("Role")
            .withName(roleName(context))
            .withApiGroup("rbac.authorization.k8s.io")
            .build())
        .build();
  }

  @Inject
  public void setLabelFactory(LabelFactoryForDbOps labelFactory) {
    this.labelFactory = labelFactory;
  }

}
