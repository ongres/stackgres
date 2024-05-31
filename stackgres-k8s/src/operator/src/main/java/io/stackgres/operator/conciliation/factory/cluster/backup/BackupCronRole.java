/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.backup;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.stackgres.common.VolumeSnapshotUtil;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class BackupCronRole implements ResourceGenerator<StackGresClusterContext> {

  public static final String SUFFIX = "-backup";

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  public static String roleName(StackGresClusterContext context) {
    return roleName(context.getSource());
  }

  public static String roleName(StackGresCluster cluster) {
    return roleName(cluster.getMetadata().getName());
  }

  public static String roleName(String clusterName) {
    return ResourceUtil.resourceName(clusterName + SUFFIX);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    return Stream.of(
        createServiceAccount(context),
        createRole(context),
        createRoleBinding(context));
  }

  private ServiceAccount createServiceAccount(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(cluster);
    final String serviceAccountName = roleName(context);
    final String serviceAccountNamespace = cluster.getMetadata().getNamespace();

    return new ServiceAccountBuilder()
        .withNewMetadata()
        .withName(serviceAccountName)
        .withNamespace(serviceAccountNamespace)
        .withLabels(labels)
        .endMetadata()
        .withImagePullSecrets(Optional.ofNullable(context.getConfig().getSpec().getImagePullSecrets())
            .stream()
            .map(LocalObjectReference.class::cast)
            .toList())
        .build();

  }

  private Role createRole(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(cluster);
    return new RoleBuilder()
        .withNewMetadata()
        .withName(roleName(context))
        .withNamespace(cluster.getMetadata().getNamespace())
        .withLabels(labels)
        .endMetadata()
        .addToRules(
            new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(Pod.class))
            .withResources(HasMetadata.getPlural(Pod.class))
            .withVerbs("get", "list")
            .build())
        .addToRules(
            new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(Pod.class))
            .withResources(HasMetadata.getPlural(Pod.class) + "/exec")
            .withVerbs("create")
            .build())
        .addToRules(
            new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(HasMetadata.getPlural(StackGresCluster.class))
            .withVerbs("get", "patch")
            .build())
        .addToRules(
            new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(HasMetadata.getPlural(StackGresBackup.class))
            .withVerbs("list", "get", "create", "patch", "update", "delete")
            .build())
        .addToRules(
            new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(HasMetadata.getPlural(StackGresObjectStorage.class))
            .withVerbs("get")
            .build())
        .addToRules(
            new PolicyRuleBuilder()
            .withApiGroups(VolumeSnapshotUtil.VOLUME_SNAPSHOT_GROUP)
            .withResources(VolumeSnapshotUtil.VOLUME_SNAPSHOT_CRD_PLURAL)
            .withVerbs("create", "get", "list", "watch")
            .build())
        .build();
  }

  private RoleBinding createRoleBinding(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(cluster);
    return new RoleBindingBuilder()
        .withNewMetadata()
        .withName(roleName(context))
        .withNamespace(cluster.getMetadata().getNamespace())
        .withLabels(labels)
        .endMetadata()
        .withSubjects(new SubjectBuilder()
            .withKind("ServiceAccount")
            .withName(roleName(context))
            .withNamespace(cluster.getMetadata().getNamespace())
            .build())
        .withRoleRef(new RoleRefBuilder()
            .withKind("Role")
            .withName(roleName(context))
            .withApiGroup("rbac.authorization.k8s.io")
            .build())
        .build();
  }

  @Inject
  public void setLabelFactory(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

}
