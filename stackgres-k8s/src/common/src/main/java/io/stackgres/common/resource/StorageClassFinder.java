/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.resource;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.storage.StorageClass;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StorageClassFinder implements
    ResourceFinder<StorageClass>,
    ResourceScanner<StorageClass> {

  private final KubernetesClient client;

  @Inject
  public StorageClassFinder(KubernetesClient client) {
    this.client = client;
  }

  @Override
  public Optional<StorageClass> findByName(String name) {
    return Optional.ofNullable(client.storage().v1().storageClasses().withName(name).get());
  }

  @Override
  public Optional<StorageClass> findByNameAndNamespace(String name, String namespace) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<StorageClass> findResources() {
    return client.storage().v1().storageClasses().list().getItems();
  }

  @Override
  public List<StorageClass> findResourcesInNamespace(String namespace) {
    throw new UnsupportedOperationException();
  }

}
