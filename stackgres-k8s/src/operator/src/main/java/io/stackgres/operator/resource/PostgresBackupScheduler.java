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
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfig;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigDefinition;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigDoneable;
import io.stackgres.operator.customresource.sgbackupconfig.StackGresBackupConfigList;
import io.stackgres.operator.sidecars.pgbouncer.customresources.StackGresPgbouncerConfigDefinition;
import io.stackgres.operatorframework.resource.ResourceUtil;

@ApplicationScoped
public class PostgresBackupScheduler implements CustomResourceScheduler<StackGresBackupConfig> {

  private KubernetesClientFactory kubeClient;

  @Inject
  public PostgresBackupScheduler(KubernetesClientFactory kubeClient) {
    this.kubeClient = kubeClient;
  }

  @Override
  public void create(StackGresBackupConfig resource) {

    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresBackupConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPgbouncerConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresBackupConfig.class,
          StackGresBackupConfigList.class,
          StackGresBackupConfigDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .create(resource);
    }

  }

  @Override
  public void update(StackGresBackupConfig resource) {

    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresBackupConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPgbouncerConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresBackupConfig.class,
          StackGresBackupConfigList.class,
          StackGresBackupConfigDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .createOrReplace(resource);
    }

  }

  @Override
  public void delete(StackGresBackupConfig resource) {

    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresBackupConfigDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresPgbouncerConfigDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresBackupConfig.class,
          StackGresBackupConfigList.class,
          StackGresBackupConfigDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .delete(resource);
    }

  }
}
