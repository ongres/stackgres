/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.resource.ResourceFinder;
import io.stackgres.common.resource.ResourceWriter;
import org.jetbrains.annotations.NotNull;

@ApplicationScoped
public class ServiceAccountFinder implements ResourceFinder<ServiceAccount>,
    ResourceWriter<ServiceAccount> {

  final KubernetesClient client;

  @Inject
  public ServiceAccountFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<ServiceAccount> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<ServiceAccount> findByNameAndNamespace(String name, String namespace) {
    return Optional.ofNullable(client.serviceAccounts()
        .inNamespace(namespace)
        .withName(name)
        .get());
  }

  @Override
  public ServiceAccount create(@NotNull ServiceAccount resource) {
    return client.serviceAccounts()
        .resource(resource)
        .create();
  }

  @Override
  public ServiceAccount update(@NotNull ServiceAccount resource) {
    return client.serviceAccounts()
        .resource(resource)
        .patch(resource);
  }

  @Override
  public void delete(@NotNull ServiceAccount resource) {
    client.serviceAccounts()
        .resource(resource)
        .delete();
  }
}
