/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Event;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.stackgres.common.ClusterContext;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgdbops.StackGresDbOps;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.common.crd.sgpooling.StackGresPoolingConfig;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgscript.StackGresScript;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class PatroniRole implements
    ResourceGenerator<StackGresClusterContext> {

  private LabelFactoryForCluster<StackGresCluster> labelFactory;

  public static String roleName(ClusterContext clusterContext) {
    return PatroniUtil.roleName(clusterContext.getCluster());
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresClusterContext context) {
    return Stream.of(
        createServiceAccount(context),
        createRole(context),
        createRoleBinding(context));
  }

  /**
   * Create the ServiceAccount for patroni associated to the cluster.
   */
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

  /**
   * Create the Role for patroni associated to the cluster.
   */
  private Role createRole(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(cluster);
    return new RoleBuilder()
        .withNewMetadata()
        .withName(roleName(context))
        .withNamespace(cluster.getMetadata().getNamespace())
        .withLabels(labels)
        .endMetadata()
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(Endpoints.class))
            .withResources(HasMetadata.getPlural(Endpoints.class))
            .withVerbs("get", "list", "patch", "update", "watch", "deletecollection")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(ConfigMap.class))
            .withResources(HasMetadata.getPlural(ConfigMap.class))
            .withVerbs("create", "get", "list", "patch", "update", "watch")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(Endpoints.class))
            .withResources(HasMetadata.getPlural(Endpoints.class) + "/restricted")
            .withVerbs("create")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(Secret.class))
            .withResources(HasMetadata.getPlural(Secret.class))
            .withVerbs("get")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(Pod.class))
            .withResources(HasMetadata.getPlural(Pod.class))
            .withVerbs("get", "list", "patch", "update", "watch")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(Pod.class))
            .withResources(HasMetadata.getPlural(Pod.class) + "/exec")
            .withVerbs("create")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(Pod.class))
            .withResources(HasMetadata.getPlural(Pod.class))
            .withVerbs("delete")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(PersistentVolumeClaim.class))
            .withResources(HasMetadata.getPlural(PersistentVolumeClaim.class))
            .withVerbs("delete")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(Service.class))
            .withResources(HasMetadata.getPlural(Service.class))
            .withVerbs("create")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(Event.class))
            .withResources(HasMetadata.getPlural(Event.class))
            .withVerbs("get", "list", "create", "patch", "update")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(PersistentVolumeClaim.class))
            .withResources(HasMetadata.getPlural(PersistentVolumeClaim.class))
            .withVerbs("get", "list", "patch", "update")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(HasMetadata.getGroup(StatefulSet.class))
            .withResources(HasMetadata.getPlural(StatefulSet.class))
            .withVerbs("get")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(HasMetadata.getPlural(StackGresBackup.class))
            .withVerbs("list", "get", "create", "patch", "update", "delete")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(
                HasMetadata.getPlural(StackGresCluster.class),
                HasMetadata.getPlural(StackGresPostgresConfig.class),
                HasMetadata.getPlural(StackGresObjectStorage.class),
                HasMetadata.getPlural(StackGresPoolingConfig.class),
                HasMetadata.getPlural(StackGresProfile.class),
                HasMetadata.getPlural(StackGresDistributedLogs.class),
                HasMetadata.getPlural(StackGresDbOps.class),
                HasMetadata.getPlural(StackGresScript.class),
                HasMetadata.getPlural(StackGresShardedCluster.class))
            .withVerbs("get", "list", "watch", "patch", "update")
            .build())
        .build();
  }

  /**
   * Create the RoleBinding for patroni associated to the cluster.
   */
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
