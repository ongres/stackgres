/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingList;
import io.fabric8.kubernetes.api.model.rbac.RoleRefBuilder;
import io.fabric8.kubernetes.api.model.rbac.SubjectBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.app.KubernetesClientFactory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SgClusterRoleBindings {

  private static final Logger LOGGER = LoggerFactory.getLogger(SgClusterRoleBindings.class);

  @ConfigProperty(name = "stackgres.namespace", defaultValue = "stackgres")
  @NonNull
  String namespace;

  @Inject
  @NonNull
  KubernetesClientFactory kubClientFactory;

  /**
   * Create the Service associated to the cluster.
   */
  public @NonNull ClusterRoleBinding create(@NonNull String name) {
    LOGGER.debug("Creating ClusterRoleBinding: {}", name);

    try (KubernetesClient client = kubClientFactory.retrieveKubernetesClient()) {
      ClusterRoleBinding crb = new ClusterRoleBindingBuilder()
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

      ClusterRoleBindingList list =
          client.rbac().clusterRoleBindings().inNamespace(namespace).list();
      for (ClusterRoleBinding item : list.getItems()) {
        LOGGER.debug(item.getMetadata().getName());
        if (item.getMetadata().getName().equals(name)) {
          crb = item;
        }
      }

      return crb;
    }
  }

}
