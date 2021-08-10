/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.stackgres.common.StackGresKubernetesClientFactory;
import io.stackgres.common.resource.ResourceWriter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Priority(1)
@Alternative
@ApplicationScoped
public class OperatorStatefulSetWriter implements ResourceWriter<StatefulSet> {

  private static final Logger LOGGER = LoggerFactory.getLogger(OperatorStatefulSetWriter.class);

  private final StackGresKubernetesClientFactory clientFactory;

  @Inject
  public OperatorStatefulSetWriter(StackGresKubernetesClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  @Override
  public StatefulSet create(@NotNull StatefulSet resource) {
    try (var client = clientFactory.create()) {
      return client.serverSideApply(new PatchContext.Builder()
          .withFieldManager(STACKGRES_FIELD_MANAGER)
          .withForce(true)
          .build(), resource);
    }
  }

  @Override
  public StatefulSet update(@NotNull StatefulSet resource) {
    try (var client = clientFactory.create()) {
      try {
        return client.serverSideApply(new PatchContext.Builder()
            .withFieldManager(STACKGRES_FIELD_MANAGER)
            .withForce(true)
            .build(), resource);
      } catch (KubernetesClientException ex) {
        LOGGER.warn("Server side apply failed, switching back to JSON merge", ex);
        return client.apps().statefulSets()
            .inNamespace(resource.getMetadata().getNamespace())
            .withName(resource.getMetadata().getName())
            .patch(resource);
      }
    }
  }

  @Override
  public void delete(@NotNull StatefulSet resource) {
    try (var client = clientFactory.create()) {
      client.apps().statefulSets()
          .inNamespace(resource.getMetadata().getNamespace())
          .withName(resource.getMetadata().getName())
          .delete();
    }
  }

  @Override
  public void deleteWithoutCascading(@NotNull StatefulSet resource) {
    clientFactory.withNewClient(
        client -> {
          String namespace = resource.getMetadata().getNamespace();
          String name = resource.getMetadata().getName();
          client.apps().statefulSets()
              .inNamespace(namespace).withName(name)
              .withPropagationPolicy(DeletionPropagation.ORPHAN)
              .delete();
          return null;
        });
  }
}
