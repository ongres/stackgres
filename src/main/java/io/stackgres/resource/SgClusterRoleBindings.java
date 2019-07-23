/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingList;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;
import io.stackgres.crd.sgcluster.StackGresCluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SgClusterRoleBindings {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgClusterRoleBindings.class);

  @Inject
  KubernetesClientFactory kubClientFactory;

  /**
   * Create the Service associated to the cluster.
   */
  public ClusterRoleBinding create(StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {

      ServiceAccount sa = client.serviceAccounts().inNamespace(namespace).withName(name).get();
      if (sa == null) {
        sa = new ServiceAccountBuilder()
            .withNewMetadata()
            .withName(name)
            .withNamespace(namespace)
            .endMetadata()
            .build();
        client.serviceAccounts().inNamespace(namespace).createOrReplace(sa);
        LOGGER.trace("ServiceAccount: {}", sa);
      }

      ClusterRoleBinding crb = client.rbac().clusterRoleBindings().inNamespace(namespace)
          .withName(name).get();
      if (crb == null) {
        crb = new ClusterRoleBindingBuilder()
            .withNewMetadata()
            .withName(name)
            .endMetadata()
            .withSubjects(new SubjectBuilder()
                .withKind("ServiceAccount")
                .withName(name)
                .withNamespace(namespace)
                .build())
            .withRoleRef(new RoleRefBuilder()
                .withKind("ClusterRole")
                .withName("cluster-admin")
                .withApiGroup("rbac.authorization.k8s.io")
                .build())
            .build();

        client.rbac().clusterRoleBindings().inNamespace(namespace).createOrReplace(crb);
        LOGGER.trace("ClusterRoleBinding: {}", crb);
      }

      ClusterRoleBindingList list =
          client.rbac().clusterRoleBindings().inNamespace(namespace).list();
      for (ClusterRoleBinding item : list.getItems()) {
        if (item.getMetadata().getName().equals(name)) {
          crb = item;
        }
      }

      LOGGER.debug("Creating ClusterRoleBinding: {}", name);
      return crb;
    }
  }

  /**
   * Delete resource.
   */
  public ClusterRoleBinding delete(StackGresCluster resource) {
    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      return delete(client, resource);
    }
  }

  /**
   * Delete resource.
   */
  public ClusterRoleBinding delete(KubernetesClient client, StackGresCluster resource) {
    final String name = resource.getMetadata().getName();
    final String namespace = resource.getMetadata().getNamespace();

    ClusterRoleBinding crb = client.rbac().clusterRoleBindings().inNamespace(namespace)
        .withName(name).get();
    if (crb != null) {
      client.rbac().clusterRoleBindings().inNamespace(namespace).withName(name).delete();
      LOGGER.debug("Deleting ClusterRoleBinding: {}", name);
    }

    ServiceAccount servAccount = client.serviceAccounts().inNamespace(namespace)
        .withName(name).get();
    if (servAccount != null) {
      client.serviceAccounts().inNamespace(namespace).withName(name).delete();
      LOGGER.debug("Deleting ServiceAccount: {}", name);
    }

    return crb;
  }

}
