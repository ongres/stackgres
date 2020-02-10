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
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfig;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDefinition;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDoneable;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigList;

@ApplicationScoped
public class PgPoolingConfigScheduler implements CustomResourceScheduler<StackGresPgbouncerConfig> {

  private KubernetesClientFactory kubeClient;

  @Inject
  public PgPoolingConfigScheduler(KubernetesClientFactory kubeClient) {
    this.kubeClient = kubeClient;
  }

  @Override
  public void create(StackGresPgbouncerConfig resource) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresPgbouncerConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPgbouncerConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresPgbouncerConfig.class,
          StackGresPgbouncerConfigList.class,
          StackGresPgbouncerConfigDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .create(resource);
    }
  }

  @Override
  public void update(StackGresPgbouncerConfig resource) {

    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresPgbouncerConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPgbouncerConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresPgbouncerConfig.class,
          StackGresPgbouncerConfigList.class,
          StackGresPgbouncerConfigDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .createOrReplace(resource);
    }

  }

  @Override
  public void delete(StackGresPgbouncerConfig resource) {

    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresPgbouncerConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPgbouncerConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresPgbouncerConfig.class,
          StackGresPgbouncerConfigList.class,
          StackGresPgbouncerConfigDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .delete(resource);
    }

  }
}
