/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.patroni;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.PolicyRuleBuilder;
import io.fabric8.kubernetes.api.model.rbac.Role;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.sgcluster.StackGresCluster;
import io.stackgres.operator.app.KubernetesClientFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SgPatroniRole {

  public static final String SUFFIX = "-patroni";

  private static final Logger LOGGER = LoggerFactory.getLogger(SgPatroniRole.class);

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create the Service associated to the cluster.
   */
  public void create(StackGresCluster resource) {
    final String name = resource.getMetadata().getName() + SUFFIX;
    final String namespace = resource.getMetadata().getNamespace();

    try (KubernetesClient client = kubClientFactory.create()) {
      createServiceAccount(name, namespace, client);
      createRole(name, namespace, client);
      createRoleBinding(name, namespace, client);
      LOGGER.debug("Creating Patroni Role: {}", name);
    }
  }

  private void createServiceAccount(final String name, final String namespace,
      KubernetesClient client) {
    ServiceAccount serviceAccount = client.serviceAccounts().inNamespace(namespace)
        .withName(name).get();
    if (serviceAccount == null) {
      serviceAccount = new ServiceAccountBuilder()
          .withNewMetadata()
          .withName(name)
          .withNamespace(namespace)
          .endMetadata()
          .build();
      serviceAccount = client.serviceAccounts().inNamespace(namespace)
          .createOrReplace(serviceAccount);
    }
    LOGGER.trace("ServiceAccount: {}", serviceAccount);
  }

  private void createRole(final String name, final String namespace, KubernetesClient client) {
    Role role = client.rbac().roles().inNamespace(namespace).withName(name).get();
    if (role == null) {
      role = new RoleBuilder()
          .withNewMetadata()
          .withName(name)
          .endMetadata()
          .addToRules(new PolicyRuleBuilder()
              .withApiGroups("")
              .withResources("endpoints", "configmaps")
              .withVerbs("create", "get", "list", "patch", "update", "watch")
              .build())
          .addToRules(new PolicyRuleBuilder()
              .withApiGroups("")
              .withResources("pods")
              .withVerbs("get", "list", "patch", "update", "watch")
              .build())
          .addToRules(new PolicyRuleBuilder()
              .withApiGroups("")
              .withResources("services")
              .withVerbs("create")
              .build())
          .build();

      role = client.rbac().roles().inNamespace(namespace).createOrReplace(role);
    }
    LOGGER.trace("Role: {}", role);
  }

  private void createRoleBinding(final String name, final String namespace,
      KubernetesClient client) {
    RoleBinding roleBind = client.rbac().roleBindings().inNamespace(namespace)
        .withName(name).get();
    if (roleBind == null) {
      roleBind = new RoleBindingBuilder()
          .withNewMetadata()
          .withName(name)
          .endMetadata()
          .withSubjects(new SubjectBuilder()
              .withKind("ServiceAccount")
              .withName(name)
              .withNamespace(namespace)
              .build())
          .withRoleRef(new RoleRefBuilder()
              .withKind("Role")
              .withName(name)
              .withApiGroup("rbac.authorization.k8s.io")
              .build())
          .build();

      roleBind = client.rbac().roleBindings().inNamespace(namespace).createOrReplace(roleBind);
    }
    LOGGER.trace("RoleBinding: {}", roleBind);
  }

  /**
   * Delete resource.
   */
  public void delete(StackGresCluster resource) {
    try (KubernetesClient client = kubClientFactory.create()) {
      delete(client, resource);
    }
  }

  /**
   * Delete resource.
   */
  public void delete(KubernetesClient client, StackGresCluster resource) {
    final String name = resource.getMetadata().getName() + SUFFIX;
    final String namespace = resource.getMetadata().getNamespace();

    RoleBinding roleBind = client.rbac().roleBindings().inNamespace(namespace)
        .withName(name).get();
    if (roleBind != null) {
      client.rbac().roleBindings().inNamespace(namespace).withName(name).delete();
      LOGGER.debug("Deleting RoleBinding: {}", name);
    }

    Role role = client.rbac().roles().inNamespace(namespace).withName(name).get();
    if (role != null) {
      client.rbac().roles().inNamespace(namespace).withName(name).delete();
      LOGGER.debug("Deleting RoleBinding: {}", name);
    }

    ServiceAccount serviceAccount = client.serviceAccounts().inNamespace(namespace)
        .withName(name).get();
    if (serviceAccount != null) {
      client.serviceAccounts().inNamespace(namespace).withName(name).delete();
      LOGGER.debug("Deleting ServiceAccount: {}", name);
    }
  }

}
