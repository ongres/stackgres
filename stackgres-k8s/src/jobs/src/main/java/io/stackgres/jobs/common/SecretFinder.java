/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.jobs.common;

import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

public class SecretFinder implements
    ResourceFinder<Secret> {

  final KubernetesClientFactory kubClientFactory;

  public SecretFinder(KubernetesClientFactory kubClientFactory) {
    this.kubClientFactory = kubClientFactory;
  }

  @Override
  public Optional<Secret> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Secret> findByNameAndNamespace(String name, String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return Optional.ofNullable(client.secrets().inNamespace(namespace).withName(name).get());
    }
  }

}
