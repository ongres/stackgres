/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.common.KubernetesClientFactory;

@ApplicationScoped
public class JobFinder implements
    ResourceFinder<Job>,
    ResourceScanner<Job> {

  private final KubernetesClientFactory kubClientFactory;

  @Inject
  public JobFinder(KubernetesClientFactory kubClientFactory) {
    this.kubClientFactory = kubClientFactory;
  }

  @Override
  public Optional<Job> findByName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Job> findByNameAndNamespace(String name, String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return Optional.ofNullable(client.batch().v1().jobs()
          .inNamespace(namespace)
          .withName(name)
          .get());
    }
  }

  @Override
  public List<Job> findResources() {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.batch().v1().jobs()
          .inAnyNamespace()
          .list()
          .getItems();
    }
  }

  @Override
  public List<Job> findResourcesInNamespace(String namespace) {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.batch().v1().jobs()
          .inNamespace(namespace)
          .list()
          .getItems();
    }
  }

}
