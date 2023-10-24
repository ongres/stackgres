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
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.stackgres.common.resource.AbstractResourceWriter;
import io.stackgres.common.resource.ResourceFinder;

@ApplicationScoped
public class ServiceAccountFinder
    extends AbstractResourceWriter<ServiceAccount>
    implements ResourceFinder<ServiceAccount> {

  final KubernetesClient client;

  @Inject
  public ServiceAccountFinder(KubernetesClient client) {
    super(client);
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
  protected MixedOperation<ServiceAccount, ?, ?> getResourceEndpoints(KubernetesClient client) {
    return client.serviceAccounts();
  }

}
