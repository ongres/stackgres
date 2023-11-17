/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedbackup;

import java.util.Map;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgshardedbackup.StackGresShardedBackup;
import io.stackgres.common.labels.LabelFactoryForShardedBackup;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.factory.shardedcluster.backup.ShardedBackupCronRole;
import io.stackgres.operator.conciliation.shardedbackup.StackGresShardedBackupContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class ShardedBackupRole implements ResourceGenerator<StackGresShardedBackupContext> {

  public static final String SUFFIX = "-backup-copy";

  private LabelFactoryForShardedBackup labelFactory;

  public static String roleName(StackGresShardedBackupContext context) {
    return roleName(context.getSource());
  }

  public static String roleName(StackGresShardedBackup backup) {
    return roleName(backup.getMetadata().getName());
  }

  public static String roleName(String backupName) {
    return ResourceUtil.resourceName(backupName + SUFFIX);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresShardedBackupContext context) {
    if (isNotBackupCopy(context)) {
      return Stream.of();
    }
    return Stream.of(
        createRole(context),
        createRoleBinding(context));
  }

  private boolean isNotBackupCopy(StackGresShardedBackupContext context) {
    return !StackGresUtil.isRelativeIdNotInSameNamespace(
        context.getSource().getSpec().getSgShardedCluster());
  }

  private Role createRole(StackGresShardedBackupContext context) {
    final StackGresShardedBackup backup = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(backup);
    return new RoleBuilder()
        .withNewMetadata()
        .withName(roleName(context))
        .withNamespace(backup.getMetadata().getNamespace())
        .withLabels(labels)
        .endMetadata()
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(HasMetadata.getPlural(StackGresShardedBackup.class))
            .withVerbs("list")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(HasMetadata.getPlural(StackGresShardedBackup.class))
            .withVerbs("get", "patch", "update", "delete")
            .withResourceNames(backup.getMetadata().getName())
            .build())
        .build();
  }

  private RoleBinding createRoleBinding(StackGresShardedBackupContext context) {
    final StackGresShardedBackup backup = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(backup);
    return new RoleBindingBuilder()
        .withNewMetadata()
        .withName(roleName(context))
        .withNamespace(backup.getMetadata().getNamespace())
        .withLabels(labels)
        .endMetadata()
        .withSubjects(new SubjectBuilder()
            .withKind("ServiceAccount")
            .withName(ShardedBackupCronRole.roleName(context.getShardedCluster()))
            .withNamespace(context.getShardedCluster().getMetadata().getNamespace())
            .build())
        .withRoleRef(new RoleRefBuilder()
            .withKind("Role")
            .withName(roleName(context))
            .withApiGroup("rbac.authorization.k8s.io")
            .build())
        .build();
  }

  @Inject
  public void setLabelFactory(LabelFactoryForShardedBackup labelFactory) {
    this.labelFactory = labelFactory;
  }

}
