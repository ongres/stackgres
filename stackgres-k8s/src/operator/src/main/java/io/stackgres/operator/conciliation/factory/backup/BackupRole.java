/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.stackgres.common.LabelFactoryForBackup;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.cluster.backup.BackupCronRole;
import io.stackgres.operatorframework.resource.ResourceUtil;

@Singleton
@OperatorVersionBinder
public class BackupRole implements ResourceGenerator<StackGresBackupContext> {

  public static final String SUFFIX = "-backup-copy";

  private LabelFactoryForBackup labelFactory;

  public static String roleName(StackGresBackupContext context) {
    return roleName(context.getSource());
  }

  public static String roleName(StackGresBackup backup) {
    return roleName(backup.getMetadata().getName());
  }

  public static String roleName(String backupName) {
    return ResourceUtil.resourceName(backupName + SUFFIX);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresBackupContext context) {
    if (isNotBackupCopy(context)) {
      return Stream.of();
    }
    return Stream.of(
        createRole(context),
        createRoleBinding(context));
  }

  private boolean isNotBackupCopy(StackGresBackupContext context) {
    return Objects.equals(
        context.getSource().getSpec().getSgCluster(),
        context.getCluster().getMetadata().getName());
  }

  private Role createRole(StackGresBackupContext context) {
    final StackGresBackup backup = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(backup);
    return new RoleBuilder()
        .withNewMetadata()
        .withName(roleName(context))
        .withNamespace(backup.getMetadata().getNamespace())
        .withLabels(labels)
        .endMetadata()
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(HasMetadata.getPlural(StackGresBackup.class))
            .withVerbs("list")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(HasMetadata.getPlural(StackGresBackup.class))
            .withVerbs("get", "patch", "update", "delete")
            .withResourceNames(backup.getMetadata().getName())
            .build())
        .build();
  }

  private RoleBinding createRoleBinding(StackGresBackupContext context) {
    final StackGresBackup backup = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(backup);
    return new RoleBindingBuilder()
        .withNewMetadata()
        .withName(roleName(context))
        .withNamespace(backup.getMetadata().getNamespace())
        .withLabels(labels)
        .endMetadata()
        .withSubjects(new SubjectBuilder()
            .withKind("ServiceAccount")
            .withName(BackupCronRole.roleName(context.getCluster()))
            .withNamespace(context.getCluster().getMetadata().getNamespace())
            .build())
        .withRoleRef(new RoleRefBuilder()
            .withKind("Role")
            .withName(roleName(context))
            .withApiGroup("rbac.authorization.k8s.io")
            .build())
        .build();
  }

  @Inject
  public void setLabelFactory(LabelFactoryForBackup labelFactory) {
    this.labelFactory = labelFactory;
  }

}
