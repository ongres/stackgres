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
import io.stackgres.operator.customresource.sgbackup.StackGresBackup;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupDefinition;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupDoneable;
import io.stackgres.operator.customresource.sgbackup.StackGresBackupList;

@ApplicationScoped
public class BackupScheduler implements CustomResourceScheduler<StackGresBackup> {

  private KubernetesClientFactory kubeClient;

  @Inject
  public BackupScheduler(KubernetesClientFactory kubeClient) {
    this.kubeClient = kubeClient;
  }

  @Override
  public void create(StackGresBackup resource) {
    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresBackupDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresBackupDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresBackup.class,
          StackGresBackupList.class,
          StackGresBackupDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .create(resource);
    }
  }

  @Override
  public void update(StackGresBackup resource) {

    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresBackupDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresBackupDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresBackup.class,
          StackGresBackupList.class,
          StackGresBackupDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .createOrReplace(resource);
    }

  }

  @Override
  public void delete(StackGresBackup resource) {

    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresBackupDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresBackupDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresBackup.class,
          StackGresBackupList.class,
          StackGresBackupDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .delete(resource);
    }

  }
}
