/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.stream;

import java.util.Map;
import java.util.stream.Stream;

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
import io.stackgres.common.crd.CommonDefinition;
import io.stackgres.common.crd.sgstream.StackGresStream;
import io.stackgres.common.labels.LabelFactoryForStream;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.ResourceGenerator;
import io.stackgres.operator.conciliation.stream.StackGresStreamContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@OperatorVersionBinder
public class StreamRole implements ResourceGenerator<StackGresStreamContext> {

  public static final String SUFFIX = "-stream";

  private LabelFactoryForStream labelFactory;

  public static String roleName(StackGresStreamContext context) {
    return roleName(context.getSource());
  }

  public static String roleName(StackGresStream stream) {
    return roleName(stream.getMetadata().getName());
  }

  public static String roleName(String streamName) {
    return ResourceUtil.resourceName(streamName + SUFFIX);
  }

  @Override
  public Stream<HasMetadata> generateResource(StackGresStreamContext context) {
    return Stream.<HasMetadata>of(
        createServiceAccount(context),
        createRole(context),
        createRoleBinding(context));
  }

  /**
   * Create the ServiceAccount for Job associated to the stream.
   */
  private ServiceAccount createServiceAccount(StackGresStreamContext context) {
    final StackGresStream stream = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(context.getSource());

    final String serviceAccountName = roleName(context);
    final String serviceAccountNamespace = stream.getMetadata().getNamespace();
    return new ServiceAccountBuilder()
        .withNewMetadata()
        .withName(serviceAccountName)
        .withNamespace(serviceAccountNamespace)
        .withLabels(labels)
        .endMetadata()
        .build();

  }

  /**
   * Create the Role for Job associated to the stream.
   */
  private Role createRole(StackGresStreamContext context) {
    final StackGresStream stream = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(context.getSource());
    return new RoleBuilder()
        .withNewMetadata()
        .withName(roleName(context))
        .withNamespace(stream.getMetadata().getNamespace())
        .withLabels(labels)
        .endMetadata()
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("pods")
            .withVerbs("get", "list", "watch")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("secrets", "endpoints")
            .withVerbs("get", "list")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups("")
            .withResources("events")
            .withVerbs("get", "list", "create", "patch", "update")
            .build())
        .addToRules(new PolicyRuleBuilder()
            .withApiGroups(CommonDefinition.GROUP)
            .withResources(HasMetadata.getPlural(StackGresStream.class))
            .withVerbs("get", "list", "watch", "patch", "update")
            .build())
        .build();
  }

  /**
   * Create the RoleBinding for Job associated to the stream.
   */
  private RoleBinding createRoleBinding(StackGresStreamContext context) {
    final StackGresStream stream = context.getSource();
    final Map<String, String> labels = labelFactory.genericLabels(context.getSource());
    return new RoleBindingBuilder()
        .withNewMetadata()
        .withName(roleName(context))
        .withNamespace(stream.getMetadata().getNamespace())
        .withLabels(labels)
        .endMetadata()
        .withSubjects(new SubjectBuilder()
            .withKind("ServiceAccount")
            .withName(roleName(context))
            .withNamespace(stream.getMetadata().getNamespace())
            .build())
        .withRoleRef(new RoleRefBuilder()
            .withKind("Role")
            .withName(roleName(context))
            .withApiGroup("rbac.authorization.k8s.io")
            .build())
        .build();
  }

  @Inject
  public void setLabelFactory(LabelFactoryForStream labelFactory) {
    this.labelFactory = labelFactory;
  }

}
