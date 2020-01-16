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
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfig;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfigDefinition;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfigDoneable;
import io.stackgres.operator.customresource.sgrestoreconfig.StackgresRestoreConfigList;

@ApplicationScoped
public class RestoreConfigScheduler implements CustomResourceScheduler<StackgresRestoreConfig> {

  private KubernetesClientFactory kubeClient;

  @Inject
  public RestoreConfigScheduler(KubernetesClientFactory kubeClient) {
    this.kubeClient = kubeClient;
  }

  @Override
  public void create(StackgresRestoreConfig resource) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackgresRestoreConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackgresRestoreConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackgresRestoreConfig.class,
          StackgresRestoreConfigList.class,
          StackgresRestoreConfigDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .create(resource);
    }
  }

  @Override
  public void update(StackgresRestoreConfig resource) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackgresRestoreConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackgresRestoreConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackgresRestoreConfig.class,
          StackgresRestoreConfigList.class,
          StackgresRestoreConfigDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .createOrReplace(resource);
    }

  }

  @Override
  public void delete(StackgresRestoreConfig resource) {

    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackgresRestoreConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackgresRestoreConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackgresRestoreConfig.class,
          StackgresRestoreConfigList.class,
          StackgresRestoreConfigDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .delete(resource);
    }

  }
}
