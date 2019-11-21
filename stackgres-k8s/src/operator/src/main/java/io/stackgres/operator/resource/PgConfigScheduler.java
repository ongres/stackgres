/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfig;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigDefinition;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigDoneable;
import io.stackgres.operator.customresource.sgpgconfig.StackGresPostgresConfigList;

@ApplicationScoped
public class PgConfigScheduler implements CustomResourceScheduler<StackGresPostgresConfig> {

  private KubernetesClientFactory kubeClient;

  @Inject
  public PgConfigScheduler(KubernetesClientFactory kubeClient) {
    this.kubeClient = kubeClient;
  }

  @Override
  public void create(StackGresPostgresConfig resource) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresPostgresConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPostgresConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresPostgresConfig.class,
          StackGresPostgresConfigList.class,
          StackGresPostgresConfigDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .create(resource);
    }
  }

  @Override
  public void update(StackGresPostgresConfig resource) {

    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresPostgresConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPostgresConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresPostgresConfig.class,
          StackGresPostgresConfigList.class,
          StackGresPostgresConfigDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .createOrReplace(resource);
    }

  }

  @Override
  public void delete(StackGresPostgresConfig resource) {

    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresPostgresConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPostgresConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresPostgresConfig.class,
          StackGresPostgresConfigList.class,
          StackGresPostgresConfigDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .delete(resource);
    }

  }
}
