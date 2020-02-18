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
import io.stackgres.operator.customresource.sgprofile.StackGresProfile;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileDefinition;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileDoneable;
import io.stackgres.operator.customresource.sgprofile.StackGresProfileList;
import io.stackgres.operatorframework.resource.ResourceUtil;

@ApplicationScoped
public class ProfileScheduler implements CustomResourceScheduler<StackGresProfile> {

  private KubernetesClientFactory kubeClient;

  @Inject
  public ProfileScheduler(KubernetesClientFactory kubeClient) {
    this.kubeClient = kubeClient;
  }

  @Override
  public void create(StackGresProfile profile) {

    try (KubernetesClient client = kubeClient.create()) {

      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresProfileDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresProfileDefinition.NAME + " not found."));

      client.customResources(crd,
          StackGresProfile.class,
          StackGresProfileList.class,
          StackGresProfileDoneable.class)
          .inNamespace(profile.getMetadata().getNamespace())
          .create(profile);

    }

  }

  @Override
  public void update(StackGresProfile resource) {

    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresProfileDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresProfileDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresProfile.class,
          StackGresProfileList.class,
          StackGresProfileDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .createOrReplace(resource);
    }

  }

  @Override
  public void delete(StackGresProfile resource) {

    try (KubernetesClient client = kubeClient.create()) {
      CustomResourceDefinition crd = ResourceUtil.getCustomResource(
          client, StackGresProfileDefinition.NAME)
          .orElseThrow(() -> new RuntimeException("StackGres is not correctly installed:"
              + " CRD " + StackGresProfileDefinition.NAME + " not found."));
      client.customResources(crd,
          StackGresProfile.class,
          StackGresProfileList.class,
          StackGresProfileDoneable.class)
          .inNamespace(resource.getMetadata().getNamespace())
          .delete(resource);
    }

  }
}
