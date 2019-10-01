/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.services;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.customresource.sgprofile.StackGresProfile;
import io.stackgres.common.customresource.sgprofile.StackGresProfileDefinition;
import io.stackgres.common.customresource.sgprofile.StackGresProfileDoneable;
import io.stackgres.common.customresource.sgprofile.StackGresProfileList;
import io.stackgres.common.resource.ResourceUtil;
import io.stackgres.operator.app.KubernetesClientFactory;

@ApplicationScoped
public class ProfileConfigFinder implements KubernetesCustomResourceFinder<StackGresProfile> {

  private KubernetesClientFactory kubClientFactory;

  @Inject
  public ProfileConfigFinder(KubernetesClientFactory kubClientFactory) {
    this.kubClientFactory = kubClientFactory;
  }

  @Override
  public Optional<StackGresProfile> findByNameAndNamespace(String name, String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      Optional<CustomResourceDefinition> crd =
          ResourceUtil.getCustomResource(client, StackGresProfileDefinition.NAME);
      if (crd.isPresent()) {
        return Optional.ofNullable(client
            .customResources(crd.get(),
                StackGresProfile.class,
                StackGresProfileList.class,
                StackGresProfileDoneable.class)
            .inNamespace(namespace)
            .withName(name)
            .get());
      }
    }
    return Optional.empty();
  }
}
