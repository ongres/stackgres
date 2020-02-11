/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.resource;

import java.util.List;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.stackgres.operator.app.KubernetesClientFactory;

@ApplicationScoped
public class StorageClassFinder implements
    KubernetesResourceFinder<StorageClass>,
    KubernetesResourceScanner<StorageClass> {

  private KubernetesClientFactory kubClientFactory;

  @Inject
  public StorageClassFinder(KubernetesClientFactory kubClientFactory) {
    this.kubClientFactory = kubClientFactory;
  }

  @Override
  public Optional<StorageClass> findByName(String name) {

    try (KubernetesClient client = kubClientFactory.create()) {
      return Optional.ofNullable(client.storage().storageClasses().withName(name).get());
    }
  }

  @Override
  public List<StorageClass> findResources() {
    try (KubernetesClient client = kubClientFactory.create()) {
      return client.storage().storageClasses().list().getItems();
    }
  }
}
