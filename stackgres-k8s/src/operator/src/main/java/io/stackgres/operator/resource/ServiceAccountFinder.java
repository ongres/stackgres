/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.stackgres.common.KubernetesClientFactory;
import io.stackgres.common.resource.ResourceFinder;

@ApplicationScoped
public class ServiceAccountFinder implements ResourceFinder<ServiceAccount> {

  final KubernetesClientFactory clientFactory;

  @Inject
  public ServiceAccountFinder(KubernetesClientFactory clientFactory) {
    this.clientFactory = clientFactory;
  }

  @Override
  public Optional<ServiceAccount> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<ServiceAccount> findByNameAndNamespace(String name, String namespace) {
    try (var client = clientFactory.create()) {
      return Optional.ofNullable(client.serviceAccounts().inNamespace(namespace)
          .withName(name).get());
    }
  }
}
